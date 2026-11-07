package com.ilsecondodasinistra.workitout.alarm

import com.ilsecondodasinistra.workitout.data.HistoryRepository
import com.ilsecondodasinistra.workitout.domain.WorkTimeCalculator
import kotlinx.coroutines.flow.first

/**
 * Re-creates the exit reminder from the stored session. Alarms do not survive a reboot or an
 * app update, so this runs from [com.ilsecondodasinistra.workitout.ReminderRestoreReceiver].
 */
class ExitReminderRestorer(
    private val repository: HistoryRepository,
    private val scheduler: ExitReminderScheduler,
) {
    suspend fun restore() {
        val session = repository.activeSession.first() ?: return
        val dailyHours = repository.dailyHours.first()
        val exit = WorkTimeCalculator.expectedExit(session.enterTime, session.pauses, dailyHours) ?: return
        scheduler.schedule(exit)
    }
}
