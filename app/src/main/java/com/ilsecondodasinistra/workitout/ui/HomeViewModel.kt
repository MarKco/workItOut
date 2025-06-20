package com.ilsecondodasinistra.workitout.ui

import IHomeViewModel
import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.viewModelScope
import com.ilsecondodasinistra.workitout.data.HistoryRepository
import com.ilsecondodasinistra.workitout.data.SerializablePausePair
import com.ilsecondodasinistra.workitout.data.WorkHistoryEntry
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

data class PausePair(
    val start: Date? = null,
    val end: Date? = null
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

sealed class ButtonType(val text: String) {
    object Enter : ButtonType("Ingresso")
    object ToLunch : ButtonType("In Pausa")
    object FromLunch : ButtonType("Fine Pausa")
    object Exit : ButtonType("Uscita")
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

    private suspend fun loadCurrentSession() {
        Log.d("HomeViewModel", "Attempting to load current session from DataStore.")
        val sessionData = workHistoryRepository.getCurrentSession().firstOrNull()
        if (sessionData != null) {
            val (enterTimeMillis, exitTimeMillis, currentPausesJson) = sessionData
            val restoredEnterTime = enterTimeMillis?.let { Date(it) }
            // If exitTimeMillis is present, it means a session was concluded but not fully cleared or app was killed.
            // We should honor this as the effective exit, but not reset the UI if enterTime is also present.
            val restoredExitTime = exitTimeMillis?.let { Date(it) }

            val restoredPauses: List<PausePair> = currentPausesJson?.mapNotNull { jsonString ->
                try {
                    val serializablePause = json.decodeFromString<SerializablePausePair>(jsonString)
                    PausePair(
                        start = serializablePause.start?.let { Date(it) },
                        end = serializablePause.end?.let { Date(it) }
                    )
                } catch (e: Exception) {
                    Log.e("HomeViewModel", "Error deserializing pause: $jsonString", e)
                    null
                }
            } ?: emptyList()

            _uiState.update {
                it.copy(
                    enterTime = restoredEnterTime,
                    exitTime = restoredExitTime, // Will show if session was previously concluded
                    pauses = restoredPauses,
                    message = "Sessione precedente ripristinata."
                )
            }
            Log.d("HomeViewModel", "Session restored: Enter=${formatTime(restoredEnterTime)}, Exit=${formatTime(restoredExitTime)}, Pauses=${restoredPauses.size}")
            recalculateAndUpdateUi() // This will update totalWorkedTime and calculatedExitTime based on restored state

            if (restoredEnterTime != null && restoredExitTime == null) { // Only poll if session is ongoing
                 startOrUpdateNotificationPolling(_uiState.value.calculatedExitTime)
            }
        } else {
            Log.d("HomeViewModel", "No current session found in DataStore.")
        }
    }

    private suspend fun persistCurrentSessionState() {
        val current = _uiState.value
        // Only persist if there's an active, uncompleted session
        if (current.enterTime != null && current.exitTime == null) {
            Log.d("HomeViewModel", "Persisting current active session state to DataStore.")
            val enterTimeMillis = current.enterTime?.time
            // exitTimeMillis should be null here for an active session
            val pausesJson: Set<String>? = current.pauses.mapNotNull { pause ->
                try {
                    json.encodeToString(SerializablePausePair(pause.start?.time, pause.end?.time))
                } catch (e: Exception) {
                    Log.e("HomeViewModel", "Error serializing pause: $pause", e)
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
            Log.d("HomeViewModel", "Skipping persistence: No active session (enterTime null or exitTime already set).")
        }
    }


    private fun observeDailyHours() {
        viewModelScope.launch {
            workHistoryRepository.getDailyHours().collect { loadedDailyHours ->
                if (_uiState.value.dailyHours != loadedDailyHours) {
                    _uiState.update {
                        it.copy(dailyHours = loadedDailyHours)
                    }
                    recalculateAndUpdateUi() // Recalculate if daily hours change
                    Log.d("HomeViewModel", "Daily hours updated from DataStore: $loadedDailyHours")
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
            null // If session is already exited, no calculated exit time needed
        }
        val newTotalWorkedTime = if (current.exitTime != null && current.enterTime != null) {
            calculateTotalWorkedTime(current.enterTime, current.pauses, current.exitTime)
        } else {
            current.totalWorkedTime // Preserve if already calculated, or null if not yet exited
        }

        _uiState.update {
            it.copy(
                calculatedExitTime = newCalculatedExitTime,
                totalWorkedTime = newTotalWorkedTime
            )
        }
         if (current.enterTime != null && current.exitTime == null) { // Only poll if session is ongoing
            startOrUpdateNotificationPolling(newCalculatedExitTime)
        } else {
            notificationPollingJob?.cancel()
             Log.d("HomeViewModel", "Recalculate: Polling stopped (enter null or exit already set).")
        }
    }


    private fun startOrUpdateNotificationPolling(expectedExitTime: Date?) {
        notificationPollingJob?.cancel()
        if (expectedExitTime == null || _uiState.value.exitTime != null || _uiState.value.enterTime == null) {
            Log.d("HomeViewModel", "Notification polling stopped/not started. Expected: ${formatTime(expectedExitTime)}, Actual Exit: ${formatTime(_uiState.value.exitTime)}, Enter: ${formatTime(_uiState.value.enterTime)}")
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

    // This function now handles all session finalization logic
    private suspend fun finalizeAndSaveWorkSession(rawConfirmedExitTime: Date) {
        val current = _uiState.value
        if (current.enterTime == null) {
            _uiState.update { it.copy(message = "Devi prima registrare l'ingresso per questa sessione!") }
            return
        }

        var adjustedExitTime = rawConfirmedExitTime
        if (rawConfirmedExitTime.before(current.enterTime)) {
            val calendar = Calendar.getInstance()
            calendar.time = rawConfirmedExitTime
            calendar.add(Calendar.DAY_OF_YEAR, 1)
            adjustedExitTime = calendar.time
            Log.d("HomeViewModel", "Exit time ${formatTime(rawConfirmedExitTime)} was before entry ${formatTime(current.enterTime)}. Adjusted to next day: ${formatTime(adjustedExitTime)}")
        }

        val finalTotalWorked = calculateTotalWorkedTime(current.enterTime, current.pauses, adjustedExitTime)
        var tempMessage: String

        if (finalTotalWorked != null) {
            val entryToSave = WorkHistoryEntry(
                id = current.enterTime.time.toString(), // Consider a more robust ID
                enterTime = current.enterTime.time,
                pauses = current.pauses.map { SerializablePausePair(it.start?.time, it.end?.time) },
                exitTime = adjustedExitTime.time,
                totalWorkedTime = finalTotalWorked,
                dailyHoursTarget = current.dailyHours
            )
            try {
                workHistoryRepository.addWorkHistoryEntry(entryToSave)
                workHistoryRepository.saveCurrentSession(null, null, null) // Clear active session
                tempMessage = "Sessione salvata. Totale: $finalTotalWorked"
                Log.d("HomeViewModel", "Work history entry saved. ID: ${entryToSave.id}. Active session cleared.")
                notificationPollingJob?.cancel()
                Log.d("HomeViewModel", "Polling stopped due to session finalization.")
            } catch (e: Exception) {
                tempMessage = "Errore durante il salvataggio della sessione."
                Log.e("HomeViewModel", "Error saving work history or clearing session", e)
            }
        } else {
            tempMessage = "Uscita registrata, ma impossibile calcolare il totale lavorato."
            Log.w("HomeViewModel", "Could not calculate total worked time. Enter: ${formatTime(current.enterTime)}, Exit: ${formatTime(adjustedExitTime)}")
        }

        _uiState.update {
            it.copy(
                exitTime = adjustedExitTime, // Show the (potentially adjusted) exit time
                totalWorkedTime = finalTotalWorked,
                calculatedExitTime = null, // Session is now complete
                message = tempMessage
                // enterTime and pauses remain to show the completed session details
            )
        }
        // No recalculateAndUpdateUi() here, as we've explicitly set the final state.
    }


    override fun handleTimeButtonPress(buttonType: ButtonType) {
        val currentTime = Date()
        val currentUiStateValue = _uiState.value // For read-only access to current state

        when (buttonType) {
            ButtonType.Enter -> {
                _uiState.update {
                    // This is where UI is reset for a new session
                    it.copy(
                        enterTime = currentTime,
                        pauses = emptyList(),
                        exitTime = null,
                        totalWorkedTime = null,
                        calculatedExitTime = null, // Will be recalculated
                        message = "Nuovo Ingresso registrato: ${formatTime(currentTime)}"
                    )
                }
                Log.d("HomeViewModel", "Enter pressed. New shift started at ${formatTime(currentTime)}")
                viewModelScope.launch { persistCurrentSessionState() } // Persist the new active session
                recalculateAndUpdateUi() // Recalculate for the new session
            }
            ButtonType.Exit -> {
                if (currentUiStateValue.enterTime == null) {
                    _uiState.update { it.copy(message = "Devi prima registrare l'ingresso per questa sessione!") }
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
            else -> null
        }
        timeToEdit?.let { calendar.time = it }

        _uiState.update {
            it.copy(
                timePickerEvent = TimePickerEvent(
                    type = buttonType,
                    pauseIndex = null,
                    initialHour = calendar.get(Calendar.HOUR_OF_DAY),
                    initialMinute = calendar.get(Calendar.MINUTE)
                )
            )
        }
    }

    override fun handlePauseEditStart(index: Int) {
        val currentPauses = _uiState.value.pauses
        if (index < 0 || index >= currentPauses.size) {
            _uiState.update { it.copy(message = "Errore: Pausa non trovata per la modifica.") }
            return
        }
        val pauseToEdit = currentPauses[index]
        val calendar = Calendar.getInstance()
        calendar.time = pauseToEdit.start ?: Date() // Default to now if start is null

        _uiState.update {
            it.copy(
                timePickerEvent = TimePickerEvent(
                    type = ButtonType.ToLunch,
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
            _uiState.update { it.copy(message = "Errore: Pausa non trovata per la modifica.") }
            return
        }
        val pauseToEdit = currentPauses[index]
        val calendar = Calendar.getInstance()
        calendar.time = pauseToEdit.end ?: pauseToEdit.start ?: Date() // Default to start if end is null, then to now
        if (pauseToEdit.start != null && calendar.time.before(pauseToEdit.start)) {
            calendar.time = pauseToEdit.start // Ensure end is not before start
        }

        _uiState.update {
            it.copy(
                timePickerEvent = TimePickerEvent(
                    type = ButtonType.FromLunch,
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
                        message = "Ingresso modificato: ${formatTime(selectedDate)}"
                        // exitTime, pauses, etc., remain as they were, subject to recalculateAndUpdateUi
                    )
                }
                viewModelScope.launch { persistCurrentSessionState() }
            }
            ButtonType.Exit -> {
                val currentEnterTime = _uiState.value.enterTime
                if (currentEnterTime == null) {
                     _uiState.update { it.copy(message = "Devi prima registrare l'ingresso.") }
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
                                if (current.enterTime != null && selectedDate.before(current.enterTime)) {
                                    tempMessage = "L'inizio pausa non può essere prima dell'ingresso."
                                } else if (originalPause.end != null && selectedDate.after(originalPause.end)) {
                                    tempMessage = "L'inizio pausa non può essere dopo la fine della stessa pausa."
                                } else {
                                    newPauses[pauseIndexToEdit] = originalPause.copy(start = selectedDate)
                                    tempMessage = "Inizio pausa ${pauseIndexToEdit + 1} modificato: ${formatTime(selectedDate)}"
                                    viewModelScope.launch { persistCurrentSessionState() }
                                }
                            } else { // ButtonType.FromLunch
                                if (originalPause.start == null) {
                                    tempMessage = "Impossibile modificare fine pausa: inizio pausa non impostato."
                                } else if (selectedDate.before(originalPause.start)) {
                                    tempMessage = "La fine pausa non può essere prima dell'inizio della stessa pausa."
                                } else {
                                    newPauses[pauseIndexToEdit] = originalPause.copy(end = selectedDate)
                                    tempMessage = "Fine pausa ${pauseIndexToEdit + 1} modificata: ${formatTime(selectedDate)}"
                                    viewModelScope.launch { persistCurrentSessionState() }
                                }
                            }
                        } else {
                            tempMessage = "Errore: Indice pausa non valido per la modifica."
                        }
                        current.copy(pauses = newPauses.toList(), message = tempMessage)
                    }
                } else {
                     _uiState.update { it.copy(message = "Errore: Modifica pausa non riuscita.")}
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
        if (currentMessage.isNotEmpty() &&
            !currentMessage.startsWith("NOTIFY_") &&
            !currentMessage.startsWith("Errore:") &&
            !currentMessage.startsWith("L'inizio pausa non può essere") &&
            !currentMessage.startsWith("La fine pausa non può essere") &&
            !currentMessage.startsWith("L'orario di uscita non può essere") &&
            !currentMessage.startsWith("Impossibile modificare fine pausa") &&
            !currentMessage.contains("Sessione salvata") // Don't clear success messages immediately
        ) {
            _uiState.update { it.copy(message = "") }
        }
        // Always ensure the picker event is cleared on dismiss
        _uiState.update { it.copy(timePickerEvent = null) }
    }

    override fun clearMessage() {
        val currentMessage = _uiState.value.message
        if (currentMessage.isNotEmpty() &&
            !currentMessage.startsWith("NOTIFY_") &&
            !currentMessage.startsWith("Errore:") &&
            !currentMessage.startsWith("L'inizio pausa non può essere") &&
            !currentMessage.startsWith("La fine pausa non può essere") &&
            !currentMessage.startsWith("L'orario di uscita non può essere") &&
            !currentMessage.startsWith("Impossibile modificare fine pausa") &&
            !currentMessage.contains("Sessione salvata") // Don't clear success messages from here either
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
                return@update current.copy(message = "Registra prima l'ingresso per aggiungere una pausa.")
            }
            if (current.exitTime != null) {
                return@update current.copy(message = "Sessione già conclusa. Impossibile aggiungere pause.")
            }
            val newPauses = current.pauses.toMutableList().apply { add(PausePair()) }
            current.copy(pauses = newPauses, message = "Nuova pausa aggiunta. Premi 'In Pausa'.")
        }
        viewModelScope.launch { persistCurrentSessionState() }
        recalculateAndUpdateUi()
    }

    override fun handlePauseStart(index: Int) {
        val currentTime = Date()
        _uiState.update { current ->
            if (current.enterTime == null) {
                return@update current.copy(message = "Devi prima registrare l'ingresso!")
            }
            if (current.exitTime != null) {
                return@update current.copy(message = "Sessione già conclusa. Impossibile iniziare una pausa.")
            }
            if (index < 0 || index >= current.pauses.size) {
                return@update current.copy(message = "Errore: Pausa non trovata.")
            }

            val newPauses = current.pauses.toMutableList()
            val currentPause = newPauses[index]

            if (currentPause.start != null && currentPause.end == null) {
                return@update current.copy(message = "Pausa ${index + 1} già iniziata.")
            }
             if (currentPause.start != null && currentPause.end != null && currentTime.before(currentPause.end)) {
                 return@update current.copy(message = "Non puoi iniziare una nuova pausa prima della fine della precedente.")
            }


            newPauses[index] = currentPause.copy(start = currentTime, end = null)
            current.copy(pauses = newPauses, message = "Inizio pausa ${index + 1}: ${formatTime(currentTime)}")
        }
        viewModelScope.launch { persistCurrentSessionState() }
        recalculateAndUpdateUi()
    }

    override fun handlePauseEnd(index: Int) {
        val currentTime = Date()
        _uiState.update { current ->
             if (current.enterTime == null) { // Should not happen if pause button is enabled correctly
                return@update current.copy(message = "Errore: Ingresso non registrato.")
            }
            if (current.exitTime != null) {
                return@update current.copy(message = "Sessione già conclusa. Impossibile terminare una pausa.")
            }
            if (index < 0 || index >= current.pauses.size) {
                return@update current.copy(message = "Errore: Pausa non trovata.")
            }

            val newPauses = current.pauses.toMutableList()
            val currentPause = newPauses[index]

            if (currentPause.start == null) {
                return@update current.copy(message = "Devi prima iniziare la pausa ${index + 1}.")
            }
            if (currentTime.before(currentPause.start)) {
                // This case should ideally be prevented by button logic or handled more gracefully
                return@update current.copy(message = "La fine pausa non può essere prima dell'inizio.")
            }
            newPauses[index] = currentPause.copy(end = currentTime)
            current.copy(pauses = newPauses, message = "Fine pausa ${index + 1}: ${formatTime(currentTime)}")
        }
        viewModelScope.launch { persistCurrentSessionState() }
        recalculateAndUpdateUi()
    }

    override fun onCleared() {
        super.onCleared()
        notificationPollingJob?.cancel()
        Log.d("HomeViewModel", "onCleared called, notification polling job cancelled.")
        // Persist only if there's an active, uncompleted session
        if (_uiState.value.enterTime != null && _uiState.value.exitTime == null) {
            Log.d("HomeViewModel", "Attempting to persist final session state onCleared.")
            // Run this synchronously if needed, or manage with a different scope if viewModelScope is cancelling
            // For simplicity, launching in viewModelScope, but be aware of its lifecycle.
             viewModelScope.launch { persistCurrentSessionState() }
        }
    }
}
