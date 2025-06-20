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
    // REPLACED Gson with kotlinx.serialization.json
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
                    // USE kotlinx.serialization.json for deserialization
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
            recalculateAndUpdateUi()
            if (restoredEnterTime != null && restoredExitTime == null) {
                startOrUpdateNotificationPolling(_uiState.value.calculatedExitTime)
            }
        } else {
            Log.d("HomeViewModel", "No current session found in DataStore.")
        }
    }

    private suspend fun persistCurrentSessionState() {
        val current = _uiState.value
        Log.d("HomeViewModel", "Persisting current session state to DataStore.")
        val enterTimeMillis = current.enterTime?.time
        val exitTimeMillis = current.exitTime?.time

        val pausesJson: Set<String>? = current.pauses.mapNotNull { pause ->
            try {
                // USE kotlinx.serialization.json for serialization
                json.encodeToString(SerializablePausePair(pause.start?.time, pause.end?.time))
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error serializing pause with kotlinx.serialization: $pause", e)
                null // Skip this pause if serialization fails
            }
        }.toSet().ifEmpty { null }


        try {
            workHistoryRepository.saveCurrentSession(enterTimeMillis, exitTimeMillis, pausesJson)
            Log.d("HomeViewModel", "Session state persisted successfully.")
        } catch (e: Exception) {
            Log.e("HomeViewModel", "Error persisting session state.", e)
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
            null
        }
        val newTotalWorkedTime = calculateTotalWorkedTime(current.enterTime, current.pauses, current.exitTime)

        _uiState.update {
            it.copy(
                calculatedExitTime = newCalculatedExitTime,
                totalWorkedTime = newTotalWorkedTime
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

    override fun handleTimeButtonPress(buttonType: ButtonType) {
        val currentTime = Date()
        val currentUiStateValue = _uiState.value
        var tempMessage = ""

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
                viewModelScope.launch { persistCurrentSessionState() }
            }
            ButtonType.Exit -> {
                if (currentUiStateValue.enterTime == null) {
                    tempMessage = "Devi prima registrare l'ingresso per questa sessione!"
                    _uiState.update { it.copy(message = tempMessage) }
                    return
                }
                val exitTimeToUse = currentUiStateValue.exitTime ?: currentTime
                val finalTotalWorked = calculateTotalWorkedTime(currentUiStateValue.enterTime, currentUiStateValue.pauses, exitTimeToUse)
                tempMessage = "Uscita registrata: ${formatTime(exitTimeToUse)}. Totale: $finalTotalWorked"
                Log.d("HomeViewModel", "Exit pressed at ${formatTime(exitTimeToUse)}")

                if (finalTotalWorked != null) {
                    val entryToSave = WorkHistoryEntry(
                        id = currentUiStateValue.enterTime.time.toString(),
                        enterTime = currentUiStateValue.enterTime.time,
                        pauses = currentUiStateValue.pauses.map { SerializablePausePair(it.start?.time, it.end?.time) },
                        exitTime = exitTimeToUse.time,
                        totalWorkedTime = finalTotalWorked,
                        dailyHoursTarget = currentUiStateValue.dailyHours
                    )
                    viewModelScope.launch {
                        workHistoryRepository.addWorkHistoryEntry(entryToSave)
                        Log.d("HomeViewModel", "Work history entry saved to DataStore. ID: ${entryToSave.id}")
                        workHistoryRepository.saveCurrentSession(null, null, null)
                        Log.d("HomeViewModel", "Current session cleared after saving to history.")
                    }
                } else {
                    tempMessage = "Uscita registrata, ma impossibile calcolare il totale lavorato."
                }
                _uiState.update {
                    it.copy(
                        exitTime = exitTimeToUse,
                        totalWorkedTime = finalTotalWorked,
                        calculatedExitTime = null,
                        message = tempMessage
                    )
                }
                viewModelScope.launch { persistCurrentSessionState() }
                notificationPollingJob?.cancel()
                Log.d("HomeViewModel", "Polling stopped due to Exit button press.")
            }
            else -> { /* Other button types handled by specific functions */ }
        }
        recalculateAndUpdateUi()
    }


    override fun handleTimeEditRequest(buttonType: ButtonType) {
        val current = _uiState.value
        val calendar = Calendar.getInstance()
        val timeToEdit: Date? = when (buttonType) {
            ButtonType.Enter -> current.enterTime
            ButtonType.Exit -> current.exitTime ?: current.calculatedExitTime
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
        calendar.time = pauseToEdit.start ?: Date()

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
        calendar.time = pauseToEdit.end ?: pauseToEdit.start ?: Date()
        if (pauseToEdit.start != null && calendar.time.before(pauseToEdit.start)) {
            calendar.time = pauseToEdit.start
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

        _uiState.update { current ->
            var newEnterTime = current.enterTime
            var newPauses = current.pauses.toMutableList()
            var newExitTime = current.exitTime

            if (pauseIndexToEdit != null) {
                if (pauseIndexToEdit >= 0 && pauseIndexToEdit < newPauses.size) {
                    val originalPause = newPauses[pauseIndexToEdit]
                    when (buttonType) {
                        ButtonType.ToLunch -> {
                            if (current.enterTime != null && selectedDate.before(current.enterTime)) {
                                tempMessage = "L'inizio pausa non può essere prima dell'ingresso."
                            } else if (originalPause.end != null && selectedDate.after(originalPause.end)) {
                                tempMessage = "L'inizio pausa non può essere dopo la fine della stessa pausa."
                            } else {
                                newPauses[pauseIndexToEdit] = originalPause.copy(start = selectedDate)
                                tempMessage = "Inizio pausa ${pauseIndexToEdit + 1} modificato: ${formatTime(selectedDate)}"
                                viewModelScope.launch { persistCurrentSessionState() }
                            }
                        }
                        ButtonType.FromLunch -> {
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
                        else -> { tempMessage = "Tipo di modifica pausa non riconosciuto." }
                    }
                } else {
                    tempMessage = "Errore: Indice pausa non valido per la modifica."
                }
            } else {
                when (buttonType) {
                    ButtonType.Enter -> {
                        newEnterTime = selectedDate
                        tempMessage = "Ingresso modificato: ${formatTime(newEnterTime)}"
                        viewModelScope.launch { persistCurrentSessionState() }
                    }
                    ButtonType.Exit -> {
                        if (newEnterTime != null && selectedDate.before(newEnterTime)) {
                            tempMessage = "L'orario di uscita non può essere prima dell'ingresso."
                        } else {
                            newExitTime = selectedDate
                            tempMessage = "Orario di uscita modificato: ${formatTime(newExitTime)}. Premi 'Uscita' per salvare."
                            viewModelScope.launch { persistCurrentSessionState() }
                        }
                    }
                    else -> { tempMessage = "Modifica non applicata (tipo non per modifica diretta Ingresso/Uscita)." }
                }
            }

            current.copy(
                enterTime = newEnterTime,
                pauses = newPauses.toList(),
                exitTime = newExitTime,
                message = tempMessage,
                timePickerEvent = null
            )
        }
        recalculateAndUpdateUi()
    }

    override fun onDialogDismissed() {
        _uiState.update { current ->
            if (current.message.startsWith("Errore:") ||
                current.message.startsWith("L'inizio pausa non può essere") ||
                current.message.startsWith("La fine pausa non può essere") ||
                current.message.startsWith("L'orario di uscita non può essere") ||
                current.message.startsWith("Impossibile modificare fine pausa")) {
                current.copy(timePickerEvent = null)
            } else {
                current.copy(timePickerEvent = null, message = "")
            }
        }
    }

    override fun clearMessage() {
        if (_uiState.value.message.isNotEmpty() &&
            !_uiState.value.message.startsWith("NOTIFY_") &&
            !_uiState.value.message.startsWith("Errore:") &&
            !_uiState.value.message.startsWith("L'inizio pausa non può essere") &&
            !_uiState.value.message.startsWith("La fine pausa non può essere") &&
            !_uiState.value.message.startsWith("L'orario di uscita non può essere") &&
            !_uiState.value.message.startsWith("Impossibile modificare fine pausa")
        ) {
            _uiState.update { it.copy(message = "") }
        }
    }


    override fun formatTimeToDisplay(date: Date?): String {
        // Consider making this consistent with formatTime if seconds are not needed for display
        return date?.let { SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(it) } ?: "N/A"
    }

    private fun formatTime(date: Date?): String {
        return date?.let { SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(it) } ?: "N/A"
    }

    override fun handleAddPause() {
        _uiState.update { current ->
            val newPauses = current.pauses.toMutableList().apply { add(PausePair()) }
            current.copy(pauses = newPauses, message = "Nuova pausa aggiunta. Premi 'In Pausa'.")
        }
        viewModelScope.launch { persistCurrentSessionState() }
        recalculateAndUpdateUi()
    }

    override fun handlePauseStart(index: Int) {
        val currentTime = Date()
        _uiState.update { current ->
            if (index < 0 || index >= current.pauses.size) {
                return@update current.copy(message = "Errore: Pausa non trovata.")
            }
            if (current.enterTime == null) {
                return@update current.copy(message = "Devi prima registrare l'ingresso!")
            }
            val newPauses = current.pauses.toMutableList()
            val currentPause = newPauses[index]

            if (currentPause.start != null && currentPause.end == null) {
                return@update current.copy(message = "Pausa ${index + 1} già iniziata.")
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
            if (index < 0 || index >= current.pauses.size) {
                return@update current.copy(message = "Errore: Pausa non trovata.")
            }
            val newPauses = current.pauses.toMutableList()
            val currentPause = newPauses[index]

            if (currentPause.start == null) {
                return@update current.copy(message = "Devi prima iniziare la pausa ${index + 1}.")
            }
            if (currentTime.before(currentPause.start)) {
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
        Log.d("HomeViewModel", "onCleared called, notification polling job cancelled. Attempting to persist final session state.")
        viewModelScope.launch { persistCurrentSessionState() }
    }
}