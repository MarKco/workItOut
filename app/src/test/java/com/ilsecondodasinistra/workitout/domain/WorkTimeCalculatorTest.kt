package com.ilsecondodasinistra.workitout.domain

import com.ilsecondodasinistra.workitout.data.PausePair
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

class WorkTimeCalculatorTest {

    private val hour = 3_600_000L
    private val minute = 60_000L

    @Test
    fun pauseMillis_ignoresIncompleteOrInvertedPauses() {
        assertEquals(0, WorkTimeCalculator.pauseMillis(PausePair(start = 10, end = null)))
        assertEquals(0, WorkTimeCalculator.pauseMillis(PausePair(start = null, end = 10)))
        assertEquals(0, WorkTimeCalculator.pauseMillis(PausePair(start = 20, end = 10)))
        assertEquals(10, WorkTimeCalculator.pauseMillis(PausePair(start = 10, end = 20)))
    }

    @Test
    fun pauseMinutes_hidesPausesShorterThanAMinute() {
        assertNull(WorkTimeCalculator.pauseMinutes(PausePair(0, 59_000)))
        assertEquals(30L, WorkTimeCalculator.pauseMinutes(PausePair(0, 30 * minute)))
    }

    @Test
    fun expectedExit_addsTargetAndAllPauses() {
        val pauses = listOf(PausePair(4 * hour, 5 * hour), PausePair(6 * hour, 6 * hour + 15 * minute))
        val exit = WorkTimeCalculator.expectedExit(enterTime = 0, pauses = pauses, dailyHours = 8.0)
        assertEquals(8 * hour + hour + 15 * minute, exit)
    }

    @Test
    fun expectedExit_ignoresOpenPause() {
        val exit = WorkTimeCalculator.expectedExit(0, listOf(PausePair(hour, null)), 7.5)
        assertEquals(7 * hour + 30 * minute, exit)
    }

    @Test
    fun expectedExit_isNullWithoutTarget() {
        assertNull(WorkTimeCalculator.expectedExit(0, emptyList(), 0.0))
    }

    @Test
    fun workedMillis_subtractsPauses() {
        val worked = WorkTimeCalculator.workedMillis(0, 9 * hour, listOf(PausePair(4 * hour, 5 * hour)))
        assertEquals(8 * hour, worked)
    }

    @Test
    fun workedMillis_isNullWhenExitNotAfterEnter() {
        assertNull(WorkTimeCalculator.workedMillis(10, 10, emptyList()))
        assertNull(WorkTimeCalculator.workedMillis(10, 5, emptyList()))
    }

    @Test
    fun workedMillis_neverNegative() {
        assertEquals(0L, WorkTimeCalculator.workedMillis(0, hour, listOf(PausePair(0, 2 * hour))))
    }

    @Test
    fun formatDuration_showsHoursAndMinutes() {
        assertEquals("7h 45m", WorkTimeCalculator.formatDuration(7 * hour + 45 * minute))
        assertEquals("0h 0m", WorkTimeCalculator.formatDuration(0))
        assertEquals("26h 5m", WorkTimeCalculator.formatDuration(26 * hour + 5 * minute))
    }

    @Test
    fun rollToNextDay_movesEarlierTimesForward() {
        val zone = ZoneId.of("UTC")
        val enter = Instant.parse("2026-03-10T22:00:00Z").toEpochMilli()
        val exitPickedAt01 = Instant.parse("2026-03-10T01:00:00Z").toEpochMilli()
        val expected = Instant.parse("2026-03-11T01:00:00Z").toEpochMilli()
        assertEquals(expected, WorkTimeCalculator.rollToNextDayIfBefore(exitPickedAt01, enter, zone))
    }

    @Test
    fun rollToNextDay_keepsLaterTimes() {
        val zone = ZoneId.of("UTC")
        assertEquals(500L, WorkTimeCalculator.rollToNextDayIfBefore(500, 100, zone))
    }

    @Test
    fun rollToNextDay_keepsWallClockAcrossDst() {
        val zone = ZoneId.of("Europe/Rome")
        // 2026-03-28 23:00 -> exit picked 02:00 on the 28th; next day (29th) has the DST jump at 02:00.
        val enter = Instant.parse("2026-03-28T22:00:00Z").toEpochMilli()
        val picked = Instant.parse("2026-03-28T01:00:00Z").toEpochMilli() // 02:00 local on the 28th
        val rolled = WorkTimeCalculator.rollToNextDayIfBefore(picked, enter, zone)
        assertEquals(Instant.parse("2026-03-29T01:00:00Z").toEpochMilli(), rolled)
    }

    @Test
    fun formatBalance_isSigned() {
        assertEquals("+1h 30m", WorkTimeCalculator.formatBalance(hour + 30 * minute))
        assertEquals("-45m", WorkTimeCalculator.formatBalance(-45 * minute))
        assertEquals("-2h 0m", WorkTimeCalculator.formatBalance(-2 * hour))
        assertEquals("0m", WorkTimeCalculator.formatBalance(0))
        assertEquals("0m", WorkTimeCalculator.formatBalance(30_000))
    }
}
