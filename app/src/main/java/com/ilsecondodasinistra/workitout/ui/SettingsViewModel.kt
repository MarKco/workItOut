package com.ilsecondodasinistra.workitout.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ilsecondodasinistra.workitout.R
import com.ilsecondodasinistra.workitout.data.HistoryRepository
import com.ilsecondodasinistra.workitout.data.WorkHistoryEntry
import com.ilsecondodasinistra.workitout.domain.HistoryTextExporter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.DecimalFormat

data class SettingsUiState(
    val dailyHoursInput: String = "8",
    val history: List<WorkHistoryEntry> = emptyList(),
    val message: UiMessage? = null,
    val showClearConfirmDialog: Boolean = false,
    val shareText: String? = null,
)

class SettingsViewModel(
    private val repository: HistoryRepository,
    private val strings: StringProvider,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val hoursInputFormat = DecimalFormat("#.##")
    private val validHoursInput = Regex("^\\d*\\.?\\d{0,2}$")

    init {
        viewModelScope.launch {
            repository.dailyHours.collect { hours ->
                _uiState.update { it.copy(dailyHoursInput = hoursInputFormat.format(hours)) }
            }
        }
        viewModelScope.launch {
            repository.history.collect { history -> _uiState.update { it.copy(history = history) } }
        }
    }

    fun onDailyHoursInputChange(input: String) {
        if (validHoursInput.matches(input)) _uiState.update { it.copy(dailyHoursInput = input) }
    }

    fun saveDailyHours() {
        val hours = _uiState.value.dailyHoursInput.toDoubleOrNull()
        if (hours == null || hours < 0 || hours > 24) {
            _uiState.update { it.copy(message = UiMessage(R.string.error_invalid_hours_value)) }
            return
        }
        viewModelScope.launch {
            repository.setDailyHours(hours)
            _uiState.update { it.copy(message = UiMessage(R.string.daily_hours_updated)) }
        }
    }

    fun requestClearHistory() = _uiState.update { it.copy(showClearConfirmDialog = true) }

    fun cancelClearHistory() = _uiState.update { it.copy(showClearConfirmDialog = false) }

    fun confirmClearHistory() {
        viewModelScope.launch {
            repository.clearHistory()
            _uiState.update {
                it.copy(message = UiMessage(R.string.history_cleared_successfully), showClearConfirmDialog = false)
            }
        }
    }

    fun shareHistory() {
        val history = _uiState.value.history
        if (history.isEmpty()) {
            _uiState.update { it.copy(message = UiMessage(R.string.no_history_to_share)) }
            return
        }
        val text = HistoryTextExporter.export(history, strings)
        _uiState.update { it.copy(shareText = text) }
    }

    fun onShareLaunched() = _uiState.update { it.copy(shareText = null) }

    fun messageShown() = _uiState.update { it.copy(message = null) }
}
