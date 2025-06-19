package com.ilsecondodasinistra.workitout.ui

import IHomeViewModel
import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.AndroidViewModel
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

// Constants for SharedPreferences
private const val PREFS_NAME = "workitout_settings_prefs"
private const val KEY_DAILY_HOURS = "daily_hours"
private const val KEY_CURRENT_DAY_PREFIX = "current_day_"
private const val KEY_ENTER_TIME = "enter_time"
private const val KEY_TO_LUNCH_TIME = "to_lunch_time"
private const val KEY_FROM_LUNCH_TIME = "from_lunch_time"
private const val KEY_EXIT_TIME = "exit_time" // Added for persistent exit time

data class HomeUiState(
    val enterTime: Date? = null,
    val toLunchTime: Date? = null,
    val fromLunchTime: Date? = null,
    val exitTime: Date? = null, // Will now persist in UI after exit
    val calculatedExitTime: Date? = null,
    val totalWorkedTime: String? = null,
    val dailyHours: Double = 8.0,
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

    private val sharedPreferences: SharedPreferences =
        application.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(HomeUiState())
    override val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var notificationPollingJob: Job? = null

    init {
        loadInitialData()
    }

    override fun onResume(owner: LifecycleOwner) {
        super.onResume(owner)
        Log.d("HomeViewModel", "onResume called, reloading daily hours.")
        loadDailyHoursFromPrefs()
    }

    private fun loadDailyHoursFromPrefs() {
        viewModelScope.launch {
            val loadedDailyHours = sharedPreferences.getFloat(KEY_DAILY_HOURS, 8.0f).toDouble()
            if (_uiState.value.dailyHours != loadedDailyHours) {
                _uiState.update {
                    it.copy(dailyHours = loadedDailyHours)
                }
                recalculateAndUpdateUi()
                Log.d("HomeViewModel", "Daily hours reloaded and UI updated: $loadedDailyHours")
            } else {
                Log.d("HomeViewModel", "Daily hours unchanged: $loadedDailyHours")
            }
        }
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            val savedEnterTimeMs = sharedPreferences.getLong("${KEY_CURRENT_DAY_PREFIX}$KEY_ENTER_TIME", -1L)
            val savedToLunchTimeMs = sharedPreferences.getLong("${KEY_CURRENT_DAY_PREFIX}$KEY_TO_LUNCH_TIME", -1L)
            val savedFromLunchTimeMs = sharedPreferences.getLong("${KEY_CURRENT_DAY_PREFIX}$KEY_FROM_LUNCH_TIME", -1L)
            val savedExitTimeMs = sharedPreferences.getLong("${KEY_CURRENT_DAY_PREFIX}$KEY_EXIT_TIME", -1L) // Load saved exit time

            val initialEnterTime = if (savedEnterTimeMs != -1L) Date(savedEnterTimeMs) else null
            val initialToLunchTime = if (savedToLunchTimeMs != -1L) Date(savedToLunchTimeMs) else null
            val initialFromLunchTime = if (savedFromLunchTimeMs != -1L) Date(savedFromLunchTimeMs) else null
            val initialExitTime = if (savedExitTimeMs != -1L) Date(savedExitTimeMs) else null // Assign loaded exit time

            val loadedDailyHours = sharedPreferences.getFloat(KEY_DAILY_HOURS, 8.0f).toDouble()

            _uiState.update {
                it.copy(
                    dailyHours = loadedDailyHours,
                    enterTime = initialEnterTime,
                    toLunchTime = initialToLunchTime,
                    fromLunchTime = initialFromLunchTime,
                    exitTime = initialExitTime, // Set exitTime in UI state
                    message = if (initialEnterTime != null && initialExitTime == null) "Giorno lavorativo ripreso." else if (initialExitTime != null) "Giorno precedente completato." else ""
                )
            }
            recalculateAndUpdateUi()
            Log.d("HomeViewModel", "Initial data loaded. Daily Hours: $loadedDailyHours, Exit Time: ${initialExitTime?.let { formatTime(it) }}")
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
            currentState.exitTime?.let { putLong("${KEY_CURRENT_DAY_PREFIX}$KEY_EXIT_TIME", it.time) } // Save exit time
                ?: remove("${KEY_CURRENT_DAY_PREFIX}$KEY_EXIT_TIME")
            apply()
        }
        Log.d("HomeViewModel", "Current day progress saved. Exit Time: ${currentState.exitTime?.let { formatTime(it) }}")
    }

    private fun clearCurrentDayProgressFromPrefs() {
        with(sharedPreferences.edit()) {
            remove("${KEY_CURRENT_DAY_PREFIX}$KEY_ENTER_TIME")
            remove("${KEY_CURRENT_DAY_PREFIX}$KEY_TO_LUNCH_TIME")
            remove("${KEY_CURRENT_DAY_PREFIX}$KEY_FROM_LUNCH_TIME")
            remove("${KEY_CURRENT_DAY_PREFIX}$KEY_EXIT_TIME") // Clear exit time
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
        val newCalculatedExitTime = if (current.exitTime == null) { // Only calculate if day is not yet complete
            calculateExpectedExitTime(current.enterTime, current.toLunchTime, current.fromLunchTime, current.dailyHours)
        } else {
            null // If day is complete, no need for expected exit time
        }
        val newTotalWorkedTime = calculateTotalWorkedTime(current.enterTime, current.toLunchTime, current.fromLunchTime, current.exitTime)

        _uiState.update {
            it.copy(
                calculatedExitTime = newCalculatedExitTime,
                totalWorkedTime = newTotalWorkedTime
            )
        }
        // Save progress if there's any relevant time set (day active or completed)
        if (current.enterTime != null) {
            saveCurrentDayProgress()
        }
        startOrUpdateNotificationPolling(newCalculatedExitTime)
    }

    private fun startOrUpdateNotificationPolling(expectedExitTime: Date?) {
        notificationPollingJob?.cancel()
        if (expectedExitTime == null) return

        notificationPollingJob = viewModelScope.launch {
            var notified = false
            while (isActive && !notified) {
                if (System.currentTimeMillis() >= expectedExitTime.time) {
                    _uiState.update { it.copy(message = "NOTIFY_EXIT_TIME:${formatTime(expectedExitTime)}") }
                    notified = true
                }
                delay(10000L)
            }
        }
    }

    override fun handleTimeButtonPress(buttonType: ButtonType) {
        val currentTime = Date()
        var currentUiState = _uiState.value
        var newEnterTime = currentUiState.enterTime
        var newToLunchTime = currentUiState.toLunchTime
        var newFromLunchTime = currentUiState.fromLunchTime
        var newExitTime = currentUiState.exitTime // Preserve existing exit time
        var tempMessage = ""

        when (buttonType) {
            ButtonType.Enter -> {
                // If a previous day was completed (exitTime is not null), or if it's a fresh app start (enterTime is null)
                // then clear everything for the new day.
                if (currentUiState.exitTime != null || currentUiState.enterTime == null) {
                    clearCurrentDayProgressFromPrefs() // Clear all stored times
                    newEnterTime = currentTime
                    newToLunchTime = null
                    newFromLunchTime = null
                    newExitTime = null      // Explicitly null for a new day's start
                    tempMessage = "Ingresso registrato: ${formatTime(newEnterTime)}"
                } else { // Handle re-pressing "Ingresso" on an active day (overwrite)
                    newEnterTime = currentTime
                    newToLunchTime = null
                    newFromLunchTime = null
                    newExitTime = null // Clear exit if re-entering
                    tempMessage = "Ingresso sovrascritto: ${formatTime(newEnterTime)}"
                }
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
                    tempMessage = "Devi prima registrare l'ingresso!"
                } else {
                    newExitTime = currentTime // Set current exit time
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
                        // DO NOT clear newEnterTime, newToLunchTime, newFromLunchTime
                        // DO NOT call clearCurrentDayProgressFromPrefs() here.
                        // saveCurrentDayProgress() will be called by recalculateAndUpdateUi with the new exitTime.
                    } else {
                        tempMessage = "Uscita registrata, ma impossibile calcolare il totale lavorato."
                        // Do not clear newExitTime here, let it be saved as is for transparency if calculation fails.
                        // Or revert to previous newExitTime state (currentUiState.exitTime) if preferred.
                    }
                }
            }
        }

        _uiState.update {
            it.copy(
                enterTime = newEnterTime,
                toLunchTime = newToLunchTime,
                fromLunchTime = newFromLunchTime,
                exitTime = newExitTime, // Persist exitTime in UI
                message = tempMessage
            )
        }
        // The specific reset block for ButtonType.Exit that was here is removed.
        recalculateAndUpdateUi() // This will call saveCurrentDayProgress with the latest state
    }

    override fun handleTimeEditRequest(buttonType: ButtonType) {
        val current = _uiState.value
        val calendar = Calendar.getInstance()
        val timeToEdit: Date? = when (buttonType) {
            ButtonType.Enter -> current.enterTime
            ButtonType.ToLunch -> current.toLunchTime
            ButtonType.FromLunch -> current.fromLunchTime
            ButtonType.Exit -> current.exitTime
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
        var editedExitTime: Date? = _uiState.value.exitTime // Hold current exit time

        _uiState.update { current ->
            var newEnterTime = current.enterTime
            var newToLunchTime = current.toLunchTime
            var newFromLunchTime = current.fromLunchTime

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
                     if (newEnterTime != null && selectedDate.before(newEnterTime)) {
                        tempMessage = "L'orario di uscita non può essere prima dell'ingresso."
                    } else {
                        editedExitTime = selectedDate // Update the local exit time
                        tempMessage = "Orario di uscita modificato: ${formatTime(editedExitTime)}. Premi 'Uscita' per salvare le modifiche e completare il giorno."
                    }
                }
            }
            current.copy(
                enterTime = newEnterTime,
                toLunchTime = newToLunchTime,
                fromLunchTime = newFromLunchTime,
                exitTime = if (buttonType == ButtonType.Exit) editedExitTime else current.exitTime, // Apply edited exit time
                message = tempMessage,
                timePickerEvent = null
            )
        }
        recalculateAndUpdateUi()
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
    }
}
