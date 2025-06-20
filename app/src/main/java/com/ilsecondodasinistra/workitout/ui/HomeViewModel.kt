package com.ilsecondodasinistra.workitout.ui

import IHomeViewModel
import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.viewModelScope
import com.ilsecondodasinistra.workitout.R
import com.ilsecondodasinistra.workitout.data.HistoryRepository
import com.ilsecondodasinistra.workitout.data.SerializablePausePair // Assuming this is @Serializable
import com.ilsecondodasinistra.workitout.data.WorkHistoryEntry
// REMOVED: import com.google.gson.Gson
// REMOVED: import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json // ADDED
import kotlinx.serialization.encodeToString // ADDED
import kotlinx.serialization.decodeFromString // ADDED
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

data class PausePair(
    val start: Date? = null,
    val end: Date? = null,
    val durationMinutes: Long? = null // ADDED for pause duration
)

data class HomeUiState(
    val enterTime: Date? = null,
    val pauses: List<PausePair> = emptyList(),
    val exitTime: Date? = null,
    val calculatedExitTime: Date? = null,
    val totalWorkedTime: String? = null,
    val dailyHours: Double = 8.0,
    val message: String = "",
    val timePickerEvent: TimePickerEvent? = null
)

data class TimePickerEvent(
    val type: ButtonType,
    val pauseIndex: Int? = null,
    val initialHour: Int,
    val initialMinute: Int
)

sealed class ButtonType(val textResId: Int) {
    object Enter : ButtonType(R.string.button_enter)
    object ToLunch : ButtonType(R.string.button_to_lunch)
    object FromLunch : ButtonType(R.string.button_from_lunch)
    object Exit : ButtonType(R.string.button_exit)
}

class HomeViewModel(application: Application) : AndroidViewModel(application), DefaultLifecycleObserver, IHomeViewModel {

    private val workHistoryRepository: HistoryRepository = com.ilsecondodasinistra.workitout.data.DataStoreHistoryRepository(application)
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val _uiState = MutableStateFlow(HomeUiState())
    override val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var notificationPollingJob: Job? = null

    init {
        viewModelScope.launch {
            loadCurrentSession()
            observeDailyHours()
        }
    }

    private fun calculatePauseDurationMinutes(pause: PausePair): Long? {
        return if (pause.start != null && pause.end != null && pause.end.after(pause.start)) {
            TimeUnit.MILLISECONDS.toMinutes(pause.end.time - pause.start.time)
        } else {
            null
        }
    }

    private suspend fun loadCurrentSession() {
        Log.d("HomeViewModel", "Attempting to load current session from DataStore.")
        val sessionData = workHistoryRepository.getCurrentSession().firstOrNull()
        if (sessionData != null) {
            val (enterTimeMillis, exitTimeMillis, currentPausesJson) = sessionData
            val restoredEnterTime = enterTimeMillis?.let { Date(it) }
            val restoredExitTime = exitTimeMillis?.let { Date(it) }

            val restoredPauses: List<PausePair> = currentPausesJson?.mapNotNull { jsonString ->
                try {
                    val serializablePause = json.decodeFromString<SerializablePausePair>(jsonString)
                    val loadedPause = PausePair(
                        start = serializablePause.start?.let { Date(it) },
                        end = serializablePause.end?.let { Date(it) }
                    )
                    // Calculate duration for the loaded pause
                    loadedPause.copy(durationMinutes = calculatePauseDurationMinutes(loadedPause))
                } catch (e: Exception) {
                    Log.e("HomeViewModel", "Error deserializing pause with kotlinx.serialization: $jsonString", e)
                    null
                }
            } ?: emptyList()

            _uiState.update {
                it.copy(
                    enterTime = restoredEnterTime,
                    exitTime = restoredExitTime,
                    pauses = restoredPauses,
                    message = if (restoredEnterTime != null) getApplication<Application>().getString(R.string.session_restored) else ""
                )
            }
            Log.d("HomeViewModel", "Session restored: Enter=${formatTime(restoredEnterTime)}, Exit=${formatTime(restoredExitTime)}, Pauses=${restoredPauses.size}")
            recalculateAndUpdateUi() // This will also handle calculatedExitTime
            if (restoredEnterTime != null && restoredExitTime == null) {
                 startOrUpdateNotificationPolling(_uiState.value.calculatedExitTime)
            }
        } else {
            Log.d("HomeViewModel", "No current session found in DataStore.")
             // Ensure UI is clean if no session is found, especially dailyHours should come from its observer
            _uiState.update { it.copy(enterTime = null, pauses = emptyList(), exitTime = null, totalWorkedTime = null, calculatedExitTime = null, message = "")}
        }
    }

