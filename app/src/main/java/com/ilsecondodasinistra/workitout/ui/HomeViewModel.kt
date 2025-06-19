package com.ilsecondodasinistra.workitout.ui

import IHomeViewModel
import android.app.Application // Or inject Context if not using AndroidViewModel
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.AndroidViewModel // Use AndroidViewModel if you need Application context directly
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.viewModelScope
import com.ilsecondodasinistra.workitout.NOTIFICATION_CHANNEL_ID // Assuming this is accessible
import com.ilsecondodasinistra.workitout.NOTIFICATION_ID     // Assuming this is accessible
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

// Constants for SharedPreferences (ensure these are consistent)
private const val PREFS_NAME = "workitout_settings_prefs"
private const val KEY_DAILY_HOURS = "daily_hours"
private const val KEY_CURRENT_DAY_PREFIX = "current_day_"
private const val KEY_ENTER_TIME = "enter_time"
private const val KEY_TO_LUNCH_TIME = "to_lunch_time"
private const val KEY_FROM_LUNCH_TIME = "from_lunch_time"

data class HomeUiState(
    val enterTime: Date? = null,
    val toLunchTime: Date? = null,
    val fromLunchTime: Date? = null,
    val exitTime: Date? = null, // Only set momentarily before saving to history
    val calculatedExitTime: Date? = null,
    val totalWorkedTime: String? = null,
    val dailyHours: Double = 8.0,
    val message: String = "",
    val timePickerEvent: TimePickerEvent? = null // For showing TimePickerDialog
)

data class TimePickerEvent(
    val type: ButtonType,
    val initialHour: Int,
    val initialMinute: Int
)

// ButtonType sealed class (can be here or in a common file)
sealed class ButtonType(val text: String) {
    object Enter : ButtonType("Ingresso")
    object ToLunch : ButtonType("In Pausa")
    object FromLunch : ButtonType("Fine Pausa")
    object Exit : ButtonType("Uscita")
}

class HomeViewModel(application: Application) : AndroidViewModel(application), DefaultLifecycleObserver, IHomeViewModel { // Implement DefaultLifecycleObserver

    private val sharedPreferences: SharedPreferences =
        application.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(HomeUiState())
    override val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var notificationPollingJob: Job? = null

    init {
        loadInitialData() // Loads current day progress and initial daily hours
    }

    // This method will be called when HomeScreen resumes
    override fun onResume(owner: LifecycleOwner) {
        super.onResume(owner)
        Log.d("HomeViewModel", "onResume called, reloading daily hours.")
        loadDailyHoursFromPrefs()
    }

    private fun loadDailyHoursFromPrefs() {
        viewModelScope.launch { // Ensure it runs in the ViewModel's scope
            val loadedDailyHours = sharedPreferences.getFloat(KEY_DAILY_HOURS, 8.0f).toDouble()
            if (_uiState.value.dailyHours != loadedDailyHours) {
                _uiState.update {
                    it.copy(dailyHours = loadedDailyHours)
                }
                // Important: After updating dailyHours, recalculate dependent values
                recalculateAndUpdateUi()
                Log.d("HomeViewModel", "Daily hours reloaded and UI updated: $loadedDailyHours")
            } else {
                Log.d("HomeViewModel", "Daily hours unchanged: $loadedDailyHours")
            }
        }
    }


    private fun loadInitialData() {
        viewModelScope.launch {
            // Load current day progress (enterTime, etc.) as before
            val savedEnterTimeMs = sharedPreferences.getLong("${KEY_CURRENT_DAY_PREFIX}$KEY_ENTER_TIME", -1L)
            val savedToLunchTimeMs = sharedPreferences.getLong("${KEY_CURRENT_DAY_PREFIX}$KEY_TO_LUNCH_TIME", -1L)
            val savedFromLunchTimeMs = sharedPreferences.getLong("${KEY_CURRENT_DAY_PREFIX}$KEY_FROM_LUNCH_TIME", -1L)

            val initialEnterTime = if (savedEnterTimeMs != -1L) Date(savedEnterTimeMs) else null
            val initialToLunchTime = if (savedToLunchTimeMs != -1L) Date(savedToLunchTimeMs) else null
            val initialFromLunchTime = if (savedFromLunchTimeMs != -1L) Date(savedFromLunchTimeMs) else null

            // Load daily hours initially
            val loadedDailyHours = sharedPreferences.getFloat(KEY_DAILY_HOURS, 8.0f).toDouble()

            _uiState.update {
                it.copy(
                    dailyHours = loadedDailyHours, // Set initial daily hours
                    enterTime = initialEnterTime,
                    toLunchTime = initialToLunchTime,
                    fromLunchTime = initialFromLunchTime,
                    message = if (initialEnterTime != null) "Giorno lavorativo ripreso." else ""
                )
            }
            recalculateAndUpdateUi() // Calculate based on initial values
            Log.d("HomeViewModel", "Initial data loaded. Daily Hours: $loadedDailyHours")
        }
    }

