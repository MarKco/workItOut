package com.ilsecondodasinistra.workitout.ui

import IHomeViewModel
import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.viewModelScope
import com.ilsecondodasinistra.workitout.data.HistoryRepository
import com.ilsecondodasinistra.workitout.data.SerializablePausePair // Assuming this is @Serializable
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
            val restoredExitTime = exitTimeMillis?.let { Date(it) }

            val restoredPauses: List<PausePair> = currentPausesJson?.mapNotNull { jsonString ->
                try {
                    val serializablePause = json.decodeFromString<SerializablePausePair>(jsonString)
                    PausePair(
                        start = serializablePause.start?.let { Date(it) },
                        end = serializablePause.end?.let { Date(it) }
                    )
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
                    message = "Sessione precedente ripristinata."
                )
            }
            Log.d("HomeViewModel", "Session restored: Enter=${formatTime(restoredEnterTime)}, Exit=${formatTime(restoredExitTime)}, Pauses=${restoredPauses.size}")
            recalculateAndUpdateUi() // Recalculate based on restored state
            if (restoredEnterTime != null && restoredExitTime == null) {
                startOrUpdateNotificationPolling(_uiState.value.calculatedExitTime)
            }
        } else {
            Log.d("HomeViewModel", "No current session found in DataStore.")
        }
    }

    private suspend fun persistCurrentSessionState() {
        val current = _uiState.value
        // Only persist if there's an active session (enterTime not null, and exitTime is null)
        if (current.enterTime != null && current.exitTime == null) {
            Log.d("HomeViewModel", "Persisting active session state to DataStore.")
            val enterTimeMillis = current.enterTime.time // enterTime is non-null here
            val exitTimeMillis = current.exitTime?.time // exitTime is null here, so this will be null

            val pausesJson: Set<String>? = current.pauses.mapNotNull { pause ->
                try {
                    json.encodeToString(SerializablePausePair(pause.start?.time, pause.end?.time))
                } catch (e: Exception) {
                    Log.e("HomeViewModel", "Error serializing pause with kotlinx.serialization: $pause", e)
                    null
                }
            }.toSet().ifEmpty { null }

            try {
                workHistoryRepository.saveCurrentSession(enterTimeMillis, exitTimeMillis, pausesJson)
                Log.d("HomeViewModel", "Active session state persisted successfully.")
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error persisting active session state.", e)
            }
        } else {
            Log.d("HomeViewModel", "No active session to persist or session already concluded.")
        }
    }


    private fun observeDailyHours() {
        viewModelScope.launch {
            workHistoryRepository.getDailyHours().collect { loadedDailyHours ->
                if (_uiState.value.dailyHours != loadedDailyHours) {
                    _uiState.update {
                        it.copy(dailyHours = loadedDailyHours)
                    }
                    recalculateAndUpdateUi()
                    Log.d("HomeViewModel", "Daily hours updated from DataStore: $loadedDailyHours")
                } else {
                    Log.d("HomeViewModel", "Daily hours from DataStore same as current: $loadedDailyHours, no recalculation needed based on this.")
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
            null // If exitTime is already set, no need for calculatedExitTime
        }
        // Only calculate totalWorkedTime if exitTime is actually set.
        // Otherwise, it might show a total for an incomplete session based on a previous value.
        val newTotalWorkedTime = if (current.exitTime != null) {
            calculateTotalWorkedTime(current.enterTime, current.pauses, current.exitTime)
        } else {
            current.totalWorkedTime // Keep existing if no exit time, or null if never set
        }


        _uiState.update {
            it.copy(
                calculatedExitTime = newCalculatedExitTime,
                totalWorkedTime = newTotalWorkedTime
            )
        }
        // Manage notification polling based on whether the session is active
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

    private suspend fun finalizeAndSaveWorkSession(confirmedExitTime: Date) {
        val current = _uiState.value
        var tempMessage = ""

        if (current.enterTime == null) {
            tempMessage = "Errore: Ingresso non registrato. Impossibile salvare."
            _uiState.update { it.copy(message = tempMessage) }
            return
        }

        val finalTotalWorked = calculateTotalWorkedTime(current.enterTime, current.pauses, confirmedExitTime)

        if (finalTotalWorked != null) {
            val entryToSave = WorkHistoryEntry(
                id = current.enterTime.time.toString(), // Assuming enterTime is non-null due to the check above
                enterTime = current.enterTime.time,
                pauses = current.pauses.map { SerializablePausePair(it.start?.time, it.end?.time) },
                exitTime = confirmedExitTime.time,
                totalWorkedTime = finalTotalWorked,
                dailyHoursTarget = current.dailyHours
            )
            try {
                workHistoryRepository.addWorkHistoryEntry(entryToSave)
                Log.d("HomeViewModel", "Work history entry saved to DataStore. ID: ${entryToSave.id}")
                workHistoryRepository.saveCurrentSession(null, null, null) // Clear active session
                Log.d("HomeViewModel", "Active session cleared after saving to history.")
                tempMessage = "Sessione salvata: ${formatTime(confirmedExitTime)}. Totale: $finalTotalWorked"
                notificationPollingJob?.cancel()
                Log.d("HomeViewModel", "Polling stopped due to session finalization.")
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error saving work history entry or clearing session.", e)
                tempMessage = "Errore durante il salvataggio della sessione."
            }
        } else {
            tempMessage = "Uscita registrata, ma errore nel calcolo del totale lavorato."
        }

        _uiState.update {
            it.copy(
                // enterTime and pauses are NOT reset here, as per requirement
                exitTime = confirmedExitTime,
                totalWorkedTime = finalTotalWorked,
                calculatedExitTime = null, // Session is now complete
                message = tempMessage
            )
        }
    }


    override fun handleTimeButtonPress(buttonType: ButtonType) {
        val currentTime = Date()
        val currentUiStateValue = _uiState.value // Use a consistent snapshot for this operation

        when (buttonType) {
            ButtonType.Enter -> {
                _uiState.update {
                    it.copy(
                        enterTime = currentTime,
                        pauses = emptyList(),
                        exitTime = null,
                        totalWorkedTime = null,
                        calculatedExitTime = null,
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
                if (currentEnterTime != null && selectedDate.before(currentEnterTime)) {
                    _uiState.update { it.copy(message = "L'orario di uscita non può essere prima dell'ingresso.") }
                } else if (currentEnterTime == null) {
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
            // Prevent starting a new pause if the previous one isn't complete (unless it's the first pause)
            if (index > 0 && newPauses[index-1].end == null && newPauses[index-1].start != null) {
                 return@update current.copy(message = "Completa la pausa precedente prima di iniziarne una nuova.")
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
            if (current.enterTime == null) { // Should not happen if pause was started, but good check
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
                // This case should ideally be handled by the time picker validation if one is used for pause end
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
        Log.d("HomeViewModel", "onCleared called, notification polling job cancelled. Attempting to persist final session state if active.")
        // Persist only if there's an active, uncompleted session
        if (_uiState.value.enterTime != null && _uiState.value.exitTime == null) {
            viewModelScope.launch { persistCurrentSessionState() }
        }
    }
}
