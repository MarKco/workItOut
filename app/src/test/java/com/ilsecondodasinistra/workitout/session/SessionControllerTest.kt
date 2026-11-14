package com.ilsecondodasinistra.workitout.session

import com.ilsecondodasinistra.workitout.data.ActiveSession
import com.ilsecondodasinistra.workitout.data.PausePair
import com.ilsecondodasinistra.workitout.testutil.FakeReminder
import com.ilsecondodasinistra.workitout.testutil.FakeRepository
import com.ilsecondodasinistra.workitout.testutil.MutableClock
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class SessionControllerTest {

    private val hour = 3_600_000L
    private val minute = 60_000L
    private val day = Instant.parse("2026-11-12T00:00:00Z").toEpochMilli()

    private val repository = FakeRepository()
    private val reminder = FakeReminder()
    private val clock = MutableClock(Instant.ofEpochMilli(day + 12 * hour))
    private val controller = SessionController(repository, reminder, clock)

    @Test
    fun togglePause_startsThenEndsABreakAndMovesTheReminder() = runBlocking {
        repository.activeSession.value = ActiveSession(day + 9 * hour)

        controller.togglePause()
        assertEquals(listOf(PausePair(start = day + 12 * hour)), repository.activeSession.value!!.pauses)

        clock.now = Instant.ofEpochMilli(day + 12 * hour + 40 * minute)
        controller.togglePause()
        assertEquals(listOf(PausePair(day + 12 * hour, day + 12 * hour + 40 * minute)), repository.activeSession.value!!.pauses)
        // Exit = enter + 8h + 40 min of break.
        assertEquals(day + 9 * hour + 8 * hour + 40 * minute, reminder.scheduled.last())
    }

    @Test
    fun togglePause_withoutSessionDoesNothing() = runBlocking {
        controller.togglePause()
        assertNull(repository.activeSession.value)
    }

    @Test
    fun togglePause_ignoresASessionLeftOpenOnAnEarlierDay() = runBlocking {
        repository.activeSession.value = ActiveSession(day - 15 * hour)
        controller.togglePause()
        assertTrue(repository.activeSession.value!!.pauses.isEmpty())
    }

    @Test
    fun finish_savesTheDayClosesTheBreakAndCancelsTheReminder() = runBlocking {
        repository.activeSession.value = ActiveSession(day + 8 * hour, listOf(PausePair(start = day + 11 * hour + 30 * minute)))
        clock.now = Instant.ofEpochMilli(day + 12 * hour)

        val entry = controller.finish()!!

        assertEquals("3h 30m", entry.totalWorkedTime)
        assertEquals(listOf(entry), repository.history.value)
        assertNull(repository.activeSession.value)
        assertEquals(1, reminder.cancelCount)
    }

    @Test
    fun finish_withoutSessionReturnsNull() = runBlocking {
        assertNull(controller.finish())
        assertTrue(repository.history.value.isEmpty())
    }
}
