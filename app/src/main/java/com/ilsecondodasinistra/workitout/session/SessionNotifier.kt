package com.ilsecondodasinistra.workitout.session

import android.Manifest
import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Icon
import android.os.Build
import android.os.PowerManager
import com.ilsecondodasinistra.workitout.MainActivity
import com.ilsecondodasinistra.workitout.R
import com.ilsecondodasinistra.workitout.data.ActiveSession
import com.ilsecondodasinistra.workitout.data.HistoryRepository
import com.ilsecondodasinistra.workitout.domain.SessionProgress
import com.ilsecondodasinistra.workitout.domain.TimeFormats
import com.ilsecondodasinistra.workitout.domain.WorkTimeCalculator
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Persistent notification with the progress of the day, shown from entry to exit (a Live Update on
 * Android 16+). There is no foreground service: the system draws the chronometer by itself, and a
 * non-waking alarm refreshes the rest once a minute and whenever the screen comes back on.
 *
 * On Android 14+ the user can swipe it away; it then stays hidden until the next day starts.
 */
class SessionNotifier(
    private val context: Context,
    private val repository: HistoryRepository,
) {
    private val notifications = context.getSystemService(NotificationManager::class.java)
    private val alarms = context.getSystemService(AlarmManager::class.java)
    private val power = context.getSystemService(PowerManager::class.java)
    private val state = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** One update at a time: a refresh that started before an exit must not post the closed day again. */
    private val lock = Mutex()

    suspend fun sync(now: Long = System.currentTimeMillis()) = lock.withLock {
        val session = repository.activeSession.first()
        val enabled = repository.sessionNotification.first()
        val visible = session != null &&
            enabled &&
            canNotify() &&
            !WorkTimeCalculator.isStale(session.enterTime, now) &&
            state.getLong(KEY_DISMISSED, 0L) != session.enterTime
        if (!visible) {
            alarms.cancel(tickIntent())
            notifications.cancel(NOTIFICATION_ID)
            return
        }
        alarms.set(AlarmManager.RTC, now + TICK_MS, tickIntent())
        // Screen off and already shown: reposting could wake the screen, and SCREEN_ON refreshes it anyway.
        if (!power.isInteractive && notifications.activeNotifications.any { it.id == NOTIFICATION_ID }) return
        ensureChannel()
        val countdown = repository.notificationCountdown.first()
        notifications.notify(NOTIFICATION_ID, build(session!!, repository.dailyHours.first(), countdown, now))
    }

    /** The user swiped the notification of the day started at [enterTime] away: no more of it until the next day. */
    fun dismissed(enterTime: Long) {
        state.edit().putLong(KEY_DISMISSED, enterTime).apply()
    }

    /** POST_NOTIFICATIONS exists from Android 13: before, the app-level switch is all there is. */
    fun canNotify(): Boolean =
        (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) &&
            notifications.areNotificationsEnabled()

    private fun build(session: ActiveSession, dailyHours: Double, countdown: Boolean, now: Long): Notification {
        val progress = SessionProgress.of(session, dailyHours, now)
        val worked = WorkTimeCalculator.formatDuration(progress.workedMillis)
        val exit = TimeFormats.timeShort(progress.expectedExit)
        val onPause = progress.phase == SessionProgress.Phase.OnPause

        // The system draws the chronometer right after the title: the title says what it counts.
        val title: String
        val text: String
        val chronometerStart: Long
        var chronometerDown = false
        var chipText: String? = null
        when (progress.phase) {
            SessionProgress.Phase.Working -> {
                if (countdown) {
                    title = context.getString(R.string.hero_remaining)
                    text = context.getString(R.string.notif_text_exit, exit)
                    chronometerStart = progress.expectedExit ?: now
                    chronometerDown = true
                } else {
                    title = context.getString(R.string.stat_worked)
                    val left = WorkTimeCalculator.formatDuration(progress.remainingMillis)
                    text = context.getString(R.string.notif_text_remaining_exit, left, exit)
                    chronometerStart = now - progress.workedMillis
                    chipText = worked
                }
            }
            SessionProgress.Phase.OnPause -> {
                title = context.getString(R.string.status_on_pause)
                text = context.getString(R.string.notif_text_exit, exit)
                chronometerStart = progress.pauseStart ?: now
                chipText = context.getString(R.string.notif_sub_pause)
            }
            SessionProgress.Phase.GoalReached -> {
                title = context.getString(R.string.hero_overtime)
                text = context.getString(R.string.notif_text_goal)
                chronometerStart = progress.expectedExit ?: now
                chipText = WorkTimeCalculator.formatBalance(progress.overtimeMillis)
            }
        }

        val builder = Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_exit)
            .setContentTitle(title)
            .setContentText(text)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(Notification.CATEGORY_PROGRESS)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setWhen(chronometerStart)
            .setShowWhen(true)
            .setUsesChronometer(true)
            .setChronometerCountDown(chronometerDown)
            .setContentIntent(openApp())
            .setDeleteIntent(receiverIntent(SessionNotificationReceiver.ACTION_DISMISSED, session.enterTime))
            .addAction(
                Notification.Action.Builder(
                    Icon.createWithResource(context, R.drawable.ic_stat_exit),
                    context.getString(if (onPause) R.string.action_end_pause else R.string.action_start_pause),
                    receiverIntent(SessionNotificationReceiver.ACTION_TOGGLE_PAUSE),
                ).build(),
            )
            .addAction(
                Notification.Action.Builder(
                    Icon.createWithResource(context, R.drawable.ic_stat_exit),
                    context.getString(R.string.button_exit),
                    receiverIntent(SessionNotificationReceiver.ACTION_EXIT),
                ).build(),
            )

        val fraction = (progress.fraction * PROGRESS_MAX).toInt()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
            builder.setStyle(
                Notification.ProgressStyle()
                    .setProgressSegments(listOf(Notification.ProgressStyle.Segment(PROGRESS_MAX)))
                    .setStyledByProgress(true)
                    .setProgress(fraction),
            )
            // The chip shows this text as it is; without it the system counts the chronometer by itself.
            chipText?.let(builder::setShortCriticalText)
            builder.extras.putBoolean(Notification.EXTRA_REQUEST_PROMOTED_ONGOING, true)
        } else {
            builder.setProgress(PROGRESS_MAX, fraction, false)
        }
        return builder.build()
    }

    private fun ensureChannel() {
        if (notifications.getNotificationChannel(CHANNEL_ID) != null) return
        notifications.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notif_channel_session),
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = context.getString(R.string.notif_channel_session_description)
                setShowBadge(false)
            },
        )
    }

    private fun tickIntent(): PendingIntent = receiverIntent(SessionNotificationReceiver.ACTION_TICK)

    private fun receiverIntent(action: String, enterTime: Long = 0L): PendingIntent = PendingIntent.getBroadcast(
        context,
        action.hashCode(),
        Intent(context, SessionNotificationReceiver::class.java)
            .setAction(action)
            .putExtra(SessionNotificationReceiver.EXTRA_ENTER_TIME, enterTime),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private fun openApp(): PendingIntent = PendingIntent.getActivity(
        context,
        0,
        Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private companion object {
        const val CHANNEL_ID = "session_progress"
        const val NOTIFICATION_ID = 102
        const val PROGRESS_MAX = 1000
        const val TICK_MS = 60_000L
        const val PREFS = "session_notification"
        const val KEY_DISMISSED = "dismissed_enter_time"
    }
}