    private suspend fun persistCurrentSessionState() {
        val current = _uiState.value
        // Only persist if the session is active (enter time set, but no exit time)
        if (current.enterTime != null && current.exitTime == null) {
            Log.d("HomeViewModel", "Persisting active session state to DataStore.")
            val enterTimeMillis = current.enterTime?.time
            // exitTimeMillis will be null for active sessions, which is correct
            val pausesJson: Set<String>? = current.pauses.mapNotNull { pause ->
                try {
                    json.encodeToString(SerializablePausePair(pause.start?.time, pause.end?.time))
                } catch (e: Exception) {
                    Log.e("HomeViewModel", "Error serializing pause with kotlinx.serialization: $pause", e)
                    null
                }
            }.toSet().ifEmpty { null }

            try {
                workHistoryRepository.saveCurrentSession(enterTimeMillis, null, pausesJson)
                Log.d("HomeViewModel", "Active session state persisted successfully.")
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error persisting active session state.", e)
            }
        } else {
            Log.d("HomeViewModel", "Session not active or already completed. No persistence to current session keys needed.")
        }
    }


    private fun observeDailyHours() {
        viewModelScope.launch {
            workHistoryRepository.getDailyHours().collect { loadedDailyHours ->
                if (_uiState.value.dailyHours != loadedDailyHours) {
                    _uiState.update {
                        it.copy(dailyHours = loadedDailyHours)
                    }
                    Log.d("HomeViewModel", "Daily hours updated from DataStore: $loadedDailyHours. Recalculating UI.")
                    recalculateAndUpdateUi()
                } else {
                    Log.d("HomeViewModel", "Daily hours from DataStore same as current: $loadedDailyHours.")
                }
            }
        }
    }

    private fun calculateExpectedExitTime(
        enterTime: Date?,
        pauses: List<PausePair>,
        dailyHours: Double
    ): Date? {
        if (enterTime != null && dailyHours > 0) {
            val enterMs = enterTime.time
            val totalWorkMilliseconds = (dailyHours * 60 * 60 * 1000).toLong()
            var totalPauseTime = 0L

            for (pause in pauses) {
                if (pause.start != null && pause.end != null && pause.end.after(pause.start)) {
                    totalPauseTime += pause.end.time - pause.start.time
                }
            }
            val newCalculatedExitMs = enterMs + totalWorkMilliseconds + totalPauseTime
            return Date(newCalculatedExitMs)
        }
        return null
    }

    private fun calculateTotalWorkedTime(
        enterTime: Date?,
        pauses: List<PausePair>,
        exitTime: Date?
    ): String? {
        if (enterTime != null && exitTime != null && exitTime.after(enterTime)) {
            var totalMs = exitTime.time - enterTime.time
            var totalPauseTime = 0L

            for (pause in pauses) {
                if (pause.start != null && pause.end != null && pause.end.after(pause.start)) {
                    totalPauseTime += pause.end.time - pause.start.time
                }
            }
            totalMs -= totalPauseTime
            if (totalMs < 0) totalMs = 0

            val totalHours = TimeUnit.MILLISECONDS.toHours(totalMs)
            val totalMinutes = TimeUnit.MILLISECONDS.toMinutes(totalMs) % 60
            return "${totalHours}h ${totalMinutes}m"
        }
        return null
    }

