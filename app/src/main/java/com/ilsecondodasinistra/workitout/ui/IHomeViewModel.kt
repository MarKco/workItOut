import com.ilsecondodasinistra.workitout.ui.ButtonType
import com.ilsecondodasinistra.workitout.ui.HomeUiState
import kotlinx.coroutines.flow.StateFlow
import java.util.Date

// In HomeViewModel.kt or a new file HomeViewModelInterface.kt
interface IHomeViewModel {
    val uiState: StateFlow<HomeUiState>
    fun handleTimeButtonPress(buttonType: ButtonType)
    fun handleTimeEditRequest(buttonType: ButtonType)
    fun onTimeEdited(buttonType: ButtonType, hour: Int, minute: Int)
    fun onDialogDismissed()
    fun clearMessage()
    fun formatTimeToDisplay(date: Date?): String
    fun handlePauseStart(index: Int)
    fun handlePauseEnd(index: Int)
    fun handlePauseEditStart(index: Int)
    fun handlePauseEditEnd(index: Int)
    fun handleAddPause()
    // Add any other public methods/properties your UI needs
}

// Your actual HomeViewModel would then implement this:
// class HomeViewModel(application: Application) : AndroidViewModel(application), IHomeViewModel { ... }