package com.ilsecondodasinistra.workitout

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.ilsecondodasinistra.workitout.alarm.AlarmManagerExitReminderScheduler
import com.ilsecondodasinistra.workitout.alarm.ExitReminderScheduler
import com.ilsecondodasinistra.workitout.data.DataStoreHistoryRepository
import com.ilsecondodasinistra.workitout.data.HistoryRepository
import com.ilsecondodasinistra.workitout.session.SessionController
import com.ilsecondodasinistra.workitout.session.SessionNotifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

private val Context.workDataStore: DataStore<Preferences> by preferencesDataStore(name = "work_history_datastore")

/** Hand-written dependency container: one place that wires the app together. */
class AppContainer(private val context: Context) {
    val historyRepository: HistoryRepository = DataStoreHistoryRepository(context.workDataStore)
    val exitReminderScheduler: ExitReminderScheduler = AlarmManagerExitReminderScheduler(context)
    val sessionController = SessionController(historyRepository, exitReminderScheduler)
    val sessionNotifier = SessionNotifier(context, historyRepository)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /**
     * Keeps the persistent notification in step with the stored day and the settings, for as long as
     * the process lives. A fresh process started by a notification button or the minute tick calls this too.
     */
    fun startSessionNotification() {
        scope.launch {
            combine(
                historyRepository.activeSession,
                historyRepository.dailyHours,
                historyRepository.sessionNotification,
                historyRepository.notificationCountdown,
            ) { _, _, _, _ -> }.collectLatest { sessionNotifier.sync() }
        }
        // The minute tick does not wake the device: bring the progress up to date when the screen comes back.
        ContextCompat.registerReceiver(
            context,
            object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    scope.launch { sessionNotifier.sync() }
                }
            },
            IntentFilter(Intent.ACTION_SCREEN_ON).apply { addAction(Intent.ACTION_USER_PRESENT) },
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
    }
}

class WorkItOutApp : Application() {
    val container: AppContainer by lazy { AppContainer(this) }

    override fun onCreate() {
        super.onCreate()
        container.startSessionNotification()
    }
}
