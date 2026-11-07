package com.ilsecondodasinistra.workitout.domain

import com.ilsecondodasinistra.workitout.data.DEFAULT_WORK_DAYS
import com.ilsecondodasinistra.workitout.data.PausePair
import com.ilsecondodasinistra.workitout.data.WorkHistoryEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneOffset

class WorkStatsTest {

    private val zone = ZoneOffset.UTC
    private val hour = 3_600_000L

    // 2026-03-11 is a Wednesday.
    private val today = LocalDate.of(2026, 3, 11)

    private fun day(date: String, from: Int, to: Int, pauseHours: Int = 0, target: Double = 8.0): WorkHistoryEntry {
        val d = LocalDate.parse(date)
        val enter = d.atTime(from, 0).toInstant(zone).toEpochMilli()
        val exit = d.atTime(to, 0).toInstant(zone).toEpochMilli()
        return WorkHistoryEntry(
            id = enter.toString(),
            enterTime = enter,
            pauses = if (pauseHours > 0) listOf(PausePair(enter + 3 * hour, enter + (3 + pauseHours) * hour)) else emptyList(),
            exitTime = exit,
            totalWorkedTime = "",
            dailyHoursTarget = target,
        )
    }

    private fun stats(vararg entries: WorkHistoryEntry, workDays: Set<DayOfWeek> = DEFAULT_WORK_DAYS) =
        WorkStats.compute(entries.toList(), workDays, today, zone)

    @Test
    fun emptyHistory_isAllZero() {
        val s = stats()
        assertEquals(PeriodStats(), s.week)
        assertEquals(PeriodStats(), s.overall)
        assertEquals(14, s.recentDays.size)
        assertEquals(today, s.recentDays.last().date)
    }

    @Test
    fun overtimeAndUndertime_accumulateIntoBalance() {
        // Mon: 9h - 1h pause = 8h worked (balance 0). Tue: 10h - 1h = 9h (+1h). Wed: 7h (-1h)
        val s = stats(
            day("2026-03-09", 9, 18, pauseHours = 1),
            day("2026-03-10", 9, 19, pauseHours = 1),
            day("2026-03-11", 9, 16),
        )
        assertEquals(24 * hour, s.week.workedMillis)
        assertEquals(24 * hour, s.week.targetMillis)
        assertEquals(0, s.week.balanceMillis)
        assertEquals(3, s.week.daysWorked)
        assertEquals(8 * hour, s.week.averageMillis)
    }

    @Test
    fun nonWorkDay_countsEntirelyAsOvertime() {
        val saturday = day("2026-03-07", 9, 13) // previous week
        val s = stats(saturday)
        assertEquals(4 * hour, s.overall.workedMillis)
        assertEquals(0, s.overall.targetMillis)
        assertEquals(4 * hour, s.overall.balanceMillis)
        assertFalse(s.recentDays.first { it.date == LocalDate.of(2026, 3, 7) }.isWorkDay)
    }

    @Test
    fun customWorkDays_changeTarget() {
        val saturday = day("2026-03-07", 9, 17)
        val s = stats(saturday, workDays = setOf(DayOfWeek.SATURDAY))
        assertEquals(8 * hour, s.overall.targetMillis)
        assertEquals(0, s.overall.balanceMillis)
    }

    @Test
    fun weekAndMonthBoundaries() {
        val s = stats(
            day("2026-03-08", 9, 17), // Sunday, previous ISO week, same month
            day("2026-03-09", 9, 17), // Monday, this week
            day("2026-02-27", 9, 17), // previous month
        )
        assertEquals(1, s.week.daysWorked)
        assertEquals(2, s.month.daysWorked)
        assertEquals(3, s.overall.daysWorked)
    }

    @Test
    fun twoEntriesOnSameDay_shareOneTarget() {
        val s = stats(day("2026-03-10", 8, 12), day("2026-03-10", 13, 17))
        assertEquals(8 * hour, s.week.workedMillis)
        assertEquals(8 * hour, s.week.targetMillis)
        assertEquals(1, s.week.daysWorked)
    }

    @Test
    fun recentDays_fillMissingDaysWithZero() {
        val s = stats(day("2026-03-10", 9, 17))
        val tuesday = s.recentDays.first { it.date == LocalDate.of(2026, 3, 10) }
        val monday = s.recentDays.first { it.date == LocalDate.of(2026, 3, 9) }
        assertEquals(8 * hour, tuesday.workedMillis)
        assertEquals(0, monday.workedMillis)
        assertTrue(monday.isWorkDay)
    }
}
