package com.ilsecondodasinistra.workitout.ui

import IHomeViewModel
import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.viewModelScope
import com.ilsecondodasinistra.workitout.data.DataStoreHistoryRepository
import com.ilsecondodasinistra.workitout.data.WorkHistoryEntry
import com.ilsecondodasinistra.workitout.data.HistoryRepository
// Import your WorkHistoryRepository and WorkHistoryEntry
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.concurrent.TimeUnit

// Removed SharedPreferences constants

data class HomeUiState(
    val enterTime: Date? = null,
    val toLunchTime: Date? = null,
    val fromLunchTime: Date? = null,
    val exitTime: Date? = null, // This will be set only when Exit is pressed
    val calculatedExitTime: Date? = null,
    val totalWorkedTime: String? = null,
    val dailyHours: Double = 8.0, // Default, will be loaded from DataStore
    val message: String = "",
    val timePickerEvent: TimePickerEvent? = null
)

data class TimePickerEvent(
    val type: ButtonType,
    val initialHour: Int,
    val initialMinute: Int
)

sealed class ButtonType(val text: String) {
    object Enter : ButtonType("Ingresso")
    object ToLunch : ButtonType("In Pausa")
    object FromLunch : ButtonType("Fine Pausa")
    object Exit : ButtonType("Uscita")
}

class HomeViewModel(application: Application) : AndroidViewModel(application), DefaultLifecycleObserver, IHomeViewModel {

    // Use WorkHistoryRepository for DataStore access
    private val workHistoryRepository = DataStoreHistoryRepository(application)

    private val _uiState = MutableStateFlow(HomeUiState())
    override val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var notificationPollingJob: Job? = null

    init {
        loadDailyHoursFromDataStore()
        // No longer loading a "current day progress" from SharedPreferences.
        // The UI starts fresh for timings.
    }

    override fun onResume(owner: LifecycleOwner) {
        super.onResume(owner)
        Log.d("HomeViewModel", "onResume called, reloading daily hours if necessary.")
        loadDailyHoursFromDataStore() // Ensure daily hours are up-to-date
    }

    private fun loadDailyHoursFromDataStore() {
        viewModelScope.launch {
            val loadedDailyHours = workHistoryRepository.getDailyHours().first() // Assuming getDailyHours returns Flow<Double>
            if (_uiState.value.dailyHours != loadedDailyHours) {
                _uiState.update {
                    it.copy(dailyHours = loadedDailyHours)
                }
                recalculateAndUpdateUi() // Recalculate if daily hours changed
                Log.d("HomeViewModel", "Daily hours loaded from DataStore: $loadedDailyHours")
            } else {
                Log.d("HomeViewModel", "Daily hours unchanged from DataStore: $loadedDailyHours")
            }
        }
    }

    // Removed loadInitialData, saveCurrentDayProgress, clearCurrentDayProgressFromPrefs

    private fun calculateExpectedExitTime(
        enterTime: Date?,
        toLunchTime: Date?,
        fromLunchTime: Date?,
        dailyHours: Double
    ): Date? {
        if (enterTime != null && dailyHours > 0) {
            val enterMs = enterTime.time
            val totalWorkMilliseconds = (dailyHours * 60 * 60 * 1000).toLong()
            var lunchBreakMilliseconds = 0L

            if (toLunchTime != null && fromLunchTime != null && fromLunchTime.after(toLunchTime)) {
                lunchBreakMilliseconds = fromLunchTime.time - toLunchTime.time
            }
            val newCalculatedExitMs = enterMs + totalWorkMilliseconds + lunchBreakMilliseconds
            return Date(newCalculatedExitMs)
        }
        return null
    }

