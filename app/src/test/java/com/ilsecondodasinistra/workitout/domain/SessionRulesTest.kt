package com.ilsecondodasinistra.workitout.domain

import com.ilsecondodasinistra.workitout.data.ActiveSession
import com.ilsecondodasinistra.workitout.data.PausePair
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.ZoneId

class SessionRulesTest {

    private val hour = 3_600_000L
    private val minute = 60_000L

    @Test
    fun togglePause_startsANewPauseWhenNoneIsOpen() {
        assertEquals(listOf(PausePair(start = 100)), SessionRules.togglePause(emptyList(), 100))
        val closed = listOf(PausePair(10, 20))
        assertEquals(closed + PausePair(start = 100), SessionRules.togglePause(closed, 100))
    }

    @Test
    fun togglePause_endsTheOpenPause() {
        assertEquals(listOf(PausePair(10, 100)), SessionRules.togglePause(listOf(PausePair(start = 10)), 100))
    }

    @Test
    fun togglePause_fillsAnEmptyLeftoverPause() {
        assertEquals(listOf(PausePair(start = 100)), SessionRules.togglePause(listOf(PausePair()), 100))
    }

    @Test
    fun completedEntry_computesTotalAndRollsPastMidnight() {
        val zone = ZoneId.of("UTC")
        val entry = SessionRules.completedEntry(0, listOf(PausePair(hour, hour + 30 * minute)), 9 * hour, 8.0, zone)!!
        assertEquals("8h 30m", entry.totalWorkedTime)
        assertEquals("0", entry.id)
    }

    @Test
    fun completedEntry_isNullWhenExitIsNotAfterEnter() {
        assertNull(SessionRules.completedEntry(5 * hour, emptyList(), 5 * hour, 8.0, ZoneId.of("UTC")))
    }

    @Test
    fun completedEntry_closesAPauseStillOpenAtExit() {
        val entry = SessionRules.completedEntry(0, listOf(PausePair(start = 7 * hour)), 8 * hour, 8.0, ZoneId.of("UTC"))!!
        assertEquals(listOf(PausePair(7 * hour, 8 * hour)), entry.pauses)
        assertEquals("7h 0m", entry.totalWorkedTime)
    }

    @Test
    fun progress_whileWorking() {
        val session = ActiveSession(0, listOf(PausePair(2 * hour, 3 * hour)))
        val progress = SessionProgress.of(session, 8.0, 5 * hour)
        assertEquals(SessionProgress.Phase.Working, progress.phase)
        assertEquals(4 * hour, progress.workedMillis)
        assertEquals(4 * hour, progress.remainingMillis)
        assertEquals(9 * hour, progress.expectedExit)
        assertEquals(0.5f, progress.fraction, 0.001f)
    }

    @Test
    fun progress_onPauseMovesTheExitAndFreezesWork() {
        val session = ActiveSession(0, listOf(PausePair(start = 2 * hour)))
        val progress = SessionProgress.of(session, 8.0, 2 * hour + 20 * minute)
        assertEquals(SessionProgress.Phase.OnPause, progress.phase)
        assertEquals(2 * hour, progress.workedMillis)
        assertEquals(8 * hour + 20 * minute, progress.expectedExit)
        assertEquals(2 * hour, progress.pauseStart)
    }

    @Test
    fun progress_goalReachedReportsOvertime() {
        val progress = SessionProgress.of(ActiveSession(0), 8.0, 8 * hour + 12 * minute)
        assertEquals(SessionProgress.Phase.GoalReached, progress.phase)
        assertEquals(12 * minute, progress.overtimeMillis)
        assertEquals(1f, progress.fraction, 0.001f)
    }
}
