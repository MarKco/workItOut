package com.ilsecondodasinistra.workitout.ui

import IHomeViewModel // Assuming this interface exists and will be updated
import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.viewModelScope
import com.ilsecondodasinistra.workitout.data.DataStoreHistoryRepository // Corrected from previous session
import com.ilsecondodasinistra.workitout.data.HistoryRepository // Assuming this is the interface
import com.ilsecondodasinistra.workitout.data.SerializablePausePair
import com.ilsecondodasinistra.workitout.data.WorkHistoryEntry
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
// import java.util.UUID // Unused import
import java.util.concurrent.TimeUnit

// PausePair is now defined directly below
data class PausePair(
    val start: Date? = null,
    val end: Date? = null
)

data class HomeUiState(
    val enterTime: Date? = null,
    val pauses: List<PausePair> = emptyList(), // This uses PausePair
    val exitTime: Date? = null,
    val calculatedExitTime: Date? = null,
    val totalWorkedTime: String? = null,
    val dailyHours: Double = 8.0,
    val message: String = "",
    val timePickerEvent: TimePickerEvent? = null
)

data class TimePickerEvent(
    val type: ButtonType,
    val pauseIndex: Int? = null, // Added for indexed pause editing
    val initialHour: Int,
    val initialMinute: Int
)

sealed class ButtonType(val text: String) {
    object Enter : ButtonType("Ingresso")
    object ToLunch : ButtonType("In Pausa") // Used for both starting a new pause and editing a pause start
    object FromLunch : ButtonType("Fine Pausa") // Used for both ending a new pause and editing a pause end
    object Exit : ButtonType("Uscita")
}

// Assuming IHomeViewModel interface would be updated with:
// fun handlePauseEditStart(index: Int)
// fun handlePauseEditEnd(index: Int)
// fun handlePauseStart(index: Int)
// fun handlePauseEnd(index: Int)
// fun handleAddPause()
// fun onDialogDismissed()
// fun clearMessage()
// fun formatTimeToDisplay(date: Date?): String

class HomeViewModel(application: Application) : AndroidViewModel(application), DefaultLifecycleObserver, IHomeViewModel {

    private val workHistoryRepository: HistoryRepository = DataStoreHistoryRepository(application)

    private val _uiState = MutableStateFlow(HomeUiState())
    override val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var notificationPollingJob: Job? = null

    init {
        loadDailyHoursFromDataStore()
    }

    override fun onResume(owner: LifecycleOwner) {
        super.onResume(owner)
        Log.d("HomeViewModel", "onResume called, reloading daily hours if necessary.")
        loadDailyHoursFromDataStore()
    }

    private fun loadDailyHoursFromDataStore() {
        viewModelScope.launch {
            val loadedDailyHours = workHistoryRepository.getDailyHours().first()
            if (_uiState.value.dailyHours != loadedDailyHours) {
                _uiState.update {
                    it.copy(dailyHours = loadedDailyHours)
                }
                recalculateAndUpdateUi()
                Log.d("HomeViewModel", "Daily hours loaded from DataStore: $loadedDailyHours")
            } else {
                Log.d("HomeViewModel", "Daily hours unchanged from DataStore: $loadedDailyHours")
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
        startOrUpdateNotificationPolling(newCalculatedExitTime)
    }

    private fun startOrUpdateNotificationPolling(expectedExitTime: Date?) {
        notificationPollingJob?.cancel()
        if (expectedExitTime == null || _uiState.value.exitTime != null) {
            Log.d("HomeViewModel", "Notification polling stopped or not started. Expected: $expectedExitTime, Actual Exit: ${_uiState.value.exitTime}")
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
        val currentUiState = _uiState.value 
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
            }
            ButtonType.Exit -> {
                if (currentUiState.enterTime == null) {
                    tempMessage = "Devi prima registrare l'ingresso per questa sessione!"
                     _uiState.update { it.copy(message = tempMessage) }
                } else {
                    val exitTimeToUse = currentUiState.exitTime ?: currentTime 
                    val finalTotalWorked = calculateTotalWorkedTime(currentUiState.enterTime, currentUiState.pauses, exitTimeToUse)
                    tempMessage = "Uscita registrata: ${formatTime(exitTimeToUse)}. Totale: $finalTotalWorked"
                    Log.d("HomeViewModel", "Exit pressed at ${formatTime(exitTimeToUse)}")

                    if (finalTotalWorked != null) {
                        val entryToSave = WorkHistoryEntry(
                            id = currentUiState.enterTime.time.toString(),
                            enterTime = currentUiState.enterTime.time,
                            pauses = currentUiState.pauses.map { SerializablePausePair(it.start?.time, it.end?.time) },
                            exitTime = exitTimeToUse.time,
                            totalWorkedTime = finalTotalWorked,
                            dailyHoursTarget = currentUiState.dailyHours
                        )
                        viewModelScope.launch {
                            workHistoryRepository.addWorkHistoryEntry(entryToSave)
                            Log.d("HomeViewModel", "Work history entry saved to DataStore. ID: ${entryToSave.id}")
                            _uiState.update {
                                it.copy(
                                    enterTime = currentUiState.enterTime,
                                    pauses = currentUiState.pauses,
                                    exitTime = exitTimeToUse,
                                    totalWorkedTime = finalTotalWorked,
                                    calculatedExitTime = null,
                                    message = tempMessage
                                )
                            }
                        }
                    } else {
                        tempMessage = "Uscita registrata, ma impossibile calcolare il totale lavorato."
                        _uiState.update {
                            it.copy(
                                enterTime = currentUiState.enterTime,
                                pauses = currentUiState.pauses,
                                exitTime = exitTimeToUse,
                                message = tempMessage
                            )
                        }
                    }
                    recalculateAndUpdateUi() 
                    return 
                }
            }
            else -> {
                 // ToLunch/FromLunch direct presses are handled by specific functions
            }
        }
        if (buttonType != ButtonType.Exit) {
            recalculateAndUpdateUi()
        }
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
                    }
                    ButtonType.Exit -> {
                         if (newEnterTime != null && selectedDate.before(newEnterTime)) {
                            tempMessage = "L'orario di uscita non può essere prima dell'ingresso."
                        } else {
                            newExitTime = selectedDate 
                            tempMessage = "Orario di uscita modificato: ${formatTime(newExitTime)}. Premi 'Uscita' per salvare."
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
        _uiState.update { it.copy(timePickerEvent = null, message = if (it.message.startsWith("Errore:")) "" else it.message) }
    }

    override fun clearMessage() {
        if (_uiState.value.message.isNotEmpty() && !_uiState.value.message.startsWith("NOTIFY_")) {
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
            val newPauses = current.pauses.toMutableList().apply { add(PausePair()) }
            current.copy(pauses = newPauses, message = "Nuova pausa aggiunta. Premi 'In Pausa'.")
        }
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
        recalculateAndUpdateUi()
    }

    override fun onCleared() {
        super.onCleared()
        notificationPollingJob?.cancel()
        Log.d("HomeViewModel", "onCleared called, notification polling job cancelled.")
    }
}
