package com.ilsecondodasinistra.workitout

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.ilsecondodasinistra.workitout.alarm.ExitReminderRestorer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Restores the exit reminder after reboot, app update, or when exact alarms get allowed. */
class ReminderRestoreReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in HANDLED_ACTIONS) return
        val container = (context.applicationContext as WorkItOutApp).container
        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                ExitReminderRestorer(container.historyRepository, container.exitReminderScheduler).restore()
            } finally {
                pending.finish()
            }
        }
    }

    private companion object {
        val HANDLED_ACTIONS = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            "android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED",
        )
    }
}