    private fun calculateTotalWorkedTime(
        enterTime: Date?,
        toLunchTime: Date?,
        fromLunchTime: Date?,
        exitTime: Date?
    ): String? {
        if (enterTime != null && exitTime != null && exitTime.after(enterTime)) {
            var totalMs = exitTime.time - enterTime.time
            var lunchMs = 0L

            if (toLunchTime != null && fromLunchTime != null && fromLunchTime.after(toLunchTime)) {
                lunchMs = fromLunchTime.time - toLunchTime.time
            }
            totalMs -= lunchMs
            if (totalMs < 0) totalMs = 0

            val totalHours = TimeUnit.MILLISECONDS.toHours(totalMs)
            val totalMinutes = TimeUnit.MILLISECONDS.toMinutes(totalMs) % 60
            return "${totalHours}h ${totalMinutes}m"
        }
        return null
    }

    private fun recalculateAndUpdateUi() {
        val current = _uiState.value
        // Only calculate expected exit time if no actual exit time is set for the current shift
        val newCalculatedExitTime = if (current.exitTime == null && current.enterTime != null) {
            calculateExpectedExitTime(current.enterTime, current.toLunchTime, current.fromLunchTime, current.dailyHours)
        } else {
            null // If exitTime is set, or no enterTime, no need for calculated one.
        }
        // Total worked time is calculated if an exit time is present for the current shift
        val newTotalWorkedTime = calculateTotalWorkedTime(current.enterTime, current.toLunchTime, current.fromLunchTime, current.exitTime)

        _uiState.update {
            it.copy(
                calculatedExitTime = newCalculatedExitTime,
                totalWorkedTime = newTotalWorkedTime
            )
        }
        // No longer calling saveCurrentDayProgress() here
        startOrUpdateNotificationPolling(newCalculatedExitTime)
    }


    private fun startOrUpdateNotificationPolling(expectedExitTime: Date?) {
        notificationPollingJob?.cancel()
        // Only start polling if there's an expected exit time and no actual exit time has been recorded yet for the current shift
        if (expectedExitTime == null || _uiState.value.exitTime != null) {
            Log.d("HomeViewModel", "Notification polling stopped or not started. Expected: $expectedExitTime, Actual Exit: ${_uiState.value.exitTime}")
            return
        }

        Log.d("HomeViewModel", "Starting notification polling for: ${formatTime(expectedExitTime)}")
        notificationPollingJob = viewModelScope.launch {
            var notified = false
            while (isActive && !notified && _uiState.value.exitTime == null) { // Also check if an exit time has been recorded
                if (System.currentTimeMillis() >= expectedExitTime.time) {
                    _uiState.update { it.copy(message = "NOTIFY_EXIT_TIME:${formatTime(expectedExitTime)}") }
                    notified = true
                    Log.d("HomeViewModel", "Exit time notification triggered for: ${formatTime(expectedExitTime)}")
                }
                delay(10000L) // Check every 10 seconds
            }
            if (notified || _uiState.value.exitTime != null) {
                Log.d("HomeViewModel", "Notification polling loop ended. Notified: $notified, ExitRecorded: ${_uiState.value.exitTime != null}")
            }
        }
    }

