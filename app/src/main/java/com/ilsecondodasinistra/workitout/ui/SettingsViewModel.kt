package com.ilsecondodasinistra.workitout.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Constants for SharedPreferences (ensure these are consistent)
private const val PREFS_NAME = "workitout_settings_prefs"
private const val KEY_DAILY_HOURS = "daily_hours"

data class SettingsUiState(
    val dailyHoursInputString: String = "8.0", // String representation for TextField
    val history: List<Map<String, Any?>> = emptyList(),
    val message: String = "",
    val showClearConfirmDialog: Boolean = false,
    val shareIntentEvent: ShareIntentEvent? = null
)

data class ShareIntentEvent(val textToShare: String)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val sharedPreferences: SharedPreferences =
        application.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    // Helper to format Double to String for display, avoiding ".0" for whole numbers
    private val decimalFormat = DecimalFormat("#.##") // Max 2 decimal places

    init {
        loadDailyHours()
        loadHistory()
    }

    private fun loadDailyHours() {
        val loadedHoursDouble = sharedPreferences.getFloat(KEY_DAILY_HOURS, 8.0f).toDouble()
        _uiState.update { it.copy(dailyHoursInputString = formatDoubleForInput(loadedHoursDouble)) }
        Log.d("SettingsViewModel", "Loaded daily hours: $loadedHoursDouble, string: ${_uiState.value.dailyHoursInputString}")
    }

    fun loadHistory() {
        viewModelScope.launch {
            val currentHistory = LocalHistoryRepository.history
            _uiState.update { it.copy(history = currentHistory) }
            Log.d("SettingsViewModel", "History loaded with ${currentHistory.size} items.")
        }
    }

    fun onDailyHoursInputChange(newInput: String) {
        // Allow empty string, numbers, and at most one decimal point
        val filteredInput = if (newInput.isEmpty() || newInput == "." || newInput.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
            newInput
        } else {
            _uiState.value.dailyHoursInputString // Revert to previous valid string if input is invalid
        }
        _uiState.update { it.copy(dailyHoursInputString = filteredInput) }
    }

    fun saveDailyHours() {
        viewModelScope.launch {
            val hoursToSave = _uiState.value.dailyHoursInputString.toDoubleOrNull()
            if (hoursToSave == null || hoursToSave < 0 || hoursToSave > 24) { // Basic validation
                _uiState.update { it.copy(message = "Valore ore non valido (0-24).") }
                Log.w("SettingsViewModel", "Invalid daily hours input for saving: ${_uiState.value.dailyHoursInputString}")
                return@launch
            }

            try {
                with(sharedPreferences.edit()) {
                    putFloat(KEY_DAILY_HOURS, hoursToSave.toFloat())
                    apply()
                }
                // Update the input string to the formatted version of what was saved
                // This handles cases like "8." -> "8" or "8.50" -> "8.5"
                _uiState.update {
                    it.copy(
                        message = "Ore giornaliere aggiornate!",
                        dailyHoursInputString = formatDoubleForInput(hoursToSave)
                    )
                }
                Log.d("SettingsViewModel", "Daily hours saved: $hoursToSave")
            } catch (e: Exception) {
                Log.e("SettingsViewModel", "Error updating daily hours: ${e.message}", e)
                _uiState.update { it.copy(message = "Errore durante l'aggiornamento.") }
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
                LocalHistoryRepository.clearHistory()
                _uiState.update {
                    it.copy(
                        history = LocalHistoryRepository.history, // Refresh
                        message = "Storico ripulito con successo!",
                        showClearConfirmDialog = false
                    )
                }
                Log.d("SettingsViewModel", "History cleared.")
            } catch (e: Exception) {
                Log.e("SettingsViewModel", "Error clearing history: ${e.message}", e)
                _uiState.update {
                    it.copy(
                        message = "Errore durante la pulizia dello storico.",
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
            _uiState.update { it.copy(message = "Nessuno storico da condividere!") }
            return
        }

        val shareTextBuilder = StringBuilder("Storico ore lavorate:\n\n")
        currentHistory.forEach { record ->
            shareTextBuilder.append("Data: ${record["id"]}\n") // 'id' is date string from LocalHistoryRepo
            shareTextBuilder.append("  Ingresso: ${formatLongTimestampToDisplay(record["enterTime"] as? Long)}\n")
            shareTextBuilder.append("  In pausa: ${formatLongTimestampToDisplay(record["toLunchTime"] as? Long)}\n")
            shareTextBuilder.append("  Fine pausa: ${formatLongTimestampToDisplay(record["fromLunchTime"] as? Long)}\n")
            shareTextBuilder.append("  Uscita: ${formatLongTimestampToDisplay(record["exitTime"] as? Long)}\n")
            shareTextBuilder.append("  Totale lavorato: ${record["totalWorkedTime"] ?: "N/A"}\n")
            shareTextBuilder.append("  Ore giornaliere impostate: ${record["dailyHours"] ?: "N/A"}h\n\n")
        }

        Log.d("SettingsViewModel", "Sharing history content: ${shareTextBuilder.toString()}")
        _uiState.update {
            it.copy(
                shareIntentEvent = ShareIntentEvent(shareTextBuilder.toString()),
                message = "Lo storico è pronto per essere condiviso!" // Optional message
            )
        }
    }

    fun onShareIntentLaunched() {
        _uiState.update { it.copy(shareIntentEvent = null) } // Reset event
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = "") }
    }

    // Helper for display in UI, as Composable shouldn't know about Date/SimpleDateFormat directly
    fun formatLongTimestampToDisplay(timestamp: Long?): String {
        return timestamp?.let { SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(it)) } ?: "N/A"
    }
}