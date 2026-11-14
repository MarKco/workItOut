package com.ilsecondodasinistra.workitout.domain

import com.ilsecondodasinistra.workitout.data.ActiveSession
import com.ilsecondodasinistra.workitout.data.PausePair
import com.ilsecondodasinistra.workitout.data.WorkHistoryEntry
import java.time.ZoneId

/** Pure rules on the day in progress, shared by the Home screen and the notification buttons. */
object SessionRules {

    /** The pause the user is in (or has opened but not started), if any. */
    fun openPause(pauses: List<PausePair>): PausePair? = pauses.lastOrNull()?.takeIf { it.end == null }

    /** Pauses after the break button was tapped at [now]: ends the open pause, or starts a new one. */
    fun togglePause(pauses: List<PausePair>, now: Long): List<PausePair> {
        val open = openPause(pauses)
        return when {
            open == null -> pauses + PausePair(start = now)
            open.start == null -> pauses.dropLast(1) + open.copy(start = now)
            now < open.start -> pauses
            else -> pauses.dropLast(1) + open.copy(end = now)
        }
    }

    /**
     * The history entry for a finished day, or null when the exit is not after the entry.
     * An exit earlier than the entry means the next day (see [WorkTimeCalculator.rollToNextDayIfBefore]).
     */
    fun completedEntry(
        enterTime: Long,
        pauses: List<PausePair>,
        exitTime: Long,
        dailyHours: Double,
        zone: ZoneId = ZoneId.systemDefault(),
    ): WorkHistoryEntry? {
        val exit = WorkTimeCalculator.rollToNextDayIfBefore(exitTime, enterTime, zone)
        // Leaving while on a break: the break ends when the day does.
        val closed = pauses.map { if (it.start != null && it.end == null) it.copy(end = exit) else it }
        val worked = WorkTimeCalculator.workedMillis(enterTime, exit, closed) ?: return null
        return WorkHistoryEntry(
            id = enterTime.toString(),
            enterTime = enterTime,
            pauses = closed,
            exitTime = exit,
            totalWorkedTime = WorkTimeCalculator.formatDuration(worked),
            dailyHoursTarget = dailyHours,
        )
    }
}

/** Where the day in progress stands at a given moment: what the persistent notification shows. */
data class SessionProgress(
    val phase: Phase,
    val workedMillis: Long,
    val targetMillis: Long,
    /** Enter + target + all pauses so far, including the one in progress. */
    val expectedExit: Long?,
    /** Start of the pause in progress, when [phase] is [Phase.OnPause]. */
    val pauseStart: Long?,
) {
    enum class Phase { Working, OnPause, GoalReached }

    /** 0..1 */
    val fraction: Float
        get() = if (targetMillis > 0) (workedMillis.toFloat() / targetMillis).coerceIn(0f, 1f) else 0f

    val remainingMillis: Long get() = (targetMillis - workedMillis).coerceAtLeast(0)
    val overtimeMillis: Long get() = (workedMillis - targetMillis).coerceAtLeast(0)

    companion object {
        private const val MS_PER_HOUR = 3_600_000.0

        fun of(session: ActiveSession, dailyHours: Double, now: Long): SessionProgress {
            val target = (dailyHours * MS_PER_HOUR).toLong()
            val worked = WorkTimeCalculator.workedSoFarMillis(session.enterTime, now, session.pauses)
            val pauseStart = openPause(session.pauses)?.start
            val openPauseMillis = pauseStart?.let { (now - it).coerceAtLeast(0) } ?: 0L
            val expected = WorkTimeCalculator.expectedExit(session.enterTime, session.pauses, dailyHours)
                ?.plus(openPauseMillis)
            val phase = when {
                pauseStart != null -> Phase.OnPause
                target > 0 && worked >= target -> Phase.GoalReached
                else -> Phase.Working
            }
            return SessionProgress(phase, worked, target, expected, pauseStart)
        }

        private fun openPause(pauses: List<PausePair>) = SessionRules.openPause(pauses)
    }
}
