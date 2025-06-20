package com.ilsecondodasinistra.workitout.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.IOException

// Create a DataStore instance
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "work_history_datastore")

interface HistoryRepository {
    fun getAllHistoryFlow(): Flow<List<WorkHistoryEntry>>
    suspend fun addWorkHistoryEntry(entry: WorkHistoryEntry)
    suspend fun clearAllHistory()
    fun getDailyHours(): Flow<Double>
    suspend fun saveDailyHours(hours: Double)

    // Methods for current session persistence
    suspend fun saveCurrentSession(enterTimeMillis: Long?, exitTimeMillis: Long?, currentPausesJson: Set<String>?)
    fun getCurrentSession(): Flow<Triple<Long?, Long?, Set<String>?>> // Triple: Enter, Exit, Pauses JSON Set
}

class DataStoreHistoryRepository(private val context: Context) : HistoryRepository {

    // Make PreferencesKeys a companion object or a top-level object if preferred
    // For consistency with typical DataStore examples, keeping it as an inner object.
    private object PreferencesKeys {
        val WORK_HISTORY_LIST_JSON = stringPreferencesKey("work_history_list_json")
        val DAILY_HOURS = doublePreferencesKey("daily_hours")

        // Keys for current session
        val CURRENT_ENTER_TIME = longPreferencesKey("current_enter_time")
        val CURRENT_EXIT_TIME = longPreferencesKey("current_exit_time")
        val CURRENT_PAUSES_JSON = stringSetPreferencesKey("current_pauses_json")
    }

    // This Json instance is for kotlinx.serialization, used for WorkHistoryEntry
    private val json = Json {
        prettyPrint = true
        isLenient = true
        ignoreUnknownKeys = true
    }

    override fun getAllHistoryFlow(): Flow<List<WorkHistoryEntry>> {
        return context.dataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { preferences ->
                val jsonString = preferences[PreferencesKeys.WORK_HISTORY_LIST_JSON]
                if (jsonString != null && jsonString.isNotBlank()) {
                    try {
                        json.decodeFromString(ListSerializer(WorkHistoryEntry.serializer()), jsonString)
                    } catch (e: Exception) {
                        e.printStackTrace() // Log the error
                        emptyList<WorkHistoryEntry>() // Return empty list on error
                    }
                } else {
                    emptyList<WorkHistoryEntry>()
                }
            }
    }

    override suspend fun addWorkHistoryEntry(entry: WorkHistoryEntry) {
        context.dataStore.edit { preferences ->
            val currentJsonString = preferences[PreferencesKeys.WORK_HISTORY_LIST_JSON]
            val currentList: MutableList<WorkHistoryEntry> = if (currentJsonString != null && currentJsonString.isNotBlank()) {
                try {
                    json.decodeFromString(ListSerializer(WorkHistoryEntry.serializer()), currentJsonString).toMutableList()
                } catch (e: Exception) {
                    e.printStackTrace() // Log the error
                    mutableListOf() // Return new list on error
                }
            } else {
                mutableListOf()
            }
            currentList.add(0, entry) // Add new entries to the beginning
            preferences[PreferencesKeys.WORK_HISTORY_LIST_JSON] = json.encodeToString(ListSerializer(WorkHistoryEntry.serializer()), currentList)
        }
    }

    override suspend fun clearAllHistory() {
        context.dataStore.edit { preferences ->
            preferences.remove(PreferencesKeys.WORK_HISTORY_LIST_JSON)
            // preferences.remove(PreferencesKeys.DAILY_HOURS) // Decide if daily hours should be cleared too
        }
    }

    override fun getDailyHours(): Flow<Double> {
        return context.dataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { preferences ->
                preferences[PreferencesKeys.DAILY_HOURS] ?: 8.0 // Default to 8.0 if not set
            }
    }

    override suspend fun saveDailyHours(hours: Double) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.DAILY_HOURS] = hours
        }
    }

    // Implementation for current session persistence
    override suspend fun saveCurrentSession(enterTimeMillis: Long?, exitTimeMillis: Long?, currentPausesJson: Set<String>?) {
        context.dataStore.edit { preferences ->
            if (enterTimeMillis == null) {
                preferences.remove(PreferencesKeys.CURRENT_ENTER_TIME)
            } else {
                preferences[PreferencesKeys.CURRENT_ENTER_TIME] = enterTimeMillis
            }

            if (exitTimeMillis == null) {
                preferences.remove(PreferencesKeys.CURRENT_EXIT_TIME)
            } else {
                preferences[PreferencesKeys.CURRENT_EXIT_TIME] = exitTimeMillis
            }

            if (currentPausesJson == null || currentPausesJson.isEmpty()) { // Also check for isEmpty
                preferences.remove(PreferencesKeys.CURRENT_PAUSES_JSON)
            } else {
                preferences[PreferencesKeys.CURRENT_PAUSES_JSON] = currentPausesJson
            }
        }
    }

    override fun getCurrentSession(): Flow<Triple<Long?, Long?, Set<String>?>> {
        return context.dataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { preferences ->
                val enterTime = preferences[PreferencesKeys.CURRENT_ENTER_TIME]
                val exitTime = preferences[PreferencesKeys.CURRENT_EXIT_TIME]
                val pausesJson = preferences[PreferencesKeys.CURRENT_PAUSES_JSON]
                Triple(enterTime, exitTime, pausesJson)
            }
    }
}