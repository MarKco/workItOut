package com.ilsecondodasinistra.workitout.ui

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ilsecondodasinistra.workitout.R
import com.ilsecondodasinistra.workitout.alarm.ExitReminderScheduler
import com.ilsecondodasinistra.workitout.data.ActiveSession
import com.ilsecondodasinistra.workitout.data.DEFAULT_DAILY_HOURS
import com.ilsecondodasinistra.workitout.data.HistoryRepository
import com.ilsecondodasinistra.workitout.data.PausePair
import com.ilsecondodasinistra.workitout.data.WorkHistoryEntry
import com.ilsecondodasinistra.workitout.domain.TimeFormats
import com.ilsecondodasinistra.workitout.domain.WorkTimeCalculator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.Instant
import java.time.LocalDate

enum class ButtonType(@StringRes val textResId: Int) {
    Enter(R.string.button_enter),
    ToLunch(R.string.button_to_lunch),
    FromLunch(R.string.button_from_lunch),
    Exit(R.string.button_exit),
}

data class TimePickerEvent(
    val type: ButtonType,
    val pauseIndex: Int? = null,
    val initialHour: Int,
    val initialMinute: Int,
)

/** All times are epoch milliseconds. */
data class HomeUiState(
    val enterTime: Long? = null,
    val pauses: List<PausePair> = emptyList(),
    val exitTime: Long? = null,
    val calculatedExitTime: Long? = null,
    val totalWorkedTime: String? = null,
    val dailyHours: Double = DEFAULT_DAILY_HOURS,
    val message: UiMessage? = null,
    val timePickerEvent: TimePickerEvent? = null,
    val alarmPermissionMissing: Boolean = false,
)

/** What the Home screen can ask for. Default no-ops make previews trivial. */
interface HomeActions {
    fun press(type: ButtonType) {}
    fun requestTimeEdit(type: ButtonType) {}
    fun onTimeEdited(type: ButtonType, hour: Int, minute: Int) {}
    fun dismissTimePicker() {}
    fun addPause() {}
    fun startNewPause() {}
    fun startPause(index: Int) {}
    fun endPause(index: Int) {}
    fun requestPauseStartEdit(index: Int) {}
    fun requestPauseEndEdit(index: Int) {}
    fun messageShown() {}
    fun alarmPermissionHandled() {}
    fun discardSession() {}

    object None : HomeActions
}

