package com.ilsecondodasinistra.workitout.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.ilsecondodasinistra.workitout.AlarmReceiver

const val NOTIFICATION_CHANNEL_ID = "work_tracker_channel"
const val NOTIFICATION_ID = 101
const val EXTRA_TRIGGER_AT = "trigger_at"

/** Schedules the "time to leave" notification. */
interface ExitReminderScheduler {
    /**
     * Schedules the reminder, replacing any previous one.
     * @return true if it is exact (or already in the past); false if the exact-alarm permission
     * is missing and the reminder fell back to an inexact alarm that may fire a few minutes late.
     */
    fun schedule(triggerAtMillis: Long): Boolean
    fun cancel()
}

class AlarmManagerExitReminderScheduler(private val context: Context) : ExitReminderScheduler {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    private fun canScheduleExact(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()

    override fun schedule(triggerAtMillis: Long): Boolean {
        if (triggerAtMillis <= System.currentTimeMillis()) return true
        val pendingIntent = pendingIntent(
            Intent(context, AlarmReceiver::class.java).putExtra(EXTRA_TRIGGER_AT, triggerAtMillis),
        )
        val exact = canScheduleExact()
        if (exact) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }
        return exact
    }

    override fun cancel() {
        alarmManager.cancel(pendingIntent(Intent(context, AlarmReceiver::class.java)))
    }

    private fun pendingIntent(intent: Intent) = PendingIntent.getBroadcast(
        context,
        NOTIFICATION_ID,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
}
