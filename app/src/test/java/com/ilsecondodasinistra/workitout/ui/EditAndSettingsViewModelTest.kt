package com.ilsecondodasinistra.workitout.ui

import com.ilsecondodasinistra.workitout.R
import com.ilsecondodasinistra.workitout.data.PausePair
import com.ilsecondodasinistra.workitout.data.WorkHistoryEntry
import com.ilsecondodasinistra.workitout.domain.HistoryBackup
import com.ilsecondodasinistra.workitout.testutil.FakeRepository
import com.ilsecondodasinistra.workitout.testutil.MutableClock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

@OptIn(ExperimentalCoroutinesApi::class)
class EditAndSettingsViewModelTest {

    private val clock = MutableClock(Instant.parse("2026-03-10T12:00:00Z"))
    private lateinit var repository: FakeRepository

    private fun at(s: String) = Instant.parse(s).toEpochMilli()

    private val existing = WorkHistoryEntry(
        id = "e1",
        enterTime = at("2026-03-09T09:00:00Z"),
        pauses = listOf(PausePair(at("2026-03-09T12:00:00Z"), at("2026-03-09T13:00:00Z"))),
        exitTime = at("2026-03-09T18:00:00Z"),
        totalWorkedTime = "8h 0m",
        dailyHoursTarget = 8.0,
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        repository = FakeRepository()
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun editVm(id: String?) = EditEntryViewModel(repository, id, clock)

    @Test
    fun edit_loadsExistingEntry() {
        repository.history.value = listOf(existing)
        val state = editVm("e1").uiState.value
        assertFalse(state.isNew)
        assertEquals(LocalDate.of(2026, 3, 9), state.draft?.date)
        assertEquals(1, state.draft?.pauses?.size)
        assertEquals("8", state.targetInput)
    }

    @Test
    fun new_startsFromTodayAndDailyHours() {
        repository.dailyHours.value = 7.5
        val state = editVm(null).uiState.value
        assertTrue(state.isNew)
        assertEquals(LocalDate.of(2026, 3, 10), state.draft?.date)
        assertEquals("7.5", state.targetInput)
    }

    @Test
    fun save_updatesEntryKeepingIdAndRecomputesTotal() {
        repository.history.value = listOf(existing)
        val vm = editVm("e1")
        vm.setExit(LocalTime.of(17, 0))
        vm.save()

        val saved = repository.history.value.single()
        assertEquals("e1", saved.id)
        assertEquals(at("2026-03-09T17:00:00Z"), saved.exitTime)
        assertEquals("7h 0m", saved.totalWorkedTime)
        assertTrue(vm.uiState.value.finished)
    }

    @Test
    fun save_newEntryGetsEnterTimeAsIdAndStaysSortedNewestFirst() {
        repository.history.value = listOf(existing)
        val vm = editVm(null) // 2026-03-10 09:00-18:00
        vm.save()

        val ids = repository.history.value.map { it.enterTime }
        assertEquals(listOf(at("2026-03-10T09:00:00Z"), existing.enterTime), ids)
    }

    @Test
    fun save_newEntryWithTakenIdDoesNotOverwrite() {
        val vm = editVm(null)
        vm.save()
        val second = editVm(null)
        second.save()
        assertEquals(2, repository.history.value.size)
        assertEquals(2, repository.history.value.map { it.id }.toSet().size)
    }

    @Test
    fun save_invalidDraftShowsErrorAndKeepsHistory() {
        repository.history.value = listOf(existing)
        val vm = editVm("e1")
        vm.addPause()
        vm.setPauseStart(1, LocalTime.of(10, 0)) // no end
        vm.save()

        assertEquals(R.string.error_draft_pause_incomplete, vm.uiState.value.message?.resId)
        assertFalse(vm.uiState.value.finished)
        assertEquals(listOf(existing), repository.history.value)
    }

    @Test
    fun save_badTargetShowsError() {
        val vm = editVm(null)
        vm.onTargetInputChange("30")
        vm.save()
        assertEquals(R.string.error_draft_invalid_target, vm.uiState.value.message?.resId)
    }

    @Test
    fun delete_removesEntryAfterConfirmation() {
        repository.history.value = listOf(existing)
        val vm = editVm("e1")
        vm.requestDelete()
        assertTrue(vm.uiState.value.showDeleteConfirm)
        vm.confirmDelete()
        assertTrue(repository.history.value.isEmpty())
        assertTrue(vm.uiState.value.finished)
    }

    @Test
    fun settings_toggleWorkDay() {
        val vm = SettingsViewModel(repository, clock)
        vm.toggleWorkDay(DayOfWeek.SATURDAY)
        assertTrue(DayOfWeek.SATURDAY in repository.workDays.value)
        vm.toggleWorkDay(DayOfWeek.MONDAY)
        assertFalse(DayOfWeek.MONDAY in repository.workDays.value)
        assertTrue(DayOfWeek.MONDAY in vm.uiState.value.workDays.let { setOf(DayOfWeek.MONDAY).minus(it) })
    }

    @Test
    fun settings_rejectsInvalidDailyHours() {
        val vm = SettingsViewModel(repository, clock)
        vm.onDailyHoursInputChange("25")
        vm.saveDailyHours()
        assertEquals(R.string.error_invalid_hours_value, vm.uiState.value.message?.resId)
        assertEquals(8.0, repository.dailyHours.value, 0.0)
    }

    @Test
    fun settings_backupThenRestoreRoundTrip() = runBlocking {
        repository.history.value = listOf(existing)
        repository.dailyHours.value = 7.0
        repository.workDays.value = setOf(DayOfWeek.MONDAY)
        val vm = SettingsViewModel(repository, clock)
        val backup = vm.createBackup()

        val other = FakeRepository()
        val restoreVm = SettingsViewModel(other, clock)
        restoreVm.onBackupRead(backup)
        assertNotNull(restoreVm.uiState.value.pendingRestore)
        assertTrue(other.history.value.isEmpty()) // nothing changes before confirmation

        restoreVm.confirmRestore()
        assertEquals(listOf(existing), other.history.value)
        assertEquals(7.0, other.dailyHours.value, 0.0)
        assertEquals(setOf(DayOfWeek.MONDAY), other.workDays.value)
        assertEquals(R.string.backup_restored, restoreVm.uiState.value.message?.resId)
    }

    @Test
    fun settings_invalidBackupChangesNothing() {
        repository.history.value = listOf(existing)
        val vm = SettingsViewModel(repository, clock)
        vm.onBackupRead("not a backup")
        assertNull(vm.uiState.value.pendingRestore)
        assertEquals(R.string.backup_invalid, vm.uiState.value.message?.resId)
        assertEquals(listOf(existing), repository.history.value)
    }

    @Test
    fun settings_cancelRestoreDropsPendingBackup() {
        val vm = SettingsViewModel(repository, clock)
        vm.onBackupRead(HistoryBackup.encode(listOf(existing), 8.0, emptySet(), 0))
        vm.cancelRestore()
        assertNull(vm.uiState.value.pendingRestore)
        assertTrue(repository.history.value.isEmpty())
    }

    @Test
    fun settings_backupFileNameUsesToday() {
        assertEquals("workitout-backup-2026-03-10.json", SettingsViewModel(repository, clock).backupFileName())
    }
}
