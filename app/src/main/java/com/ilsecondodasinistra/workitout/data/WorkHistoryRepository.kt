package com.ilsecondodasinistra.workitout.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
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
}

class DataStoreHistoryRepository(private val context: Context) : HistoryRepository {

    private object PreferencesKeys {
        val WORK_HISTORY_LIST_JSON = stringPreferencesKey("work_history_list_json")
        val DAILY_HOURS = doublePreferencesKey("daily_hours")
    }

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
                        e.printStackTrace()
                        emptyList<WorkHistoryEntry>()
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
                    e.printStackTrace()
                    mutableListOf()
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
            // Optionally, you might want to clear daily hours too, or keep it
            // preferences.remove(PreferencesKeys.DAILY_HOURS)
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
}