    private fun saveCurrentDayProgress() {
        val currentState = _uiState.value
        with(sharedPreferences.edit()) {
            currentState.enterTime?.let { putLong("${KEY_CURRENT_DAY_PREFIX}$KEY_ENTER_TIME", it.time) }
                ?: remove("${KEY_CURRENT_DAY_PREFIX}$KEY_ENTER_TIME")
            currentState.toLunchTime?.let { putLong("${KEY_CURRENT_DAY_PREFIX}$KEY_TO_LUNCH_TIME", it.time) }
                ?: remove("${KEY_CURRENT_DAY_PREFIX}$KEY_TO_LUNCH_TIME")
            currentState.fromLunchTime?.let { putLong("${KEY_CURRENT_DAY_PREFIX}$KEY_FROM_LUNCH_TIME", it.time) }
                ?: remove("${KEY_CURRENT_DAY_PREFIX}$KEY_FROM_LUNCH_TIME")
            apply()
        }
        Log.d("HomeViewModel", "Current day progress saved.")
    }

    private fun clearCurrentDayProgressFromPrefs() {
        with(sharedPreferences.edit()) {
            remove("${KEY_CURRENT_DAY_PREFIX}$KEY_ENTER_TIME")
            remove("${KEY_CURRENT_DAY_PREFIX}$KEY_TO_LUNCH_TIME")
            remove("${KEY_CURRENT_DAY_PREFIX}$KEY_FROM_LUNCH_TIME")
            apply()
        }
        Log.d("HomeViewModel", "Current day progress cleared from prefs.")
    }

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
        val newCalculatedExitTime = calculateExpectedExitTime(current.enterTime, current.toLunchTime, current.fromLunchTime, current.dailyHours)
        val newTotalWorkedTime = if (current.exitTime != null) { // Only calculate if exitTime is set
            calculateTotalWorkedTime(current.enterTime, current.toLunchTime, current.fromLunchTime, current.exitTime)
        } else {
            null
        }

