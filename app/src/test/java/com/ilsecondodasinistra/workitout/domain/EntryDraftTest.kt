package com.ilsecondodasinistra.workitout.domain

import com.ilsecondodasinistra.workitout.data.PausePair
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset

class EntryDraftTest {

    private val zone = ZoneOffset.UTC
    private val date = LocalDate.of(2026, 3, 10)

    private fun draft(
        enter: String = "09:00",
        exit: String = "18:00",
        pauses: List<PauseDraft> = emptyList(),
        target: Double = 8.0,
    ) = EntryDraft(date, LocalTime.parse(enter), LocalTime.parse(exit), pauses, target)

    private fun at(day: Int, time: String) =
        Instant.parse("2026-03-${day}T$time:00Z").toEpochMilli()

    private fun valid(result: DraftResult) = (result as DraftResult.Valid).entry
    private fun error(result: DraftResult) = (result as DraftResult.Invalid).error

    @Test
    fun simpleDay() {
        val entry = valid(draft().toEntry(null, zone))
        assertEquals(at(10, "09:00"), entry.enterTime)
        assertEquals(at(10, "18:00"), entry.exitTime)
        assertEquals("9h 0m", entry.totalWorkedTime)
        assertEquals(entry.enterTime.toString(), entry.id)
    }

    @Test
    fun pausesAreSubtractedAndSorted() {
        val entry = valid(
            draft(
                pauses = listOf(
                    PauseDraft(LocalTime.parse("15:00"), LocalTime.parse("15:15")),
                    PauseDraft(LocalTime.parse("12:00"), LocalTime.parse("13:00")),
                    PauseDraft(), // empty rows are dropped
                ),
            ).toEntry("keep", zone),
        )
        assertEquals("keep", entry.id)
        assertEquals(listOf(PausePair(at(10, "12:00"), at(10, "13:00")), PausePair(at(10, "15:00"), at(10, "15:15"))), entry.pauses)
        assertEquals("7h 45m", entry.totalWorkedTime)
    }

    @Test
    fun nightShift_exitAndPauseRollToNextDay() {
        val entry = valid(
            draft(
                enter = "22:00", exit = "06:00",
                pauses = listOf(PauseDraft(LocalTime.parse("01:00"), LocalTime.parse("01:30"))),
            ).toEntry(null, zone),
        )
        assertEquals(at(11, "06:00"), entry.exitTime)
        assertEquals(PausePair(at(11, "01:00"), at(11, "01:30")), entry.pauses.single())
        assertEquals("7h 30m", entry.totalWorkedTime)
    }

    @Test
    fun exitEqualToEnter_isRejected() {
        assertEquals(DraftError.ExitNotAfterEnter, error(draft(enter = "09:00", exit = "09:00").toEntry(null, zone)))
    }

    @Test
    fun halfFilledPause_isRejected() {
        val result = draft(pauses = listOf(PauseDraft(start = LocalTime.parse("12:00")))).toEntry(null, zone)
        assertEquals(DraftError.PauseIncomplete, error(result))
    }

    @Test
    fun pauseAfterExit_isRejected() {
        val result = draft(pauses = listOf(PauseDraft(LocalTime.parse("17:30"), LocalTime.parse("18:30")))).toEntry(null, zone)
        assertEquals(DraftError.PauseOutsideDay, error(result))
    }

    @Test
    fun targetOutOfRange_isRejected() {
        assertEquals(DraftError.InvalidTarget, error(draft(target = 25.0).toEntry(null, zone)))
        assertEquals(DraftError.InvalidTarget, error(draft(target = -1.0).toEntry(null, zone)))
    }

    @Test
    fun roundTrip_fromEntryToEntry() {
        val original = valid(
            draft(pauses = listOf(PauseDraft(LocalTime.parse("12:00"), LocalTime.parse("12:45")))).toEntry("x", zone),
        )
        val again = valid(EntryDraft.fromEntry(original, zone).toEntry("x", zone))
        assertEquals(original, again)
        assertTrue(EntryDraft.fromEntry(original, zone).pauses.size == 1)
    }
}
