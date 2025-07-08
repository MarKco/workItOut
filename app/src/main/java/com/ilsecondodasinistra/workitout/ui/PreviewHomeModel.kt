// In your test or debug source set (e.g., src/debug/java/com/yourpackage/ui/previews)
// Or directly in your HomeScreen.kt file for simplicity if it's small,
// but separate files are cleaner for larger projects.

package com.ilsecondodasinistra.workitout.ui // Or a sub-package like previews

import IHomeViewModel
// import androidx.compose.runtime.Composable // Not used directly in this file for @Composable
// import androidx.compose.ui.tooling.preview.Preview // Not used directly for @Preview
// import com.ilsecondodasinistra.workitout.ui.theme.WorkItOutM3Theme // Not used directly
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.Calendar
import java.util.Date
// Make sure PausePair is imported or defined if not in the same package as HomeUiState and TimePickerEvent
// Assuming HomeUiState, PausePair, TimePickerEvent, ButtonType are accessible from this package
// If PausePair is defined in com.ilsecondodasinistra.workitout.ui (like in HomeViewModel.kt),
// it should be directly accessible.

// Fake HomeUiState for preview
fun getPreviewHomeUiState(
    enterTime: Date? = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, 9); set(Calendar.MINUTE, 0) }.time,
    pauses: List<PausePair> = listOf(
        // Example: One pause from 13:00 to 14:00
        PausePair(
            start = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, 13); set(Calendar.MINUTE, 0) }.time,
            end = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, 14); set(Calendar.MINUTE, 0) }.time
        )
    ),
    // toLunchTime and fromLunchTime are now part of the pauses list
    exitTime: Date? = null,
    calculatedExitTime: Date? = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, 18); set(Calendar.MINUTE, 0) }.time,
    totalWorkedTime: String? = "3h 0m", // Adjusted for the example pause
    dailyHours: Double = 8.0,
    message: String = "",
    timePickerEvent: TimePickerEvent? = null
): HomeUiState {
    return HomeUiState(
        enterTime = enterTime,
        pauses = pauses, // Pass the pauses list
        exitTime = exitTime,
        calculatedExitTime = calculatedExitTime,
        totalWorkedTime = totalWorkedTime,
        dailyHours = dailyHours,
        message = message,
        timePickerEvent = timePickerEvent
    )
}


