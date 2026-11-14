package com.ilsecondodasinistra.workitout.session

import com.ilsecondodasinistra.workitout.alarm.ExitReminderScheduler
import com.ilsecondodasinistra.workitout.data.HistoryRepository
import com.ilsecondodasinistra.workitout.data.WorkHistoryEntry
import com.ilsecondodasinistra.workitout.domain.SessionRules
import com.ilsecondodasinistra.workitout.domain.WorkTimeCalculator
import kotlinx.coroutines.flow.first
import java.time.Clock

/**
 * Changes the day in progress straight in the repository, with no screen involved. Used by the
 * buttons of the persistent notification, which must work with the app closed.
 */
class SessionController(
    private val repository: HistoryRepository,
    private val reminder: ExitReminderScheduler,
    private val clock: Clock = Clock.systemDefaultZone(),
) {
    /** Starts a break, or ends the one in progress. A session left open on a previous day is not touched. */
    suspend fun togglePause() {
        val session = repository.activeSession.first() ?: return
        if (WorkTimeCalculator.isStale(session.enterTime, clock.millis(), clock.zone)) return

        val pauses = SessionRules.togglePause(session.pauses, clock.millis())
        repository.saveActiveSession(session.copy(pauses = pauses))
        // Ending a break pushes the expected exit later: move the reminder with it.
        val expected = WorkTimeCalculator.expectedExit(session.enterTime, pauses, repository.dailyHours.first())
        if (expected != null) reminder.schedule(expected) else reminder.cancel()
    }

    /** Ends the day now and saves it in the history. Returns the saved day, or null if nothing was open. */
    suspend fun finish(): WorkHistoryEntry? {
        val session = repository.activeSession.first() ?: return null
        if (WorkTimeCalculator.isStale(session.enterTime, clock.millis(), clock.zone)) return null

        val entry = SessionRules.completedEntry(
            enterTime = session.enterTime,
            pauses = session.pauses,
            exitTime = clock.millis(),
            dailyHours = repository.dailyHours.first(),
            zone = clock.zone,
        ) ?: return null
        repository.addEntry(entry)
        repository.saveActiveSession(null)
        reminder.cancel()
        return entry
    }
}
