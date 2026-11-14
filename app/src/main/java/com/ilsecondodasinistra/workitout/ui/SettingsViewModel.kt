package com.ilsecondodasinistra.workitout.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ilsecondodasinistra.workitout.R
import com.ilsecondodasinistra.workitout.data.DEFAULT_WORK_DAYS
import com.ilsecondodasinistra.workitout.data.HistoryRepository
import com.ilsecondodasinistra.workitout.domain.BackupFile
import com.ilsecondodasinistra.workitout.domain.HistoryBackup
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.Clock
import java.time.DayOfWeek
import java.time.LocalDate
import java.util.Locale

data class SettingsUiState(
    val dailyHoursInput: String = "8",
    val workDays: Set<DayOfWeek> = DEFAULT_WORK_DAYS,
    val dynamicColor: Boolean = false,
    val message: UiMessage? = null,
    /** A parsed backup waiting for the user to confirm replacing the current data. */
    val pendingRestore: BackupFile? = null,
)

class SettingsViewModel(
    private val repository: HistoryRepository,
    private val clock: Clock = Clock.systemDefaultZone(),
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val hoursInputFormat = DecimalFormat("#.##", DecimalFormatSymbols(Locale.ROOT)) // always "7.5": the input only accepts a dot
    private val validHoursInput = Regex("^\\d*\\.?\\d{0,2}$")

    init {
        viewModelScope.launch {
            repository.dailyHours.collect { hours ->
                _uiState.update { it.copy(dailyHoursInput = hoursInputFormat.format(hours)) }
            }
        }
        viewModelScope.launch {
            repository.dynamicColor.collect { enabled -> _uiState.update { it.copy(dynamicColor = enabled) } }
        }
        viewModelScope.launch {
            repository.workDays.collect { days -> _uiState.update { it.copy(workDays = days) } }
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

    fun toggleWorkDay(day: DayOfWeek) {
        val days = _uiState.value.workDays.let { if (day in it) it - day else it + day }
        viewModelScope.launch { repository.setWorkDays(days) }
    }

    fun setDynamicColor(enabled: Boolean) {
        viewModelScope.launch { repository.setDynamicColor(enabled) }
    }

    // --- Backup -----------------------------------------------------------------------------

    fun backupFileName(): String = "workitout-backup-${LocalDate.now(clock)}.json"

    suspend fun createBackup(): String = HistoryBackup.encode(
        history = repository.history.first(),
        dailyHours = repository.dailyHours.first(),
        workDays = repository.workDays.first(),
        exportedAt = clock.millis(),
    )

    fun onBackupSaved() = showMessage(R.string.backup_saved)

    fun onBackupIoError() = showMessage(R.string.backup_io_error)

    /** Validates the chosen file and asks for confirmation before anything is overwritten. */
    fun onBackupRead(text: String) {
        HistoryBackup.decode(text)
            .onSuccess { file -> _uiState.update { it.copy(pendingRestore = file) } }
            .onFailure { showMessage(R.string.backup_invalid) }
    }

    fun cancelRestore() = _uiState.update { it.copy(pendingRestore = null) }

    fun confirmRestore() {
        val file = _uiState.value.pendingRestore ?: return
        _uiState.update { it.copy(pendingRestore = null) }
        viewModelScope.launch {
            repository.replaceHistory(file.history.sortedByDescending { it.enterTime })
            repository.setDailyHours(file.dailyHours)
            repository.setWorkDays(file.workDaySet())
            showMessage(R.string.backup_restored)
        }
    }

    fun messageShown() = _uiState.update { it.copy(message = null) }

    private fun showMessage(resId: Int) = _uiState.update { it.copy(message = UiMessage(resId)) }
}