    private fun recalculateAndUpdateUi() {
        val current = _uiState.value
        val newCalculatedExitTime = if (current.exitTime == null && current.enterTime != null) {
            calculateExpectedExitTime(current.enterTime, current.pauses, current.dailyHours)
        } else {
            null // If session is completed (exitTime is set) or not started, no expected exit.
        }
        // Total worked time is only calculated when exitTime is set, which is handled by finalizeAndSaveWorkSession
        // So, no need to recalculate totalWorkedTime here unless it's to clear it if enterTime is null.
        val newTotalWorkedTime = if (current.enterTime == null) null else current.totalWorkedTime

        _uiState.update {
            it.copy(
                calculatedExitTime = newCalculatedExitTime,
                totalWorkedTime = newTotalWorkedTime // Retain if session ongoing, null if new
            )
        }
        if (current.enterTime != null && current.exitTime == null) {
            startOrUpdateNotificationPolling(newCalculatedExitTime)
        } else {
            notificationPollingJob?.cancel()
            Log.d("HomeViewModel", "Recalculate: Polling stopped (enter null or exit not null).")
        }
    }

    private fun startOrUpdateNotificationPolling(expectedExitTime: Date?) {
        notificationPollingJob?.cancel()
        if (expectedExitTime == null || _uiState.value.exitTime != null || _uiState.value.enterTime == null) {
            Log.d("HomeViewModel", "Notification polling stopped or not started. Expected: ${formatTime(expectedExitTime)}, Actual Exit: ${formatTime(_uiState.value.exitTime)}, Enter: ${formatTime(_uiState.value.enterTime)}")
            return
        }

        Log.d("HomeViewModel", "Starting notification polling for: ${formatTime(expectedExitTime)}")
        notificationPollingJob = viewModelScope.launch {
            var notified = false
            while (isActive && !notified && _uiState.value.exitTime == null) {
                if (System.currentTimeMillis() >= expectedExitTime.time) {
                    _uiState.update { it.copy(message = "NOTIFY_EXIT_TIME:${formatTime(expectedExitTime)}") }
                    notified = true
                    Log.d("HomeViewModel", "Exit time notification triggered for: ${formatTime(expectedExitTime)}")
                }
                delay(10000L)
            }
            if (notified || _uiState.value.exitTime != null) {
                Log.d("HomeViewModel", "Notification polling loop ended. Notified: $notified, ExitRecorded: ${_uiState.value.exitTime != null}")
            }
        }
    }

    // This function finalizes a work session, saves it, and updates UI but does NOT reset enter/pauses.
    private suspend fun finalizeAndSaveWorkSession(rawConfirmedExitTime: Date) {
        val current = _uiState.value
        if (current.enterTime == null) {
            _uiState.update { it.copy(message = getApplication<Application>().getString(R.string.error_must_enter_first)) }
            return
        }

        var adjustedExitTime = rawConfirmedExitTime
        if (current.enterTime.after(rawConfirmedExitTime)) { // If enter is after raw exit
            val calendar = Calendar.getInstance().apply { time = rawConfirmedExitTime }
            calendar.add(Calendar.DAY_OF_YEAR, 1)
            adjustedExitTime = calendar.time
            Log.d("HomeViewModel", "Exit time adjusted to next day: ${formatTime(adjustedExitTime)}")
        }

        val finalTotalWorked = calculateTotalWorkedTime(current.enterTime, current.pauses, adjustedExitTime)

        if (finalTotalWorked != null) {
            val entryToSave = WorkHistoryEntry(
                id = current.enterTime.time.toString(), // Consider a more robust ID, like UUID
                enterTime = current.enterTime.time,
                pauses = current.pauses.map { SerializablePausePair(it.start?.time, it.end?.time) },
                exitTime = adjustedExitTime.time,
                totalWorkedTime = finalTotalWorked,
                dailyHoursTarget = current.dailyHours
            )
            try {
                workHistoryRepository.addWorkHistoryEntry(entryToSave)
                Log.d("HomeViewModel", "Work history entry saved to DataStore. ID: ${entryToSave.id}")
                workHistoryRepository.saveCurrentSession(null, null, null) // Clear active session
                Log.d("HomeViewModel", "Active session cleared from DataStore after saving to history.")

                _uiState.update {
                    it.copy(
                        exitTime = adjustedExitTime,
                        totalWorkedTime = finalTotalWorked,
                        calculatedExitTime = null, // Session is now complete
                        message = getApplication<Application>().getString(R.string.session_saved_with_total, finalTotalWorked)
                    )
                }
                notificationPollingJob?.cancel()
                Log.d("HomeViewModel", "Polling stopped due to session finalization.")
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error saving work history entry or clearing session.", e)
                _uiState.update { it.copy(message = getApplication<Application>().getString(R.string.error_session_save_failed)) }
            }
        } else {
            _uiState.update { it.copy(message = getApplication<Application>().getString(R.string.error_exit_recorded_no_total)) }
        }
    }


