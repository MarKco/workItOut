// In your test or debug source set (e.g., src/debug/java/com/yourpackage/ui/previews)
// Or directly in your HomeScreen.kt file for simplicity if it's small,
// but separate files are cleaner for larger projects.

package com.ilsecondodasinistra.workitout.ui // Or a sub-package like previews

import IHomeViewModel
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.ilsecondodasinistra.workitout.ui.theme.WorkItOutM3Theme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.Calendar
import java.util.Date

// Fake HomeUiState for preview
fun getPreviewHomeUiState(
    enterTime: Date? = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, 9); set(Calendar.MINUTE, 0) }.time,
    toLunchTime: Date? = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, 13); set(Calendar.MINUTE, 0) }.time,
    fromLunchTime: Date? = null,
    exitTime: Date? = null,
    calculatedExitTime: Date? = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, 18); set(Calendar.MINUTE, 0) }.time,
    totalWorkedTime: String? = "4h 0m",
    dailyHours: Double = 8.0,
    message: String = "",
    timePickerEvent: TimePickerEvent? = null
): HomeUiState {
    return HomeUiState(
        enterTime = enterTime,
        toLunchTime = toLunchTime,
        fromLunchTime = fromLunchTime,
        exitTime = exitTime,
        calculatedExitTime = calculatedExitTime,
        totalWorkedTime = totalWorkedTime,
        dailyHours = dailyHours,
        message = message,
        timePickerEvent = timePickerEvent
    )
}


// Fake ViewModel implementing the interface (if you created one)
// If not using an interface, create a class with the same public API as HomeViewModel
class PreviewHomeViewModel : IHomeViewModel { // Or just create a class with the needed members
    val _uiState = MutableStateFlow(getPreviewHomeUiState())
    override val uiState: StateFlow<HomeUiState> = _uiState

    override fun handleTimeButtonPress(buttonType: ButtonType) {
        // No-op or log for preview
        println("Preview: handleTimeButtonPress called with $buttonType")
        // Optionally, update the state to show some feedback
        // _uiState.value = _uiState.value.copy(message = "$buttonType pressed in preview")
    }

    override fun handleTimeEditRequest(buttonType: ButtonType) {
        println("Preview: handleTimeEditRequest called with $buttonType")
        _uiState.value = _uiState.value.copy(timePickerEvent = TimePickerEvent(buttonType, 0, 0))
    }

    override fun onTimeEdited(buttonType: ButtonType, hour: Int, minute: Int) {
        println("Preview: onTimeEdited called for $buttonType with $hour:$minute")
        _uiState.value = _uiState.value.copy(message = "Time updated for $buttonType", timePickerEvent = null)
    }

    override fun onDialogDismissed() {
        println("Preview: onDialogDismissed called")
        _uiState.value = _uiState.value.copy(timePickerEvent = null)
    }

    override fun clearMessage() {
    }

    override fun formatTimeToDisplay(date: Date?): String {
        return date?.let {
            val sdf = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
            sdf.format(it)
        } ?: "N/A"
    }
}