    override fun handleTimeButtonPress(buttonType: ButtonType) {
        val currentTime = Date()
        var currentUiState = _uiState.value
        var newEnterTime = currentUiState.enterTime
        var newToLunchTime = currentUiState.toLunchTime
        var newFromLunchTime = currentUiState.fromLunchTime
        var newExitTime = currentUiState.exitTime // Should be null unless an Exit was just pressed
        var tempMessage = ""

        when (buttonType) {
            ButtonType.Enter -> {
                // Always starts a new shift. Clear previous partial shift data from UI state.
                newEnterTime = currentTime
                newToLunchTime = null
                newFromLunchTime = null
                newExitTime = null // Explicitly null for a new shift's start
                tempMessage = "Nuovo Ingresso registrato: ${formatTime(newEnterTime)}"
                Log.d("HomeViewModel", "Enter pressed. New shift started at ${formatTime(newEnterTime)}")
            }
            ButtonType.ToLunch -> {
                if (newEnterTime == null) {
                    tempMessage = "Devi prima registrare l'ingresso!"
                } else if (newToLunchTime != null && newFromLunchTime == null) {
                    tempMessage = "Sei già in pausa. Registra il rientro."
                } else {
                    newToLunchTime = currentTime
                    tempMessage = "Inizio pausa: ${formatTime(newToLunchTime)}"
                }
            }
            ButtonType.FromLunch -> {
                if (newToLunchTime == null) {
                    tempMessage = "Devi prima registrare l'inizio della pausa!"
                } else if (currentTime.before(newToLunchTime)) {
                    tempMessage = "Il rientro non può essere prima dell'inizio pausa!"
                } else {
                    newFromLunchTime = currentTime
                    tempMessage = "Fine pausa: ${formatTime(newFromLunchTime)}"
                }
            }
            ButtonType.Exit -> {
                if (newEnterTime == null) {
                    tempMessage = "Devi prima registrare l'ingresso per questa sessione!"
                } else {
                    newExitTime = currentTime // Set current exit time for this shift
                    val finalTotalWorked = calculateTotalWorkedTime(newEnterTime, newToLunchTime, newFromLunchTime, newExitTime)
                    tempMessage = "Uscita registrata: ${formatTime(newExitTime)}. Totale: $finalTotalWorked"
                    Log.d("HomeViewModel", "Exit pressed at ${formatTime(newExitTime)}")

                    if (finalTotalWorked != null) {
                        val entryToSave = WorkHistoryEntry(
                            id = newEnterTime.time.toString(), // Using enter time as ID, ensure WorkHistoryEntry expects String
                            enterTime = newEnterTime.time,
                            toLunchTime = newToLunchTime?.time,
                            fromLunchTime = newFromLunchTime?.time,
                            exitTime = newExitTime.time, // Non-null because we just set it
                            totalWorkedTime = finalTotalWorked,
                            dailyHoursTarget = _uiState.value.dailyHours // Save the daily hours setting at the time of completion
                        )
                        viewModelScope.launch {
                            workHistoryRepository.addWorkHistoryEntry(entryToSave)
                            Log.d("HomeViewModel", "Work history entry saved to DataStore. ID: ${entryToSave.id}")
                            // Reset UI state for the next shift after successful save
                            _uiState.update {
                                it.copy(
                                    // Keep the times from the shift that was just saved
                                    enterTime = newEnterTime, // This is currentUiState.enterTime or the edited enterTime
                                    toLunchTime = newToLunchTime, // This is currentUiState.toLunchTime or the edited toLunchTime
                                    fromLunchTime = newFromLunchTime, // This is currentUiState.fromLunchTime or the edited fromLunchTime
                                    exitTime = newExitTime, // This is the currentTime when Exit was pressed or the edited exitTime
                                    totalWorkedTime = finalTotalWorked, // This is the calculated total for the completed shift
                                    calculatedExitTime = null, // No longer need a calculated exit time as the shift is done
                                    message = tempMessage // Keep the confirmation message
                                )
                            }
                            // No need to call recalculateAndUpdateUi() immediately after reset,
                            // as all relevant values are nulled.
                            // Notification polling will be stopped by recalculateAndUpdateUi if current.exitTime is null
                            // or if current.enterTime is null.
                            // We need to explicitly call it here to ensure polling stops if it was running.
                             recalculateAndUpdateUi()


                        }
                    } else {
                        tempMessage = "Uscita registrata, ma impossibile calcolare il totale lavorato."
                        // Do not clear newExitTime here, let it be saved as is for transparency if calculation fails.
                        // Or revert to previous newExitTime state (currentUiState.exitTime) if preferred.
                        // For now, we will update the UI with the exit time, even if total calculation failed
                         _uiState.update {
                            it.copy(
                                enterTime = newEnterTime, // Keep enter time
                                toLunchTime = newToLunchTime, // Keep toLunch
                                fromLunchTime = newFromLunchTime, // Keep fromLunch
                                exitTime = newExitTime, // Show the exit time that was problematic
                                message = tempMessage
                                // totalWorkedTime will be null, calculatedExitTime will be null
                            )
                        }
                        recalculateAndUpdateUi() // Update calculations (which will likely be null)
                    }
                    // The _uiState.update for successful save is now inside the coroutine.
                    // If save fails or total is null, we only update the message and potentially the exit time.
                    return // Return to avoid the general _uiState.update below for Exit case
                }
            }
        }

        _uiState.update {
            it.copy(
                enterTime = newEnterTime,
                toLunchTime = newToLunchTime,
                fromLunchTime = newFromLunchTime,
                // exitTime is only set definitively on Exit press and handled above for saving.
                // For other button presses, if an exit time was somehow present, it should be cleared
                // or managed based on whether a new shift is starting.
                // With Enter now always clearing exitTime, this should be fine.
                exitTime = if (buttonType == ButtonType.Enter) null else it.exitTime,
                message = tempMessage
            )
        }
        recalculateAndUpdateUi()
    }