    override fun handleTimeButtonPress(buttonType: ButtonType) {
        val currentTime = Date()
        val currentUiStateValue = _uiState.value // For checks before update

        when (buttonType) {
            ButtonType.Enter -> {
                _uiState.update {
                    it.copy(
                        enterTime = currentTime,
                        pauses = emptyList(), // Reset pauses for new session
                        exitTime = null,      // Reset exit time
                        totalWorkedTime = null,// Reset total worked time
                        calculatedExitTime = null, // Will be recalculated
                        message = getApplication<Application>().getString(R.string.new_entry_recorded, formatTime(currentTime))
                    )
                }
                Log.d("HomeViewModel", "Enter pressed. New shift started at ${formatTime(currentTime)}")
                viewModelScope.launch { persistCurrentSessionState() } // Persist the new active session
                recalculateAndUpdateUi() // Recalculate for the new session
            }
            ButtonType.Exit -> {
                if (currentUiStateValue.enterTime == null) {
                    _uiState.update { it.copy(message = getApplication<Application>().getString(R.string.error_must_enter_first)) }
                    return
                }
                // Use currentUiStateValue.exitTime if it was set by an edit, otherwise use currentTime
                val exitTimeToUse = currentUiStateValue.exitTime ?: currentTime
                viewModelScope.launch {
                    finalizeAndSaveWorkSession(exitTimeToUse)
                }
            }
            else -> { /* Other button types (pauses) handled by specific functions below */ }
        }
        // Note: recalculateAndUpdateUi() is called within Enter, and indirectly via finalizeAndSaveWorkSession effects for Exit.
    }


    override fun handleTimeEditRequest(buttonType: ButtonType) {
        val current = _uiState.value
        val calendar = Calendar.getInstance()
        val timeToEdit: Date? = when (buttonType) {
            ButtonType.Enter -> current.enterTime
            ButtonType.Exit -> current.exitTime ?: current.calculatedExitTime // Prefer actual exit, fallback to calculated
            else -> null // Should not happen for pause edits here
        }
        timeToEdit?.let { calendar.time = it }

        _uiState.update {
            it.copy(
                timePickerEvent = TimePickerEvent(
                    type = buttonType,
                    pauseIndex = null, // Not for direct enter/exit edit
                    initialHour = calendar.get(Calendar.HOUR_OF_DAY),
                    initialMinute = calendar.get(Calendar.MINUTE)
                )
            )
        }
    }

    override fun handlePauseEditStart(index: Int) {
        val currentPauses = _uiState.value.pauses
        if (index < 0 || index >= currentPauses.size) {
            _uiState.update { it.copy(message = getApplication<Application>().getString(R.string.error_pause_not_found)) }
            return
        }
        val pauseToEdit = currentPauses[index]
        val calendar = Calendar.getInstance()
        calendar.time = pauseToEdit.start ?: Date() // Default to now if start is null

        _uiState.update {
            it.copy(
                timePickerEvent = TimePickerEvent(
                    type = ButtonType.ToLunch, // Specific type for pause start
                    pauseIndex = index,
                    initialHour = calendar.get(Calendar.HOUR_OF_DAY),
                    initialMinute = calendar.get(Calendar.MINUTE)
                )
            )
        }
    }

