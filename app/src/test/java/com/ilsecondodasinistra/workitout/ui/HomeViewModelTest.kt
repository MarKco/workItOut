package com.ilsecondodasinistra.workitout.ui

import com.ilsecondodasinistra.workitout.R
import com.ilsecondodasinistra.workitout.alarm.ExitReminderScheduler
import com.ilsecondodasinistra.workitout.data.ActiveSession
import com.ilsecondodasinistra.workitout.data.HistoryRepository
import com.ilsecondodasinistra.workitout.data.PausePair
import com.ilsecondodasinistra.workitout.data.WorkHistoryEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private class FakeRepository : HistoryRepository {
        override val history = MutableStateFlow<List<WorkHistoryEntry>>(emptyList())
        override val dailyHours = MutableStateFlow(8.0)
        override val activeSession = MutableStateFlow<ActiveSession?>(null)
        override suspend fun addEntry(entry: WorkHistoryEntry) {
            history.value = listOf(entry) + history.value.filterNot { it.id == entry.id }
        }
        override suspend fun replaceHistory(entries: List<WorkHistoryEntry>) { history.value = entries }
        override suspend fun clearHistory() { history.value = emptyList() }
        override suspend fun setDailyHours(hours: Double) { dailyHours.value = hours }
        override suspend fun saveActiveSession(session: ActiveSession?) { activeSession.value = session }
    }

    private class FakeReminder(var canSchedule: Boolean = true) : ExitReminderScheduler {
        val scheduled = mutableListOf<Long>()
        var cancelCount = 0
        override fun schedule(triggerAtMillis: Long): Boolean {
            if (!canSchedule) return false
            scheduled += triggerAtMillis
            return true
        }
        override fun cancel() { cancelCount++ }
    }

    private class MutableClock(var now: Instant, private val zone: ZoneId = ZoneOffset.UTC) : Clock() {
        override fun getZone(): ZoneId = zone
        override fun withZone(zone: ZoneId): Clock = MutableClock(now, zone)
        override fun instant(): Instant = now
    }

    private val hour = 3_600_000L
    private val minute = 60_000L
    private val nineAm = Instant.parse("2026-03-10T09:00:00Z")

    private lateinit var repository: FakeRepository
    private lateinit var reminder: FakeReminder
    private lateinit var clock: MutableClock

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        repository = FakeRepository()
        reminder = FakeReminder()
        clock = MutableClock(nineAm)
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel() = HomeViewModel(repository, reminder, clock)
    private fun advance(millis: Long) { clock.now = clock.now.plusMillis(millis) }

    @Test
    fun enter_setsEnterTimeExpectedExitAndSchedulesReminder() {
        val vm = viewModel()
        vm.press(ButtonType.Enter)

        val state = vm.uiState.value
        assertEquals(nineAm.toEpochMilli(), state.enterTime)
        assertEquals(nineAm.toEpochMilli() + 8 * hour, state.calculatedExitTime)
        assertEquals(listOf(nineAm.toEpochMilli() + 8 * hour), reminder.scheduled)
        assertEquals(nineAm.toEpochMilli(), repository.activeSession.value?.enterTime)
    }

    @Test
    fun completedPause_pushesExpectedExitAndReminder() {
        val vm = viewModel()
        vm.press(ButtonType.Enter)
        vm.addPause()
        advance(4 * hour); vm.startPause(0)
        advance(45 * minute); vm.endPause(0)

        val expected = nineAm.toEpochMilli() + 8 * hour + 45 * minute
        assertEquals(expected, vm.uiState.value.calculatedExitTime)
        assertEquals(expected, reminder.scheduled.last())
        assertEquals(1, repository.activeSession.value?.pauses?.size)
    }

    @Test
    fun startingAnAlreadyStartedPause_showsErrorAndKeepsIt() {
        val vm = viewModel()
        vm.press(ButtonType.Enter)
        vm.addPause()
        advance(hour); vm.startPause(0)
        val started = vm.uiState.value.pauses[0]
        advance(hour); vm.startPause(0)

        assertEquals(R.string.error_pause_already_started, vm.uiState.value.message?.resId)
        assertEquals(started, vm.uiState.value.pauses[0])
    }

    @Test
    fun endPauseWithoutStart_showsError() {
        val vm = viewModel()
        vm.press(ButtonType.Enter)
        vm.addPause()
        vm.endPause(0)
        assertEquals(R.string.error_must_start_pause_first, vm.uiState.value.message?.resId)
    }

    @Test
    fun addPauseWithoutEnter_showsError() {
        val vm = viewModel()
        vm.addPause()
        assertEquals(R.string.error_must_enter_before_pause, vm.uiState.value.message?.resId)
        assertTrue(vm.uiState.value.pauses.isEmpty())
    }

    @Test
    fun exit_savesHistoryClearsSessionAndCancelsReminder() {
        val vm = viewModel()
        vm.press(ButtonType.Enter)
        vm.addPause()
        advance(4 * hour); vm.startPause(0)
        advance(hour); vm.endPause(0)
        advance(4 * hour); vm.press(ButtonType.Exit)

        val entry = repository.history.value.single()
        assertEquals("8h 0m", entry.totalWorkedTime)
        assertEquals(nineAm.toEpochMilli(), entry.enterTime)
        assertEquals(1, entry.pauses.size)
        assertNull(repository.activeSession.value)
        assertEquals("8h 0m", vm.uiState.value.totalWorkedTime)
        assertNull(vm.uiState.value.calculatedExitTime)
        assertTrue(reminder.cancelCount > 0)
    }

    @Test
    fun exitWithoutEnter_showsErrorAndSavesNothing() {
        val vm = viewModel()
        vm.press(ButtonType.Exit)
        assertEquals(R.string.error_must_enter_first, vm.uiState.value.message?.resId)
        assertTrue(repository.history.value.isEmpty())
    }

    @Test
    fun pressingExitTwice_doesNotDuplicateHistory() {
        val vm = viewModel()
        vm.press(ButtonType.Enter)
        advance(8 * hour); vm.press(ButtonType.Exit)
        advance(hour); vm.press(ButtonType.Exit)
        assertEquals(1, repository.history.value.size)
    }

    @Test
    fun exitPickedBeforeEnter_rollsToNextDay() {
        val vm = viewModel()
        vm.press(ButtonType.Enter) // 09:00
        vm.requestTimeEdit(ButtonType.Exit)
        vm.onTimeEdited(ButtonType.Exit, hour = 8, minute = 0) // 08:00 < 09:00 -> tomorrow

        val entry = repository.history.value.single()
        assertEquals(nineAm.toEpochMilli() + 23 * hour, entry.exitTime)
        assertEquals("23h 0m", entry.totalWorkedTime)
    }

    @Test
    fun editEnterTime_recomputesExpectedExitAndPersists() {
        val vm = viewModel()
        vm.press(ButtonType.Enter)
        vm.onTimeEdited(ButtonType.Enter, hour = 8, minute = 30)

        val enter = Instant.parse("2026-03-10T08:30:00Z").toEpochMilli()
        assertEquals(enter, vm.uiState.value.enterTime)
        assertEquals(enter + 8 * hour, vm.uiState.value.calculatedExitTime)
        assertEquals(enter, repository.activeSession.value?.enterTime)
        assertNull(vm.uiState.value.timePickerEvent)
    }

    @Test
    fun editPauseEnd_beforeStart_rollsToNextDay() {
        val vm = viewModel()
        vm.press(ButtonType.Enter)
        vm.addPause()
        advance(4 * hour); vm.startPause(0) // 13:00
        vm.requestPauseEndEdit(0)
        vm.onTimeEdited(ButtonType.FromLunch, hour = 12, minute = 0)

        val pause = vm.uiState.value.pauses[0]
        assertEquals(Instant.parse("2026-03-11T12:00:00Z").toEpochMilli(), pause.end)
    }

    @Test
    fun editPauseStart_afterEnd_isRejected() {
        val vm = viewModel()
        vm.press(ButtonType.Enter)
        vm.addPause()
        advance(4 * hour); vm.startPause(0) // 13:00
        advance(hour); vm.endPause(0) // 14:00
        val before = vm.uiState.value.pauses[0]
        vm.requestPauseStartEdit(0)
        vm.onTimeEdited(ButtonType.ToLunch, hour = 15, minute = 0)

        assertEquals(R.string.error_pause_start_after_end, vm.uiState.value.message?.resId)
        assertEquals(before, vm.uiState.value.pauses[0])
    }

    @Test
    fun pickerOpensWithCurrentValue() {
        val vm = viewModel()
        vm.press(ButtonType.Enter)
        vm.requestTimeEdit(ButtonType.Enter)
        val event = vm.uiState.value.timePickerEvent!!
        assertEquals(9, event.initialHour)
        assertEquals(0, event.initialMinute)
        vm.dismissTimePicker()
        assertNull(vm.uiState.value.timePickerEvent)
    }

    @Test
    fun reset_clearsDayKeepsHistoryAndCancelsReminder() {
        val vm = viewModel()
        vm.press(ButtonType.Enter)
        vm.resetAll()

        val state = vm.uiState.value
        assertNull(state.enterTime)
        assertNull(state.calculatedExitTime)
        assertNull(repository.activeSession.value)
        assertTrue(reminder.cancelCount > 0)
    }

    @Test
    fun restoresActiveSessionOnStart() {
        repository.activeSession.value = ActiveSession(nineAm.toEpochMilli(), listOf(PausePair(1, 2)))
        repository.dailyHours.value = 6.0
        val vm = viewModel()

        val state = vm.uiState.value
        assertEquals(nineAm.toEpochMilli(), state.enterTime)
        assertEquals(1, state.pauses.size)
        assertEquals(6.0, state.dailyHours, 0.0)
    }

    @Test
    fun dailyHoursChange_updatesExpectedExit() {
        val vm = viewModel()
        vm.press(ButtonType.Enter)
        repository.dailyHours.value = 6.0
        assertEquals(nineAm.toEpochMilli() + 6 * hour, vm.uiState.value.calculatedExitTime)
    }

    @Test
    fun missingExactAlarmPermission_isReported() {
        reminder.canSchedule = false
        val vm = viewModel()
        vm.press(ButtonType.Enter)
        assertTrue(vm.uiState.value.alarmPermissionMissing)
        vm.alarmPermissionHandled()
        assertFalse(vm.uiState.value.alarmPermissionMissing)
    }

    @Test
    fun messageShown_clearsMessage() {
        val vm = viewModel()
        vm.press(ButtonType.Exit)
        vm.messageShown()
        assertNull(vm.uiState.value.message)
    }
}