// Fake ViewModel implementing the interface
class PreviewHomeViewModel : IHomeViewModel {
    // Initialize with a state that includes pauses
    private val _initialPreviewState = getPreviewHomeUiState(
        pauses = listOf(
            PausePair(
                start = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, 13); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0) }.time,
                end = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, 13); set(Calendar.MINUTE, 30); set(Calendar.SECOND, 0) }.time
            )
        ),
        totalWorkedTime = "3h 30m" // Example based on 9-18 with 30min pause
    )
    val _uiState = MutableStateFlow(_initialPreviewState)
    override val uiState: StateFlow<HomeUiState> = _uiState

    override fun handleTimeButtonPress(buttonType: ButtonType) {
        println("Preview: handleTimeButtonPress called with $buttonType")
        // Example: Toggle enter time for preview
        if (buttonType == ButtonType.Enter) {
            if (_uiState.value.enterTime == null) {
                _uiState.value = getPreviewHomeUiState() // Reset to default with enter time
            } else {
                _uiState.value = getPreviewHomeUiState(enterTime = null, pauses = emptyList(), calculatedExitTime = null, totalWorkedTime = null)
            }
        }
    }

    override fun handleTimeEditRequest(buttonType: ButtonType) {
        println("Preview: handleTimeEditRequest called with $buttonType")
        // For non-pause edits, pauseIndex is null
        _uiState.value = _uiState.value.copy(
            timePickerEvent = TimePickerEvent(
                type = buttonType,
                pauseIndex = null, // Explicitly null for Enter/Exit edits
                initialHour = 9,   // Dummy initial hour for preview
                initialMinute = 0    // Dummy initial minute for preview
            )
        )
    }

    override fun onTimeEdited(buttonType: ButtonType, hour: Int, minute: Int) {
        println("Preview: onTimeEdited called for $buttonType with $hour:$minute")
        val pauseIndex = _uiState.value.timePickerEvent?.pauseIndex
        if (pauseIndex != null) {
            // Logic for updating a pause time in preview
            val updatedPauses = _uiState.value.pauses.toMutableList()
            if (pauseIndex >= 0 && pauseIndex < updatedPauses.size) {
                var currentPause = updatedPauses[pauseIndex]
                val cal = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, hour); set(Calendar.MINUTE, minute)}
                if (buttonType == ButtonType.ToLunch) { // Corresponds to pause start
                    currentPause = currentPause.copy(start = cal.time)
                } else if (buttonType == ButtonType.FromLunch) { // Corresponds to pause end
                    currentPause = currentPause.copy(end = cal.time)
                }
                updatedPauses[pauseIndex] = currentPause
                _uiState.value = _uiState.value.copy(
                    pauses = updatedPauses,
//                    message = "Pause ${pauseIndex + 1} ${if (buttonType == ButtonType.ToLunch) "start" else "end"} updated in preview",
                    timePickerEvent = null
                )
            } else {
//                 _uiState.value = _uiState.value.copy(message = "Preview: Invalid pause index $pauseIndex", timePickerEvent = null)
                 _uiState.value = _uiState.value.copy(timePickerEvent = null)
            }
        } else {
            // Logic for updating enter/exit time
//             _uiState.value = _uiState.value.copy(message = "Time updated for $buttonType in preview", timePickerEvent = null)
             _uiState.value = _uiState.value.copy(timePickerEvent = null)
        }
    }

    override fun onDialogDismissed() {
        println("Preview: onDialogDismissed called")
        _uiState.value = _uiState.value.copy(timePickerEvent = null)
    }

    override fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = "")
    }

    override fun formatTimeToDisplay(date: Date?): String {
        return date?.let {
            val sdf = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
            sdf.format(it)
        } ?: "N/A"
    }

    // --- Pause Management Stubs for Preview ---
    override fun handleAddPause() {
        println("Preview: handleAddPause called")
        val newPauses = _uiState.value.pauses.toMutableList().apply { add(PausePair()) }
//        _uiState.value = _uiState.value.copy(pauses = newPauses, message = "Preview: Pause added")
        _uiState.value = _uiState.value.copy(pauses = newPauses)
    }

    override fun handlePauseStart(index: Int) {
        println("Preview: handlePauseStart called for index $index")
        val updatedPauses = _uiState.value.pauses.toMutableList()
        if (index >= 0 && index < updatedPauses.size) {
            updatedPauses[index] = updatedPauses[index].copy(start = Date(), end = null) // Set start to now, clear end
//            _uiState.value = _uiState.value.copy(pauses = updatedPauses, message = "Preview: Pause $index started")
            _uiState.value = _uiState.value.copy(pauses = updatedPauses)
        }
    }

    override fun handlePauseEnd(index: Int) {
        println("Preview: handlePauseEnd called for index $index")
         val updatedPauses = _uiState.value.pauses.toMutableList()
        if (index >= 0 && index < updatedPauses.size && updatedPauses[index].start != null) {
            updatedPauses[index] = updatedPauses[index].copy(end = Date()) // Set end to now
//            _uiState.value = _uiState.value.copy(pauses = updatedPauses, message = "Preview: Pause $index ended")
            _uiState.value = _uiState.value.copy(pauses = updatedPauses)
        }
    }

    override fun handlePauseEditStart(index: Int) {
        println("Preview: handlePauseEditStart called for index $index")
        val currentPause = if (index >= 0 && index < _uiState.value.pauses.size) _uiState.value.pauses[index] else null
        val cal = Calendar.getInstance()
        currentPause?.start?.let { cal.time = it }

        _uiState.value = _uiState.value.copy(
            timePickerEvent = TimePickerEvent(
                type = ButtonType.ToLunch, // To signify editing a pause start
                pauseIndex = index,
                initialHour = cal.get(Calendar.HOUR_OF_DAY),
                initialMinute = cal.get(Calendar.MINUTE)
            )
        )
    }

    override fun handlePauseEditEnd(index: Int) {
        println("Preview: handlePauseEditEnd called for index $index")
        val currentPause = if (index >= 0 && index < _uiState.value.pauses.size) _uiState.value.pauses[index] else null
        val cal = Calendar.getInstance()
        currentPause?.end?.let { cal.time = it } ?: currentPause?.start?.let { cal.time = it}


        _uiState.value = _uiState.value.copy(
            timePickerEvent = TimePickerEvent(
                type = ButtonType.FromLunch, // To signify editing a pause end
                pauseIndex = index,
                initialHour = cal.get(Calendar.HOUR_OF_DAY),
                initialMinute = cal.get(Calendar.MINUTE)
            )
        )
    }
}