    override fun handlePauseEditEnd(index: Int) {
        val currentPauses = _uiState.value.pauses
        if (index < 0 || index >= currentPauses.size) {
            _uiState.update { it.copy(message = getApplication<Application>().getString(R.string.error_pause_not_found)) }
            return
        }
        val pauseToEdit = currentPauses[index]
        val calendar = Calendar.getInstance()
        calendar.time = pauseToEdit.end ?: pauseToEdit.start ?: Date() // Default to start if end is null, then to now

        // Ensure initial time for picker is not before pause start if start exists
        if (pauseToEdit.start != null && calendar.time.before(pauseToEdit.start)) {
            calendar.time = pauseToEdit.start // Set to pause start time
        }

        _uiState.update {
            it.copy(
                timePickerEvent = TimePickerEvent(
                    type = ButtonType.FromLunch, // Specific type for pause end
                    pauseIndex = index,
                    initialHour = calendar.get(Calendar.HOUR_OF_DAY),
                    initialMinute = calendar.get(Calendar.MINUTE)
                )
            )
        }
    }

    override fun onTimeEdited(buttonType: ButtonType, hour: Int, minute: Int) {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val selectedDate = calendar.time
        var tempMessage = ""

        val currentPickerEvent = _uiState.value.timePickerEvent
        val pauseIndexToEdit = currentPickerEvent?.pauseIndex

        // Dismiss the dialog first
        _uiState.update { it.copy(timePickerEvent = null) }


        when (buttonType) {
            ButtonType.Enter -> {
                _uiState.update { current ->
                    current.copy(
                        enterTime = selectedDate,
                        message = getApplication<Application>().getString(R.string.enter_modified, formatTime(selectedDate))
                        // exitTime, pauses, etc., remain as they were, subject to recalculateAndUpdateUi
                    )
                }
                viewModelScope.launch { persistCurrentSessionState() }
            }
            ButtonType.Exit -> {
                val currentEnterTime = _uiState.value.enterTime
                if (currentEnterTime == null) {
                     _uiState.update { it.copy(message = getApplication<Application>().getString(R.string.error_must_enter_first_short)) }
                } else {
                    // Directly finalize and save if Exit time is edited
                    viewModelScope.launch {
                        finalizeAndSaveWorkSession(selectedDate)
                    }
                    // The message and UI update will be handled by finalizeAndSaveWorkSession
                }
            }
            ButtonType.ToLunch, ButtonType.FromLunch -> {
                if (pauseIndexToEdit != null) {
                    _uiState.update { current ->
                        var newPauses = current.pauses.toMutableList()
                        if (pauseIndexToEdit >= 0 && pauseIndexToEdit < newPauses.size) {
                            val originalPause = newPauses[pauseIndexToEdit]
                            if (buttonType == ButtonType.ToLunch) {
                                var adjustedPauseStartDate = selectedDate
                                if (current.enterTime != null && selectedDate.before(current.enterTime)) {
                                    val cal = Calendar.getInstance().apply { time = selectedDate }
                                    cal.add(Calendar.DAY_OF_YEAR, 1)
                                    adjustedPauseStartDate = cal.time
                                    Log.d("HomeViewModel", "Pause start time adjusted to next day: ${formatTime(adjustedPauseStartDate)}")
                                    tempMessage = getApplication<Application>().getString(R.string.pause_start_modified_next_day, pauseIndexToEdit + 1, formatTime(adjustedPauseStartDate))
                                } else {
                                     tempMessage = getApplication<Application>().getString(R.string.pause_start_modified, pauseIndexToEdit + 1, formatTime(adjustedPauseStartDate))
                                }

                                if (originalPause.end != null && adjustedPauseStartDate.after(originalPause.end)) {
                                    tempMessage = getApplication<Application>().getString(R.string.error_pause_start_after_end)
                                } else {
                                    val updatedPause = originalPause.copy(start = adjustedPauseStartDate)
                                    newPauses[pauseIndexToEdit] = updatedPause.copy(durationMinutes = calculatePauseDurationMinutes(updatedPause))
                                    viewModelScope.launch { persistCurrentSessionState() }
                                }
                            } else { // ButtonType.FromLunch
                                if (originalPause.start == null) {
                                    tempMessage = getApplication<Application>().getString(R.string.error_cannot_edit_pause_end_no_start)
                                } else {
                                    var adjustedPauseEndDate = selectedDate
                                    if (selectedDate.before(originalPause.start)) {
                                        val cal = Calendar.getInstance().apply { time = selectedDate }
                                        cal.add(Calendar.DAY_OF_YEAR, 1)
                                        adjustedPauseEndDate = cal.time
                                        Log.d("HomeViewModel", "Pause end time adjusted to next day: ${formatTime(adjustedPauseEndDate)}")
                                        tempMessage = getApplication<Application>().getString(R.string.pause_end_modified_next_day, pauseIndexToEdit + 1, formatTime(adjustedPauseEndDate))
                                    } else {
                                         tempMessage = getApplication<Application>().getString(R.string.pause_end_modified, pauseIndexToEdit + 1, formatTime(adjustedPauseEndDate))
                                    }
                                    val updatedPause = originalPause.copy(end = adjustedPauseEndDate)
                                    newPauses[pauseIndexToEdit] = updatedPause.copy(durationMinutes = calculatePauseDurationMinutes(updatedPause))
                                    viewModelScope.launch { persistCurrentSessionState() }
                                }
                            }
                        } else {
                            tempMessage = getApplication<Application>().getString(R.string.error_pause_index_invalid)
                        }
                        current.copy(pauses = newPauses.toList(), message = tempMessage)
                    }
                } else {
                     _uiState.update { it.copy(message = getApplication<Application>().getString(R.string.error_pause_edit_failed))}
                }
            }
        }
        // Recalculate UI based on changes, especially for Enter and Pauses.
        // For Exit, finalizeAndSaveWorkSession handles the final UI state.
        if (buttonType != ButtonType.Exit) {
            recalculateAndUpdateUi()
        }
    }

