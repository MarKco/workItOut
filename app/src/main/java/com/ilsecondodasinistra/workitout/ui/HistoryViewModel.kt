package com.ilsecondodasinistra.workitout.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ilsecondodasinistra.workitout.R
import com.ilsecondodasinistra.workitout.data.HistoryRepository
import com.ilsecondodasinistra.workitout.data.WorkHistoryEntry
import com.ilsecondodasinistra.workitout.domain.HistoryCsv
import com.ilsecondodasinistra.workitout.domain.HistoryTextExporter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HistoryUiState(
    val history: List<WorkHistoryEntry> = emptyList(),
    val message: UiMessage? = null,
    val showClearConfirmDialog: Boolean = false,
    /** One-shot: text for the share sheet. */
    val shareText: String? = null,
    /** One-shot: CSV content to be written to a file and shared. */
    val shareCsv: String? = null,
)

class HistoryViewModel(
    private val repository: HistoryRepository,
    private val strings: StringProvider,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.history.collect { history -> _uiState.update { it.copy(history = history) } }
        }
    }

    fun shareText() {
        val history = _uiState.value.history
        if (history.isEmpty()) return showMessage(R.string.no_history_to_share)
        _uiState.update { it.copy(shareText = HistoryTextExporter.export(history, strings)) }
    }

    fun shareCsv() {
        val history = _uiState.value.history
        if (history.isEmpty()) return showMessage(R.string.no_history_to_share)
        _uiState.update { it.copy(shareCsv = HistoryCsv.export(history)) }
    }

    fun onShareLaunched() = _uiState.update { it.copy(shareText = null, shareCsv = null) }

    fun showShareError() = showMessage(R.string.error_cannot_start_sharing)

    fun requestClear() = _uiState.update { it.copy(showClearConfirmDialog = true) }

    fun cancelClear() = _uiState.update { it.copy(showClearConfirmDialog = false) }

    fun confirmClear() {
        _uiState.update { it.copy(showClearConfirmDialog = false) }
        viewModelScope.launch {
            repository.clearHistory()
            showMessage(R.string.history_cleared_successfully)
        }
    }

    fun messageShown() = _uiState.update { it.copy(message = null) }

    private fun showMessage(resId: Int) = _uiState.update { it.copy(message = UiMessage(resId)) }
}