        _uiState.update {
            it.copy(
                calculatedExitTime = newCalculatedExitTime,
                totalWorkedTime = newTotalWorkedTime
            )
        }
        // Save progress if the day hasn't ended
        if (current.exitTime == null &&
            (current.enterTime != null || current.toLunchTime != null || current.fromLunchTime != null) // Only save if there's progress
        ) {
            saveCurrentDayProgress()
        }
        startOrUpdateNotificationPolling(newCalculatedExitTime)
    }

    private fun startOrUpdateNotificationPolling(expectedExitTime: Date?) {
        notificationPollingJob?.cancel() // Cancel previous job
        if (expectedExitTime == null) return

        notificationPollingJob = viewModelScope.launch {
            var notified = false
            while (isActive && !notified) {
                if (System.currentTimeMillis() >= expectedExitTime.time) {
                    _uiState.update { it.copy(message = "NOTIFY_EXIT_TIME:${formatTime(expectedExitTime)}") } // Special message for UI to trigger notification
                    notified = true
                }
                delay(10000L) // Check every 10 seconds
            }
        }
    }


    override fun handleTimeButtonPress(buttonType: ButtonType) {
        val currentTime = Date()
        var newEnterTime = _uiState.value.enterTime
        var newToLunchTime = _uiState.value.toLunchTime
        var newFromLunchTime = _uiState.value.fromLunchTime
        var newExitTime: Date? = null // Reset for exit logic
        var tempMessage = ""

        when (buttonType) {
            ButtonType.Enter -> {
                newEnterTime = currentTime
                newToLunchTime = null
                newFromLunchTime = null
                // newExitTime remains null
                tempMessage = "Ingresso registrato: ${formatTime(newEnterTime)}"
            }
            ButtonType.ToLunch -> {
                if (newEnterTime == null) {
                    tempMessage = "Devi prima registrare l'ingresso!"
                } else if (newToLunchTime != null && newFromLunchTime == null) {
                    tempMessage = "Sei già in pausa. Registra il rientro."
                }
                else {
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
                    tempMessage = "Devi prima registrare l'ingresso!"
                } else {
                    newExitTime = currentTime // Set exit time
                    val finalTotalWorked = calculateTotalWorkedTime(newEnterTime, newToLunchTime, newFromLunchTime, newExitTime)
                    tempMessage = "Uscita registrata: ${formatTime(newExitTime)}. Totale: $finalTotalWorked"

                    if (finalTotalWorked != null) {
                        val record = mapOf(
                            "enterTime" to newEnterTime?.time,
                            "toLunchTime" to newToLunchTime?.time,
                            "fromLunchTime" to newFromLunchTime?.time,
                            "exitTime" to newExitTime?.time,
                            "totalWorkedTime" to finalTotalWorked,
                            "dailyHours" to _uiState.value.dailyHours
                        )
                        LocalHistoryRepository.addRecord(record)
                        Log.d("HomeViewModel", "Day record saved to LocalHistoryRepository.")

                        // Reset state for next day
                        newEnterTime = null
                        newToLunchTime = null
                        newFromLunchTime = null
                        // newExitTime will be cleared by the update
                        clearCurrentDayProgressFromPrefs()
                    } else {
                        tempMessage = "Uscita registrata, ma impossibile calcolare il totale lavorato."
                        newExitTime = null // Don't persist a problematic exit
                    }
                }
            }
        }

        _uiState.update {
            it.copy(
                enterTime = newEnterTime,
                toLunchTime = newToLunchTime,
                fromLunchTime = newFromLunchTime,
                exitTime = if (buttonType == ButtonType.Exit && newExitTime != null) newExitTime else null, // Momentarily set for calculation then cleared
                message = tempMessage
            )
        }
        if (buttonType == ButtonType.Exit && newExitTime != null && _uiState.value.totalWorkedTime != null) {
            // If exit was successful and record saved, reset the UI for the next day
            _uiState.update { it.copy(enterTime = null, toLunchTime = null, fromLunchTime = null, exitTime = null, totalWorkedTime = null, calculatedExitTime = null) }
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
            ButtonType.Exit -> current.exitTime // Should be null or the last recorded exit
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
            // Note: Editing Exit time via dialog doesn't finalize the day,
            // it just updates the value for recalculation. User still needs to press the main Exit button.

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
                ButtonType.Exit -> {
                    // This scenario is tricky. If we allow editing exit time directly to a past time,
                    // it implies the day ended. For simplicity, let's assume editing exit time
                    // via dialog just updates the value for calculation if user then presses the main exit button.
                    // Or, if user edits exit, we could auto-trigger the exit logic.
                    // For now, let's just update the state and message.
                    if (newEnterTime != null && selectedDate.before(newEnterTime)) {
                        tempMessage = "L'orario di uscita non può essere prima dell'ingresso."
                    } else {
                        // We are not setting uiState.exitTime here directly from dialog to complete the day.
                        // We update the local variable and then call recalculate.
                        // The main Exit button press is what finalizes.
                        // This behavior might need refinement based on exact UX desired.
                        tempMessage = "Orario di uscita (per calcolo) modificato: ${formatTime(selectedDate)}. Premi 'Uscita' per salvare."
                        // To make this change affect calculatedExitTime immediately before pressing main Exit button:
                        _uiState.value.copy(exitTime = selectedDate) // This is a temporary update for calculation
                    }
                }
            }
            current.copy(
                enterTime = newEnterTime,
                toLunchTime = newToLunchTime,
                fromLunchTime = newFromLunchTime,
                message = tempMessage,
                timePickerEvent = null // Clear event after handling
            )
        }
        recalculateAndUpdateUi()
    }

    override fun onDialogDismissed() {
        _uiState.update { it.copy(timePickerEvent = null) }
    }

    override fun clearMessage() {
        // Debounce or delay message clearing if needed, or clear immediately.
        // For Toast, it clears itself. For in-app messages, you might want this.
        if (_uiState.value.message.isNotEmpty() && !_uiState.value.message.startsWith("NOTIFY_")) {
            // viewModelScope.launch { delay(3000); _uiState.update { it.copy(message = "") } }
            _uiState.update { it.copy(message = "") } // Clear immediately for now
        }
    }

    // Helper to be called from Composable for formatting
    override fun formatTimeToDisplay(date: Date?): String {
        return date?.let { SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(it) } ?: "N/A"
    }

    private fun formatTime(date: Date?): String { // Internal utility
        return date?.let { SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(it) } ?: "N/A"
    }

    override fun onCleared() {
        super.onCleared()
        notificationPollingJob?.cancel()
    }
}