    override fun handleTimeEditRequest(buttonType: ButtonType) {
        val current = _uiState.value
        val calendar = Calendar.getInstance()
        val timeToEdit: Date? = when (buttonType) {
            ButtonType.Enter -> current.enterTime
            ButtonType.ToLunch -> current.toLunchTime
            ButtonType.FromLunch -> current.fromLunchTime
            ButtonType.Exit -> current.exitTime // User can edit the exit time before confirming the actual "Exit"
        }
        timeToEdit?.let { calendar.time = it }

        _uiState.update {
            it.copy(
                timePickerEvent = TimePickerEvent(
                    type = buttonType,
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

        _uiState.update { current ->
            var newEnterTime = current.enterTime
            var newToLunchTime = current.toLunchTime
            var newFromLunchTime = current.fromLunchTime
            var newExitTime = current.exitTime // This is the value that might be edited

            when (buttonType) {
                ButtonType.Enter -> {
                    newEnterTime = selectedDate
                    tempMessage = "Ingresso modificato: ${formatTime(newEnterTime)}"
                }
                ButtonType.ToLunch -> {
                    if (newEnterTime != null && selectedDate.before(newEnterTime)) {
                        tempMessage = "L'inizio pausa non può essere prima dell'ingresso."
                    } else {
                        newToLunchTime = selectedDate
                        tempMessage = "Inizio pausa modificato: ${formatTime(newToLunchTime)}"
                    }
                }
                ButtonType.FromLunch -> {
                    if (newToLunchTime != null && selectedDate.before(newToLunchTime)) {
                        tempMessage = "Il rientro non può essere prima dell'inizio pausa."
                    } else {
                        newFromLunchTime = selectedDate
                        tempMessage = "Fine pausa modificata: ${formatTime(newFromLunchTime)}"
                    }
                }
                ButtonType.Exit -> { // Editing the proposed/actual exit time
                     if (newEnterTime != null && selectedDate.before(newEnterTime)) {
                        tempMessage = "L'orario di uscita non può essere prima dell'ingresso."
                    } else {
                        newExitTime = selectedDate // Update the UI state's exitTime
                        tempMessage = "Orario di uscita modificato: ${formatTime(newExitTime)}. Premi 'Uscita' per salvare."
                    }
                }
            }
            current.copy(
                enterTime = newEnterTime,
                toLunchTime = newToLunchTime,
                fromLunchTime = newFromLunchTime,
                exitTime = newExitTime, // Apply the edited time to UI state for all cases
                message = tempMessage,
                timePickerEvent = null
            )
        }
        recalculateAndUpdateUi() // Recalculate based on potentially new times
    }


    override fun onDialogDismissed() {
        _uiState.update { it.copy(timePickerEvent = null) }
    }

    override fun clearMessage() {
        if (_uiState.value.message.isNotEmpty() && !_uiState.value.message.startsWith("NOTIFY_")) {
            _uiState.update { it.copy(message = "") }
        }
    }

    override fun formatTimeToDisplay(date: Date?): String {
        return date?.let { SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(it) } ?: "N/A"
    }

    private fun formatTime(date: Date?): String { // Internal utility
        return date?.let { SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(it) } ?: "N/A"
    }

    override fun onCleared() {
        super.onCleared()
        notificationPollingJob?.cancel()
        Log.d("HomeViewModel", "onCleared called, notification polling job cancelled.")
    }
}
