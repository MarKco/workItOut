package com.ilsecondodasinistra.workitout.ui

import android.app.Application
// import android.content.Context // No longer needed
// import android.content.SharedPreferences // No longer needed
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ilsecondodasinistra.workitout.R
import com.ilsecondodasinistra.workitout.data.DataStoreHistoryRepository
import com.ilsecondodasinistra.workitout.data.HistoryRepository // Assuming this is the interface DataStoreHistoryRepository implements
import com.ilsecondodasinistra.workitout.data.WorkHistoryEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect // For collecting the flow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// SharedPreferences constants are no longer needed
// private const val PREFS_NAME = "workitout_settings_prefs"
// private const val KEY_DAILY_HOURS = "daily_hours"

data class SettingsUiState(
    val dailyHoursInputString: String = "8.0",
    val history: List<Map<String, Any?>> = emptyList(),
    val message: String = "",
    val showClearConfirmDialog: Boolean = false,
    val shareIntentEvent: ShareIntentEvent? = null
)

data class ShareIntentEvent(val textToShare: String)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    // Removed SharedPreferences instance
    // private val sharedPreferences: SharedPreferences =
    //     application.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // Use DataStoreHistoryRepository for all persistent data
    private val workHistoryRepository: HistoryRepository = DataStoreHistoryRepository(application) // Use interface type

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val decimalFormat = DecimalFormat("#.##")

    init {
        loadDailyHours() // Will now use DataStore
        observeHistory()
    }

    private fun observeHistory() {
        viewModelScope.launch {
            workHistoryRepository.getAllHistoryFlow().collect { entryList ->
                val mapped = entryList.map { entry -> workHistoryEntryToMap(entry) }
                _uiState.update { it.copy(history = mapped) }
                Log.d("SettingsViewModel", "History loaded from DataStore with ${mapped.size} items.")
            }
        }
    }

    private fun workHistoryEntryToMap(entry: WorkHistoryEntry): Map<String, Any?> {
        val firstPause = entry.pauses.firstOrNull()
        return mapOf(
            "id" to entry.id,
            "enterTime" to entry.enterTime,
            "toLunchTime" to firstPause?.start,
            "fromLunchTime" to firstPause?.end,
            "exitTime" to entry.exitTime,
            "totalWorkedTime" to entry.totalWorkedTime,
            "dailyHours" to entry.dailyHoursTarget,
            "pauses" to entry.pauses
        )
    }

    // Updated to load from DataStoreHistoryRepository
    private fun loadDailyHours() {
        viewModelScope.launch {
            workHistoryRepository.getDailyHours().collect { loadedHoursDouble ->
                _uiState.update { it.copy(dailyHoursInputString = formatDoubleForInput(loadedHoursDouble)) }
                Log.d("SettingsViewModel", "Loaded daily hours from DataStore: $loadedHoursDouble, string: ${_uiState.value.dailyHoursInputString}")
            }
        }
    }

    fun onDailyHoursInputChange(newInput: String) {
        val filteredInput = if (newInput.isEmpty() || newInput == "." || newInput.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
            newInput
        } else {
            _uiState.value.dailyHoursInputString
        }
        _uiState.update { it.copy(dailyHoursInputString = filteredInput) }
    }

    // Updated to save to DataStoreHistoryRepository
    fun saveDailyHours() {
        viewModelScope.launch {
            val hoursToSave = _uiState.value.dailyHoursInputString.toDoubleOrNull()
            if (hoursToSave == null || hoursToSave < 0 || hoursToSave > 24) {
                _uiState.update { it.copy(message = getApplication<Application>().getString(R.string.error_invalid_hours_value)) }
                Log.w("SettingsViewModel", "Invalid daily hours input for saving: ${_uiState.value.dailyHoursInputString}")
                return@launch
            }

            try {
                workHistoryRepository.saveDailyHours(hoursToSave) // Use repository to save
                _uiState.update {
                    it.copy(
                        message = getApplication<Application>().getString(R.string.daily_hours_updated),
                        dailyHoursInputString = formatDoubleForInput(hoursToSave)
                    )
                }
                Log.d("SettingsViewModel", "Daily hours saved to DataStore: $hoursToSave")
            } catch (e: Exception) {
                Log.e("SettingsViewModel", "Error updating daily hours in DataStore: ${e.message}", e)
                _uiState.update { it.copy(message = getApplication<Application>().getString(R.string.error_during_update)) }
            }
        }
    }

    private fun formatDoubleForInput(value: Double): String {
        return decimalFormat.format(value)
    }

    fun requestClearHistory() {
        _uiState.update { it.copy(showClearConfirmDialog = true) }
    }

    fun confirmClearHistory() {
        viewModelScope.launch {
            try {
                workHistoryRepository.clearAllHistory()
                _uiState.update {
                    it.copy(
                        history = emptyList(),
                        message = getApplication<Application>().getString(R.string.history_cleared_successfully),
                        showClearConfirmDialog = false
                    )
                }
                Log.d("SettingsViewModel", "History cleared.")
            } catch (e: Exception) {
                Log.e("SettingsViewModel", "Error clearing history: ${e.message}", e)
                _uiState.update {
                    it.copy(
                        message = getApplication<Application>().getString(R.string.error_during_clear),
                        showClearConfirmDialog = false
                    )
                }
            }
        }
    }

    fun cancelClearHistory() {
        _uiState.update { it.copy(showClearConfirmDialog = false) }
    }

    fun shareHistory() {
        val currentHistory = _uiState.value.history
        if (currentHistory.isEmpty()) {
            _uiState.update { it.copy(message = getApplication<Application>().getString(R.string.no_history_to_share)) }
            return
        }

        val shareTextBuilder = StringBuilder("${getApplication<Application>().getString(R.string.work_history)}:\n\n")
        currentHistory.forEach { record ->
            // Using 'id' which now correctly comes from DataStoreHistoryRepository as a string (timestamp)
            val entryDate = try {
                SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date((record["id"] as String).toLong()))
            } catch (e: Exception) {
                record["id"] as? String ?: getApplication<Application>().getString(R.string.invalid_date) // Fallback to raw id string if conversion fails
            }
            shareTextBuilder.append("${getApplication<Application>().getString(R.string.date, entryDate)}\n")
            shareTextBuilder.append("  ${getApplication<Application>().getString(R.string.entry_time, formatLongTimestampToDisplay(record["enterTime"] as? Long))}\n")
            shareTextBuilder.append("  ${getApplication<Application>().getString(R.string.pause_start, formatLongTimestampToDisplay(record["toLunchTime"] as? Long))}\n")
            shareTextBuilder.append("  ${getApplication<Application>().getString(R.string.pause_end, formatLongTimestampToDisplay(record["fromLunchTime"] as? Long))}\n")
            shareTextBuilder.append("  ${getApplication<Application>().getString(R.string.exit_time, formatLongTimestampToDisplay(record["exitTime"] as? Long))}\n")
            shareTextBuilder.append("  ${getApplication<Application>().getString(R.string.total_worked_hours, record["totalWorkedTime"] ?: "N/A")}\n")
            shareTextBuilder.append("  ${getApplication<Application>().getString(R.string.target_hours, record["dailyHours"] ?: "N/A")}h\n\n")
        }

        Log.d("SettingsViewModel", "Sharing history content: ${shareTextBuilder.toString()}")
        _uiState.update {
            it.copy(
                shareIntentEvent = ShareIntentEvent(shareTextBuilder.toString()),
                message = getApplication<Application>().getString(R.string.history_ready_to_share)
            )
        }
    }

    fun onShareIntentLaunched() {
        _uiState.update { it.copy(shareIntentEvent = null) }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = "") }
    }

    fun formatLongTimestampToDisplay(timestamp: Long?): String {
        return timestamp?.let { SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(it)) } ?: "N/A"
    }
}
