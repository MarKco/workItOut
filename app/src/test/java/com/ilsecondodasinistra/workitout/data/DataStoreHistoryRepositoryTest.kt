package com.ilsecondodasinistra.workitout.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class DataStoreHistoryRepositoryTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var file: File
    private val dataStore by lazy { PreferenceDataStoreFactory.create(scope = scope, produceFile = { file }) }
    private val repository by lazy { DataStoreHistoryRepository(dataStore) }

    @Before
    fun setUp() {
        file = File(tmp.root, "test.preferences_pb")
    }

    @After
    fun tearDown() = scope.cancel()

    private fun entry(id: String, enter: Long = id.toLong()) = WorkHistoryEntry(
        id = id,
        enterTime = enter,
        pauses = listOf(PausePair(enter + 10, enter + 20)),
        exitTime = enter + 100,
        totalWorkedTime = "8h 0m",
        dailyHoursTarget = 8.0,
    )

    @Test
    fun defaults_whenEmpty() = runBlocking {
        assertEquals(8.0, repository.dailyHours.first(), 0.0)
        assertEquals(emptyList<WorkHistoryEntry>(), repository.history.first())
        assertNull(repository.activeSession.first())
    }

    @Test
    fun addEntry_putsNewestFirst() = runBlocking {
        repository.addEntry(entry("1"))
        repository.addEntry(entry("2"))
        assertEquals(listOf("2", "1"), repository.history.first().map { it.id })
    }

    @Test
    fun addEntry_withSameIdReplacesInsteadOfDuplicating() = runBlocking {
        repository.addEntry(entry("1"))
        repository.addEntry(entry("1").copy(totalWorkedTime = "9h 0m"))
        val history = repository.history.first()
        assertEquals(1, history.size)
        assertEquals("9h 0m", history.single().totalWorkedTime)
    }

    @Test
    fun clearHistory_emptiesList() = runBlocking {
        repository.addEntry(entry("1"))
        repository.clearHistory()
        assertEquals(0, repository.history.first().size)
    }

    @Test
    fun replaceHistory_overwritesEverything() = runBlocking {
        repository.addEntry(entry("1"))
        repository.replaceHistory(listOf(entry("5"), entry("4")))
        assertEquals(listOf("5", "4"), repository.history.first().map { it.id })
    }

    @Test
    fun dailyHours_roundTrips() = runBlocking {
        repository.setDailyHours(7.5)
        assertEquals(7.5, repository.dailyHours.first(), 0.0)
    }

    @Test
    fun activeSession_keepsPauseOrderAndDuplicates() = runBlocking {
        val pauses = listOf(PausePair(30, 40), PausePair(10, 20), PausePair(10, 20), PausePair())
        repository.saveActiveSession(ActiveSession(5, pauses))
        assertEquals(ActiveSession(5, pauses), repository.activeSession.first())
    }

    @Test
    fun activeSession_nullClearsIt() = runBlocking {
        repository.saveActiveSession(ActiveSession(5, listOf(PausePair(1, 2))))
        repository.saveActiveSession(null)
        assertNull(repository.activeSession.first())
    }

    /** Data written by app versions <= 3.6.0: pauses in an unordered string set. */
    @Test
    fun activeSession_readsLegacyPauseSetInChronologicalOrder() = runBlocking {
        dataStore.edit {
            it[longPreferencesKey("current_enter_time")] = 1_000
            it[stringSetPreferencesKey("current_pauses_json")] = setOf(
                """{"start":5000,"end":6000}""",
                """{"start":2000,"end":3000}""",
                """{"start":8000,"end":null}""",
            )
        }
        val session = repository.activeSession.first()!!
        assertEquals(1_000L, session.enterTime)
        assertEquals(
            listOf(PausePair(2000, 3000), PausePair(5000, 6000), PausePair(8000, null)),
            session.pauses,
        )
    }

    @Test
    fun savingSession_removesLegacyPauseSet() = runBlocking {
        dataStore.edit {
            it[longPreferencesKey("current_enter_time")] = 1_000
            it[stringSetPreferencesKey("current_pauses_json")] = setOf("""{"start":1,"end":2}""")
        }
        repository.saveActiveSession(ActiveSession(1_000, emptyList()))
        assertEquals(emptyList<PausePair>(), repository.activeSession.first()!!.pauses)
    }

    /** History JSON as written by released versions. */
    @Test
    fun history_readsExistingJsonFormat() = runBlocking {
        dataStore.edit {
            it[stringPreferencesKey("work_history_list_json")] = """
                [{"id":"1700000000000","enterTime":1700000000000,
                  "pauses":[{"start":1700003600000,"end":1700005400000}],
                  "exitTime":1700030000000,"totalWorkedTime":"7h 30m","dailyHoursTarget":8.0}]
            """.trimIndent()
        }
        val entry = repository.history.first().single()
        assertEquals("7h 30m", entry.totalWorkedTime)
        assertEquals(PausePair(1700003600000, 1700005400000), entry.pauses.single())
    }

    @Test
    fun history_corruptJsonYieldsEmptyList() = runBlocking {
        dataStore.edit { it[stringPreferencesKey("work_history_list_json")] = "{not json" }
        assertEquals(0, repository.history.first().size)
    }

    @Test
    fun workDays_defaultToWeekdaysAndRoundTrip() = runBlocking {
        assertEquals(DEFAULT_WORK_DAYS, repository.workDays.first())
        repository.setWorkDays(setOf(java.time.DayOfWeek.SATURDAY, java.time.DayOfWeek.MONDAY))
        assertEquals(setOf(java.time.DayOfWeek.MONDAY, java.time.DayOfWeek.SATURDAY), repository.workDays.first())
        repository.setWorkDays(emptySet())
        assertEquals(emptySet<java.time.DayOfWeek>(), repository.workDays.first())
    }
}
