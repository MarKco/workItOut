package com.ilsecondodasinistra.workitout

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.ilsecondodasinistra.workitout.alarm.EXTRA_TRIGGER_AT
import com.ilsecondodasinistra.workitout.alarm.NOTIFICATION_CHANNEL_ID
import com.ilsecondodasinistra.workitout.alarm.NOTIFICATION_ID
import com.ilsecondodasinistra.workitout.domain.TimeFormats

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (!canPostNotifications(context)) return

        val exitTime = TimeFormats.time(intent.getLongExtra(EXTRA_TRIGGER_AT, -1).takeIf { it > 0 })
        val notification = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(context.getString(R.string.notification_exit_time_title))
            .setContentText(context.getString(R.string.notification_exit_time_body, exitTime))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {
            // Permission revoked between the check and the call: nothing to show.
        }
    }

    private fun canPostNotifications(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
}