    override fun onDialogDismissed() {
        // Clear message only if it's not a persistent error/validation message
        // or a notification trigger.
        val currentMessage = _uiState.value.message
        val errorPrefix = getApplication<Application>().getString(R.string.error_prefix)
        val sessionSaved = getApplication<Application>().getString(R.string.session_saved)
        if (currentMessage.isNotEmpty() &&
            !currentMessage.startsWith("NOTIFY_") &&
            !currentMessage.startsWith(errorPrefix) &&
            !currentMessage.startsWith(getApplication<Application>().getString(R.string.error_pause_start_after_end)) &&
            !currentMessage.startsWith(getApplication<Application>().getString(R.string.error_cannot_edit_pause_end_no_start)) &&
            !currentMessage.contains(sessionSaved) // Don't clear success messages immediately
        ) {
            _uiState.update { it.copy(message = "") }
        }
        // Always ensure the picker event is cleared on dismiss
        _uiState.update { it.copy(timePickerEvent = null) }
    }

    override fun clearMessage() {
        val currentMessage = _uiState.value.message
        val errorPrefix = getApplication<Application>().getString(R.string.error_prefix)
        val sessionSaved = getApplication<Application>().getString(R.string.session_saved)
        if (currentMessage.isNotEmpty() &&
            !currentMessage.startsWith("NOTIFY_") &&
            !currentMessage.startsWith(errorPrefix) &&
            !currentMessage.startsWith(getApplication<Application>().getString(R.string.error_pause_start_after_end)) &&
            !currentMessage.startsWith(getApplication<Application>().getString(R.string.error_cannot_edit_pause_end_no_start)) &&
            !currentMessage.contains(sessionSaved)
        ) {
            _uiState.update { it.copy(message = "") }
        }
    }


    override fun formatTimeToDisplay(date: Date?): String {
        return date?.let { SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(it) } ?: "N/A"
    }

