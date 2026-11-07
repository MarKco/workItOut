package com.ilsecondodasinistra.workitout.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ilsecondodasinistra.workitout.R
import com.ilsecondodasinistra.workitout.data.HistoryRepository
import com.ilsecondodasinistra.workitout.domain.DraftError
import com.ilsecondodasinistra.workitout.domain.DraftResult
import com.ilsecondodasinistra.workitout.domain.EntryDraft
import com.ilsecondodasinistra.workitout.domain.PauseDraft
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.Clock
import java.time.LocalDate
import java.time.LocalTime
import java.util.Locale

data class EditEntryUiState(
    val loading: Boolean = true,
    val isNew: Boolean = true,
    val draft: EntryDraft? = null,
    val targetInput: String = "",
    val message: UiMessage? = null,
    val showDeleteConfirm: Boolean = false,
    /** Set once saved or deleted: the screen should close. */
    val finished: Boolean = false,
)

/** Edits one history entry ([entryId]) or creates a new one when [entryId] is null. */
class EditEntryViewModel(
    private val repository: HistoryRepository,
    private val entryId: String?,
    private val clock: Clock = Clock.systemDefaultZone(),
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditEntryUiState())
    val uiState: StateFlow<EditEntryUiState> = _uiState.asStateFlow()

    private val targetFormat = DecimalFormat("#.##", DecimalFormatSymbols(Locale.ROOT)) // always "7.5": the input only accepts a dot
    private val validTargetInput = Regex("^\\d*\\.?\\d{0,2}$")

    init {
        viewModelScope.launch {
            val existing = entryId?.let { id -> repository.history.first().firstOrNull { it.id == id } }
            val draft = if (existing != null) {
                EntryDraft.fromEntry(existing, clock.zone)
            } else {
                EntryDraft(
                    date = LocalDate.now(clock),
                    enter = LocalTime.of(9, 0),
                    exit = LocalTime.of(18, 0),
                    pauses = emptyList(),
                    targetHours = repository.dailyHours.first(),
                )
            }
            _uiState.value = EditEntryUiState(
                loading = false,
                isNew = existing == null,
                draft = draft,
                targetInput = targetFormat.format(draft.targetHours),
            )
        }
    }

    fun setDate(date: LocalDate) = edit { it.copy(date = date) }
    fun setEnter(time: LocalTime) = edit { it.copy(enter = time) }
    fun setExit(time: LocalTime) = edit { it.copy(exit = time) }
    fun addPause() = edit { it.copy(pauses = it.pauses + PauseDraft()) }
    fun removePause(index: Int) = edit { it.copy(pauses = it.pauses.filterIndexed { i, _ -> i != index }) }
    fun setPauseStart(index: Int, time: LocalTime) = editPause(index) { it.copy(start = time) }
    fun setPauseEnd(index: Int, time: LocalTime) = editPause(index) { it.copy(end = time) }

    fun onTargetInputChange(input: String) {
        if (validTargetInput.matches(input)) _uiState.update { it.copy(targetInput = input) }
    }

    fun save() {
        val state = _uiState.value
        val draft = state.draft ?: return
        val target = state.targetInput.toDoubleOrNull() ?: return showError(DraftError.InvalidTarget)

        viewModelScope.launch {
            val history = repository.history.first()
            val id = entryId ?: uniqueId(draft, history.map { it.id }.toSet())
            when (val result = draft.copy(targetHours = target).toEntry(id, clock.zone)) {
                is DraftResult.Invalid -> showError(result.error)
                is DraftResult.Valid -> {
                    val updated = (history.filterNot { it.id == id } + result.entry).sortedByDescending { it.enterTime }
                    repository.replaceHistory(updated)
                    _uiState.update { it.copy(finished = true) }
                }
            }
        }
    }

    fun requestDelete() = _uiState.update { it.copy(showDeleteConfirm = true) }

    fun cancelDelete() = _uiState.update { it.copy(showDeleteConfirm = false) }

    fun confirmDelete() {
        val id = entryId ?: return
        viewModelScope.launch {
            repository.replaceHistory(repository.history.first().filterNot { it.id == id })
            _uiState.update { it.copy(showDeleteConfirm = false, finished = true) }
        }
    }

    fun messageShown() = _uiState.update { it.copy(message = null) }

    private fun edit(change: (EntryDraft) -> EntryDraft) =
        _uiState.update { state -> state.draft?.let { state.copy(draft = change(it)) } ?: state }

    private fun editPause(index: Int, change: (PauseDraft) -> PauseDraft) =
        edit { draft -> draft.copy(pauses = draft.pauses.mapIndexed { i, p -> if (i == index) change(p) else p }) }

    /** New entries use the entry time as id; add a suffix if another day already has it. */
    private fun uniqueId(draft: EntryDraft, taken: Set<String>): String {
        val base = (draft.toEntry(null, clock.zone) as? DraftResult.Valid)?.entry?.id ?: "manual"
        if (base !in taken) return base
        return generateSequence(2) { it + 1 }.map { "$base-$it" }.first { it !in taken }
    }

    private fun showError(error: DraftError) {
        val resId = when (error) {
            DraftError.PauseIncomplete -> R.string.error_draft_pause_incomplete
            DraftError.PauseOutsideDay -> R.string.error_draft_pause_outside
            DraftError.ExitNotAfterEnter -> R.string.error_draft_exit_not_after_enter
            DraftError.InvalidTarget -> R.string.error_draft_invalid_target
        }
        _uiState.update { it.copy(message = UiMessage(resId)) }
    }
}