class HomeViewModel(
    private val repository: HistoryRepository,
    private val reminder: ExitReminderScheduler,
    private val clock: Clock = Clock.systemDefaultZone(),
) : ViewModel(), HomeActions {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    /** Exit time the reminder is currently set for, to avoid rescheduling on unrelated updates. */
    private var reminderFor: Long? = null

    init {
        viewModelScope.launch {
            repository.activeSession.first()?.let { session ->
                update { it.copy(enterTime = session.enterTime, pauses = session.pauses) }
            }
            repository.dailyHours.collect { hours -> update { it.copy(dailyHours = hours) } }
        }
    }

    // --- Main buttons -----------------------------------------------------------------------

    override fun press(type: ButtonType) {
        when (type) {
            ButtonType.Enter -> {
                update {
                    it.copy(enterTime = clock.millis(), pauses = emptyList(), exitTime = null, totalWorkedTime = null)
                }
                persistSession()
            }
            ButtonType.Exit -> {
                val state = _uiState.value
                if (state.enterTime == null) {
                    showMessage(R.string.error_must_enter_first)
                } else {
                    finalizeSession(state.exitTime ?: clock.millis())
                }
            }
            ButtonType.ToLunch, ButtonType.FromLunch -> Unit // pauses use startPause/endPause
        }
    }

    override fun requestTimeEdit(type: ButtonType) {
        val state = _uiState.value
        val current = when (type) {
            ButtonType.Enter -> state.enterTime
            ButtonType.Exit -> state.exitTime ?: state.calculatedExitTime
            else -> return
        }
        showTimePicker(type, null, current)
    }

    override fun requestPauseStartEdit(index: Int) {
        val pause = _uiState.value.pauses.getOrNull(index) ?: return showMessage(R.string.error_pause_not_found)
        showTimePicker(ButtonType.ToLunch, index, pause.start)
    }

    override fun requestPauseEndEdit(index: Int) {
        val pause = _uiState.value.pauses.getOrNull(index) ?: return showMessage(R.string.error_pause_not_found)
        val start = pause.start
        val initial = (pause.end ?: start)?.let { if (start != null && it < start) start else it }
        showTimePicker(ButtonType.FromLunch, index, initial)
    }

    override fun onTimeEdited(type: ButtonType, hour: Int, minute: Int) {
        val pauseIndex = _uiState.value.timePickerEvent?.pauseIndex
        update { it.copy(timePickerEvent = null) }
        val selected = editBaseDate(type).atTime(hour, minute).atZone(clock.zone).toInstant().toEpochMilli()

        when (type) {
            ButtonType.Enter -> {
                update { it.copy(enterTime = selected) }
                showMessage(R.string.enter_modified, TimeFormats.time(selected))
                persistSession()
            }
            ButtonType.Exit ->
                if (_uiState.value.enterTime == null) {
                    showMessage(R.string.error_must_enter_first_short)
                } else {
                    finalizeSession(selected)
                }
            ButtonType.ToLunch -> editPauseStart(pauseIndex, selected)
            ButtonType.FromLunch -> editPauseEnd(pauseIndex, selected)
        }
    }

    /**
     * Times are picked without a date. For a session forgotten on an earlier day the exit and
     * pause times belong to that day, not to today.
     */
    private fun editBaseDate(type: ButtonType): LocalDate {
        val today = LocalDate.now(clock)
        val state = _uiState.value
        val enter = state.enterTime
        if (type == ButtonType.Enter || enter == null || state.exitTime != null) return today
        return if (WorkTimeCalculator.isStale(enter, clock.millis(), clock.zone)) {
            Instant.ofEpochMilli(enter).atZone(clock.zone).toLocalDate()
        } else {
            today
        }
    }

    override fun dismissTimePicker() = update { it.copy(timePickerEvent = null) }

    // --- Pauses -----------------------------------------------------------------------------

    override fun addPause() {
        if (_uiState.value.enterTime == null) return showMessage(R.string.error_must_enter_before_pause)
        update { it.copy(pauses = it.pauses + PausePair()) }
        persistSession()
    }

    /** Starts a new pause right now. */
    override fun startNewPause() {
        val state = _uiState.value
        if (state.enterTime == null) return showMessage(R.string.error_must_enter_before_pause)
        if (state.exitTime != null) return
        update { it.copy(pauses = it.pauses + PausePair(start = clock.millis())) }
        persistSession()
    }

    override fun startPause(index: Int) {
        val state = _uiState.value
        if (state.enterTime == null) return showMessage(R.string.error_must_enter_first_exclamation)
        val pause = state.pauses.getOrNull(index) ?: return showMessage(R.string.error_pause_not_found_general)
        // Started but not ended: most likely a double tap.
        if (pause.start != null && pause.end == null) {
            return showMessage(R.string.error_pause_already_started, index + 1)
        }
        replacePause(index, PausePair(start = clock.millis(), end = null))
        persistSession()
    }

    override fun endPause(index: Int) {
        val state = _uiState.value
        if (state.enterTime == null) return showMessage(R.string.error_entry_not_recorded)
        val pause = state.pauses.getOrNull(index) ?: return showMessage(R.string.error_pause_not_found_general)
        val start = pause.start ?: return showMessage(R.string.error_must_start_pause_first, index + 1)
        val now = clock.millis()
        if (now < start) return showMessage(R.string.error_pause_end_before_start)
        replacePause(index, pause.copy(end = now))
        persistSession()
    }

    private fun editPauseStart(index: Int?, selected: Long) {
        if (index == null) return showMessage(R.string.error_pause_edit_failed)
        val state = _uiState.value
        val pause = state.pauses.getOrNull(index) ?: return showMessage(R.string.error_pause_index_invalid)
        val enter = state.enterTime
        val start = if (enter != null) WorkTimeCalculator.rollToNextDayIfBefore(selected, enter, clock.zone) else selected
        if (pause.end != null && start > pause.end) return showMessage(R.string.error_pause_start_after_end)

        replacePause(index, pause.copy(start = start))
        val text = TimeFormats.time(start)
        if (start != selected) {
            showMessage(R.string.pause_start_modified_next_day, index + 1, text)
        } else {
            showMessage(R.string.pause_start_modified, index + 1, text)
        }
        persistSession()
    }

    private fun editPauseEnd(index: Int?, selected: Long) {
        if (index == null) return showMessage(R.string.error_pause_edit_failed)
        val pause = _uiState.value.pauses.getOrNull(index) ?: return showMessage(R.string.error_pause_index_invalid)
        val pauseStart = pause.start ?: return showMessage(R.string.error_cannot_edit_pause_end_no_start)
        val end = WorkTimeCalculator.rollToNextDayIfBefore(selected, pauseStart, clock.zone)

        replacePause(index, pause.copy(end = end))
        val text = TimeFormats.time(end)
        if (end != selected) {
            showMessage(R.string.pause_end_modified_next_day, index + 1, text)
        } else {
            showMessage(R.string.pause_end_modified, index + 1, text)
        }
        persistSession()
    }

    private fun replacePause(index: Int, pause: PausePair) =
        update { state -> state.copy(pauses = state.pauses.mapIndexed { i, p -> if (i == index) pause else p }) }

    // --- Session lifecycle ------------------------------------------------------------------

    override fun discardSession() = resetAll()

    /** Clears the current day and cancels the reminder. History is untouched. */
    fun resetAll() {
        update { HomeUiState(dailyHours = it.dailyHours) }
        persistSession()
    }

    private fun finalizeSession(rawExitTime: Long) {
        val state = _uiState.value
        val enter = state.enterTime ?: return showMessage(R.string.error_must_enter_first)
        val exit = WorkTimeCalculator.rollToNextDayIfBefore(rawExitTime, enter, clock.zone)
        val worked = WorkTimeCalculator.workedMillis(enter, exit, state.pauses) ?: return
        val total = WorkTimeCalculator.formatDuration(worked)

        viewModelScope.launch {
            repository.addEntry(
                WorkHistoryEntry(
                    id = enter.toString(),
                    enterTime = enter,
                    pauses = state.pauses,
                    exitTime = exit,
                    totalWorkedTime = total,
                    dailyHoursTarget = state.dailyHours,
                ),
            )
            repository.saveActiveSession(null)
        }
        update { it.copy(exitTime = exit, totalWorkedTime = total) }
    }

    // --- Messages & alarm -------------------------------------------------------------------

    override fun messageShown() = update { it.copy(message = null) }

    override fun alarmPermissionHandled() = _uiState.update { it.copy(alarmPermissionMissing = false) }

    private fun showMessage(@StringRes resId: Int, vararg args: Any) =
        _uiState.update { it.copy(message = UiMessage(resId, args.toList())) }

    private fun showTimePicker(type: ButtonType, pauseIndex: Int?, initialMillis: Long?) {
        val time = Instant.ofEpochMilli(initialMillis ?: clock.millis()).atZone(clock.zone).toLocalTime()
        _uiState.update {
            it.copy(timePickerEvent = TimePickerEvent(type, pauseIndex, time.hour, time.minute))
        }
    }

    // --- State plumbing ---------------------------------------------------------------------

    /** Applies [change], recomputes derived values, and keeps the exit reminder in sync. */
    private fun update(change: (HomeUiState) -> HomeUiState) {
        _uiState.update { withDerivedValues(change(it)) }
        syncReminder()
    }

    private fun withDerivedValues(state: HomeUiState): HomeUiState {
        val expectedExit = state.enterTime
            ?.takeIf { state.exitTime == null }
            ?.let { WorkTimeCalculator.expectedExit(it, state.pauses, state.dailyHours) }
        return state.copy(
            calculatedExitTime = expectedExit,
            totalWorkedTime = state.totalWorkedTime.takeIf { state.enterTime != null },
        )
    }

    private fun syncReminder() {
        val target = _uiState.value.calculatedExitTime
        if (target == reminderFor) return
        reminderFor = target
        if (target == null) {
            reminder.cancel()
        } else if (!reminder.schedule(target)) {
            _uiState.update { it.copy(alarmPermissionMissing = true) }
        }
    }

    private fun persistSession() {
        val state = _uiState.value
        val session = state.enterTime
            ?.takeIf { state.exitTime == null }
            ?.let { ActiveSession(it, state.pauses) }
        viewModelScope.launch { repository.saveActiveSession(session) }
    }
}