    private fun formatTime(date: Date?): String {
        return date?.let { SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(it) } ?: "N/A"
    }

    override fun handleAddPause() {
        _uiState.update { current ->
            if (current.enterTime == null) {
                return@update current.copy(message = getApplication<Application>().getString(R.string.error_must_enter_before_pause))
            }
            // Allow adding pauses even if exitTime is set, as 
            // the session might be reviewed before a new "Enter" clears it.
            val newPauses = current.pauses.toMutableList().apply { add(PausePair()) } // New pause has null duration
            current.copy(pauses = newPauses, message = getApplication<Application>().getString(R.string.new_pause_added))
        }
        // No need to persist here as only an empty pause is added.
        // Persist will happen when start/end of this new pause is set.
        recalculateAndUpdateUi() // Update calculated exit time if session is active
    }

    override fun handlePauseStart(index: Int) {
        val currentTime = Date()
        _uiState.update { current ->
            if (current.enterTime == null) {
                return@update current.copy(message = getApplication<Application>().getString(R.string.error_must_enter_first_exclamation))
            }
            if (index < 0 || index >= current.pauses.size) {
                return@update current.copy(message = getApplication<Application>().getString(R.string.error_pause_not_found_general))
            }

            val newPauses = current.pauses.toMutableList()
            val currentPause = newPauses[index]

            if (currentPause.start != null && currentPause.end == null) {
                // If pause already started and not ended, this might be a misclick.
                // Or, if you want to allow re-starting a pause, clear the end time.
                // For now, let's assume it's a misclick if start is set and end is not.
                return@update current.copy(message = getApplication<Application>().getString(R.string.error_pause_already_started, index + 1))
            }
            // If starting a new pause or restarting a completed one
            val updatedPause = currentPause.copy(start = currentTime, end = null, durationMinutes = null)
            newPauses[index] = updatedPause
            current.copy(pauses = newPauses, message = getApplication<Application>().getString(R.string.pause_start_recorded, index + 1, formatTime(currentTime)))
        }
        viewModelScope.launch { persistCurrentSessionState() }
        recalculateAndUpdateUi()
    }

    override fun handlePauseEnd(index: Int) {
        val currentTime = Date()
        _uiState.update { current ->
             if (current.enterTime == null) { // Should not happen if pause button is enabled correctly
                return@update current.copy(message = getApplication<Application>().getString(R.string.error_entry_not_recorded))
            }
            if (index < 0 || index >= current.pauses.size) {
                return@update current.copy(message = getApplication<Application>().getString(R.string.error_pause_not_found_general))
            }

            val newPauses = current.pauses.toMutableList()
            val currentPause = newPauses[index]

            if (currentPause.start == null) {
                return@update current.copy(message = getApplication<Application>().getString(R.string.error_must_start_pause_first, index + 1))
            }
            if (currentTime.before(currentPause.start)) {
                // This check is for direct button press, next-day logic is for edits.
                return@update current.copy(message = getApplication<Application>().getString(R.string.error_pause_end_before_start))
            }
            val updatedPause = currentPause.copy(end = currentTime)
            newPauses[index] = updatedPause.copy(durationMinutes = calculatePauseDurationMinutes(updatedPause))
            current.copy(pauses = newPauses, message = getApplication<Application>().getString(R.string.pause_end_recorded, index + 1, formatTime(currentTime), newPauses[index].durationMinutes ?: 0))
        }
        viewModelScope.launch { persistCurrentSessionState() }
        recalculateAndUpdateUi()
    }

    @androidx.annotation.CallSuper
    override fun onCleared() {
        super.onCleared()
        notificationPollingJob?.cancel()
        Log.d("HomeViewModel", "onCleared called, notification polling job cancelled.")
        // Persist one last time if the session was active and not completed
        val current = _uiState.value
        if (current.enterTime != null && current.exitTime == null) {
            Log.d("HomeViewModel", "Attempting to persist final session state onCleared.")
            viewModelScope.launch { persistCurrentSessionState() } // This will only save if active
        }
    }
}
