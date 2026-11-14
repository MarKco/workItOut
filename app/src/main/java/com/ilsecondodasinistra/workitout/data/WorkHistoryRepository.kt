package com.ilsecondodasinistra.workitout.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.io.IOException
import java.time.DayOfWeek

const val DEFAULT_DAILY_HOURS = 8.0

/** Monday to Friday. */
val DEFAULT_WORK_DAYS: Set<DayOfWeek> = setOf(
    DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY,
)

interface HistoryRepository {
    val history: Flow<List<WorkHistoryEntry>>
    val dailyHours: Flow<Double>
    val workDays: Flow<Set<DayOfWeek>>
    val activeSession: Flow<ActiveSession?>

    /** Use wallpaper-based colors (Android 12+) instead of the app palette. Off by default. */
    val dynamicColor: Flow<Boolean>

    /** Newest entry goes first; an entry with the same id is replaced. */
    suspend fun addEntry(entry: WorkHistoryEntry)
    suspend fun replaceHistory(entries: List<WorkHistoryEntry>)
    suspend fun clearHistory()
    suspend fun setDailyHours(hours: Double)
    suspend fun setWorkDays(days: Set<DayOfWeek>)
    suspend fun saveActiveSession(session: ActiveSession?)
    suspend fun setDynamicColor(enabled: Boolean)
}

/**
 * [HistoryRepository] backed by Preferences DataStore. The key names and value formats are kept
 * compatible with every version released so far, so existing users keep their data.
 */
class DataStoreHistoryRepository(
    private val dataStore: DataStore<Preferences>,
) : HistoryRepository {

    private object Keys {
        val HISTORY_JSON = stringPreferencesKey("work_history_list_json")
        val DAILY_HOURS = doublePreferencesKey("daily_hours")
        /** Comma separated [DayOfWeek] names; absent means [DEFAULT_WORK_DAYS]. */
        val WORK_DAYS = stringPreferencesKey("work_days")
        val ENTER_TIME = longPreferencesKey("current_enter_time")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")

        /** Legacy: pauses as an unordered string set, one JSON object per pause. */
        val PAUSES_LEGACY_SET = stringSetPreferencesKey("current_pauses_json")

        /** Current: pauses as one ordered JSON array. */
        val PAUSES_JSON = stringPreferencesKey("current_pauses_list_json")
    }

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }
    private val historySerializer = ListSerializer(WorkHistoryEntry.serializer())
    private val pausesSerializer = ListSerializer(PausePair.serializer())

    private val safeData: Flow<Preferences> = dataStore.data.catch { e ->
        if (e is IOException) emit(emptyPreferences()) else throw e
    }

    override val history: Flow<List<WorkHistoryEntry>> =
        safeData.map { decodeHistory(it[Keys.HISTORY_JSON]) }.distinctUntilChanged()

    override val dailyHours: Flow<Double> =
        safeData.map { it[Keys.DAILY_HOURS] ?: DEFAULT_DAILY_HOURS }.distinctUntilChanged()

    override val workDays: Flow<Set<DayOfWeek>> =
        safeData.map { prefs -> prefs[Keys.WORK_DAYS]?.let(::decodeWorkDays) ?: DEFAULT_WORK_DAYS }
            .distinctUntilChanged()

    override val activeSession: Flow<ActiveSession?> =
        safeData.map { prefs ->
            val enter = prefs[Keys.ENTER_TIME] ?: return@map null
            ActiveSession(enter, decodePauses(prefs))
        }.distinctUntilChanged()

    override val dynamicColor: Flow<Boolean> =
        safeData.map { it[Keys.DYNAMIC_COLOR] ?: false }.distinctUntilChanged()

    override suspend fun setDynamicColor(enabled: Boolean) {
        dataStore.edit { it[Keys.DYNAMIC_COLOR] = enabled }
    }

    override suspend fun addEntry(entry: WorkHistoryEntry) {
        dataStore.edit { prefs ->
            val others = decodeHistory(prefs[Keys.HISTORY_JSON]).filterNot { it.id == entry.id }
            val updated = listOf(entry) + others
            prefs[Keys.HISTORY_JSON] = json.encodeToString(historySerializer, updated)
        }
    }

    override suspend fun replaceHistory(entries: List<WorkHistoryEntry>) {
        dataStore.edit { it[Keys.HISTORY_JSON] = json.encodeToString(historySerializer, entries) }
    }

    override suspend fun clearHistory() {
        dataStore.edit { it.remove(Keys.HISTORY_JSON) }
    }

    override suspend fun setDailyHours(hours: Double) {
        dataStore.edit { it[Keys.DAILY_HOURS] = hours }
    }

    override suspend fun setWorkDays(days: Set<DayOfWeek>) {
        dataStore.edit { it[Keys.WORK_DAYS] = days.sorted().joinToString(",") { day -> day.name } }
    }

    override suspend fun saveActiveSession(session: ActiveSession?) {
        dataStore.edit { prefs ->
            prefs.remove(Keys.PAUSES_LEGACY_SET)
            if (session == null) {
                prefs.remove(Keys.ENTER_TIME)
                prefs.remove(Keys.PAUSES_JSON)
            } else {
                prefs[Keys.ENTER_TIME] = session.enterTime
                if (session.pauses.isEmpty()) {
                    prefs.remove(Keys.PAUSES_JSON)
                } else {
                    prefs[Keys.PAUSES_JSON] = json.encodeToString(pausesSerializer, session.pauses)
                }
            }
        }
    }

    private fun decodeHistory(raw: String?): List<WorkHistoryEntry> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching { json.decodeFromString(historySerializer, raw) }.getOrDefault(emptyList())
    }

    private fun decodeWorkDays(raw: String): Set<DayOfWeek> =
        raw.split(',').mapNotNull { name -> DayOfWeek.entries.firstOrNull { it.name == name.trim() } }.toSet()

    private fun decodePauses(prefs: Preferences): List<PausePair> {
        prefs[Keys.PAUSES_JSON]?.let { raw ->
            return runCatching { json.decodeFromString(pausesSerializer, raw) }.getOrDefault(emptyList())
        }
        // Legacy set has no order: restore chronological order from the pause start times.
        return prefs[Keys.PAUSES_LEGACY_SET].orEmpty()
            .mapNotNull { runCatching { json.decodeFromString(PausePair.serializer(), it) }.getOrNull() }
            .sortedBy { it.start ?: Long.MAX_VALUE }
    }
}
