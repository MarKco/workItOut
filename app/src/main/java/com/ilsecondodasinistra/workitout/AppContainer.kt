package com.ilsecondodasinistra.workitout

import android.app.Application
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.ilsecondodasinistra.workitout.alarm.AlarmManagerExitReminderScheduler
import com.ilsecondodasinistra.workitout.alarm.ExitReminderScheduler
import com.ilsecondodasinistra.workitout.data.DataStoreHistoryRepository
import com.ilsecondodasinistra.workitout.data.HistoryRepository

private val Context.workDataStore: DataStore<Preferences> by preferencesDataStore(name = "work_history_datastore")

/** Hand-written dependency container: one place that wires the app together. */
class AppContainer(context: Context) {
    val historyRepository: HistoryRepository = DataStoreHistoryRepository(context.workDataStore)
    val exitReminderScheduler: ExitReminderScheduler = AlarmManagerExitReminderScheduler(context)
}

class WorkItOutApp : Application() {
    val container: AppContainer by lazy { AppContainer(this) }
}
