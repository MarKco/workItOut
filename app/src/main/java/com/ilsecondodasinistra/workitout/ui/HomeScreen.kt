package com.ilsecondodasinistra.workitout.ui

import IHomeViewModel
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ilsecondodasinistra.workitout.NOTIFICATION_CHANNEL_ID
import com.ilsecondodasinistra.workitout.NOTIFICATION_ID
import com.ilsecondodasinistra.workitout.ui.theme.WorkItOutM3Theme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    homeViewModel: IHomeViewModel)
{
    val uiState by homeViewModel.uiState.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val timePickerState = rememberTimePickerState(is24Hour = true)
    var showTimePickerDialog by remember { mutableStateOf(false) }

    // Observe lifecycle events to call ViewModel's onResume
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            // We only need to tell the ViewModel about the resume event,
            // as it implements DefaultLifecycleObserver and will call its own onResume.
            // However, to be explicit or if not using DefaultLifecycleObserver directly on VM:
            // if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
            //     homeViewModel.someSpecificOnResumeFunction() // if you had one
            // }
        }
        lifecycleOwner.lifecycle.addObserver(homeViewModel as LifecycleObserver) // Add ViewModel as an observer

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(homeViewModel as LifecycleObserver) // Clean up
        }
    }

    // Handle showing TimePickerDialog based on ViewModel event
    LaunchedEffect(uiState.timePickerEvent) {
        uiState.timePickerEvent?.let { event ->
            timePickerState.hour = event.initialHour
            timePickerState.minute = event.initialMinute
            // The dialog visibility will be controlled by a separate state below
        }
    }


    LaunchedEffect(uiState.timePickerEvent) {
        showTimePickerDialog = uiState.timePickerEvent != null
    }


    // Handle messages (Toast and Notifications)
    LaunchedEffect(uiState.message) {
        if (uiState.message.isNotEmpty()) {
            if (uiState.message.startsWith("NOTIFY_EXIT_TIME:")) {
                val notificationMessage = uiState.message.substringAfter("NOTIFY_EXIT_TIME:")
                showLocalNotification(context, "Ora di uscire!", "Il tuo orario di uscita previsto è $notificationMessage")
                homeViewModel.clearMessage() // Clear notification trigger message
            } else {
                Toast.makeText(context, uiState.message, Toast.LENGTH_LONG).show()
                homeViewModel.clearMessage() // Clear regular message after showing
            }
        }
    }


// Apply your M3 Theme
    WorkItOutM3Theme {
        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp), // Overall horizontal padding
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth() // Buttons will take full width with padding
                        .weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically) // Spacing and centering
                ) {
                    TimeButtonM3(
                        buttonType = ButtonType.Enter,
                        time = homeViewModel.formatTimeToDisplay(uiState.enterTime),
                        onClick = { homeViewModel.handleTimeButtonPress(ButtonType.Enter) },
                        onEditClick = { homeViewModel.handleTimeEditRequest(ButtonType.Enter) },
                        enabled = true,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                    TimeButtonM3(
                        buttonType = ButtonType.ToLunch,
                        time = homeViewModel.formatTimeToDisplay(uiState.toLunchTime),
                        onClick = { homeViewModel.handleTimeButtonPress(ButtonType.ToLunch) },
                        onEditClick = { homeViewModel.handleTimeEditRequest(ButtonType.ToLunch) },
                        enabled = uiState.enterTime != null,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer, // Use tonal for secondary actions
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    )
                    TimeButtonM3(
                        buttonType = ButtonType.FromLunch,
                        time = homeViewModel.formatTimeToDisplay(uiState.fromLunchTime),
                        onClick = { homeViewModel.handleTimeButtonPress(ButtonType.FromLunch) },
                        onEditClick = { homeViewModel.handleTimeEditRequest(ButtonType.FromLunch) },
                        enabled = uiState.toLunchTime != null,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    )
                    TimeButtonM3(
                        buttonType = ButtonType.Exit,
                        time = homeViewModel.formatTimeToDisplay(uiState.exitTime),
                        onClick = { homeViewModel.handleTimeButtonPress(ButtonType.Exit) },
                        onEditClick = { homeViewModel.handleTimeEditRequest(ButtonType.Exit) },
                        enabled = uiState.enterTime != null && ((uiState.toLunchTime != null && uiState.fromLunchTime != null) || uiState.toLunchTime == null),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.tertiary, // Or error for strong exit
                            contentColor = MaterialTheme.colorScheme.onTertiary
                        )
                    )
                }

                // Summary Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    shape = MaterialTheme.shapes.large, // Use M3 shape
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant, // Slightly different surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.Start // Align text to start for better readability
                    ) {
                        SummaryTextRow("Ore giornaliere:", "${"%.1f".format(uiState.dailyHours)}h")
                        SummaryTextRow("Uscita stimata:", homeViewModel.formatTimeToDisplay(uiState.calculatedExitTime))
                        SummaryTextRow("Totale oggi:", uiState.totalWorkedTime ?: "N/A", isBold = true)
                    }
                }
            }

            if (showTimePickerDialog && uiState.timePickerEvent != null) {
                val currentDialogEvent = uiState.timePickerEvent
                TimePickerDialog(
                    onDismiss = { homeViewModel.onDialogDismissed() },
                    onConfirm = {
                        homeViewModel.onTimeEdited(
                            buttonType = currentDialogEvent!!.type,
                            hour = timePickerState.hour,
                            minute = timePickerState.minute
                        )
                    }
                ) {
                    TimePicker(
                        state = timePickerState,
                        colors = TimePickerDefaults.colors( // Customize picker colors
                            clockDialColor = MaterialTheme.colorScheme.surfaceVariant,
                            periodSelectorBorderColor = MaterialTheme.colorScheme.primary,
                            selectorColor = MaterialTheme.colorScheme.primary
                            // ... explore other color options
                        )
                    )
                }
            }
        }
    }
}

// Reusable Composable for Summary Text Rows
@Composable
fun SummaryTextRow(label: String, value: String, isBold: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 4.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
            textAlign = TextAlign.End
        )
    }
}


// Updated TimeButton for M3 style
@Composable
fun TimeButtonM3(
    buttonType: ButtonType,
    time: String,
    onClick: () -> Unit,
    onEditClick: () -> Unit,
    enabled: Boolean = true,
    colors: ButtonColors = ButtonDefaults.elevatedButtonColors()
) {
    ElevatedButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth(0.9f)
            .height(72.dp),
        shape = MaterialTheme.shapes.medium,
        colors = colors, // Pass the whole ButtonColors object
        elevation = ButtonDefaults.elevatedButtonElevation(defaultElevation = 4.dp, pressedElevation = 2.dp),
        enabled = enabled
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(horizontalAlignment = Alignment.Start) {
                // The content color for this Text will be resolved automatically by the Button
                Text(buttonType.text, style = MaterialTheme.typography.titleMedium)
                if (time != "N/A") {
                    Text(
                        time,
                        style = MaterialTheme.typography.bodySmall,
                        // The Button's contentColor will apply, but we can make it slightly transparent
                        // For a more subtle effect, it's better to rely on the theme's onSurface.copy(alpha=...)
                        // or ensure the provided `colors` in ButtonColors already has the desired alpha.
                        // For simplicity here, we'll let the button's default content color apply,
                        // or ensure the `ButtonColors` passed in has appropriate alpha.
                        // If you need explicit alpha control for sub-elements:
                        // color = LocalContentColor.current.copy(alpha = if (enabled) 0.8f else ContentAlpha.disabled)
                        // However, ButtonColors should handle the disabled state correctly.
                    )
                }
            }
            if (time != "N/A") {
                IconButton(onClick = onEditClick, enabled = enabled) {
                    Icon(
                        Icons.Outlined.Edit,
                        contentDescription = "Modifica ${buttonType.text}",
                        // Tint for Icon in IconButton should also use LocalContentColor or be explicitly set
                        // The IconButton itself will also provide a content color.
                        // If you want it to match the button's text, rely on what IconButton provides
                        // or explicitly use a themed color like MaterialTheme.colorScheme.onPrimary.copy(alpha=0.7f)
                        // For consistency with the button's behavior, let the theme resolve it.
                        // If the passed `colors: ButtonColors` defines a specific contentColor, that will be used by default.
                    )
                }
            }
        }
    }
}

// Helper function for showing local notifications (can be moved to a utility file)
private fun showLocalNotification(context: Context, title: String, body: String) {
    if (ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
    ) {
        val builder = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        with(NotificationManagerCompat.from(context)) {
            notify(NOTIFICATION_ID, builder.build())
        }
    } else {
        Log.w("Notification", "POST_NOTIFICATIONS permission not granted. Requesting...")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            (context as? androidx.activity.ComponentActivity)?.let { activity ->
                ActivityCompat.requestPermissions(
                    activity,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    0, // Define a request code if you need to handle the result
                )
            }
        }
        // Fallback: Show toast if permission cannot be requested or is denied.
        Toast.makeText(context, "$title: $body (Notification permission needed)", Toast.LENGTH_LONG).show()

    }
}

@Preview(showBackground = true, name = "HomeScreen Logged In")
@Composable
fun HomeScreenPreview_LoggedIn() {
    WorkItOutM3Theme {
        // If HomeScreen takes the ViewModel interface:
        HomeScreen(homeViewModel = PreviewHomeViewModel())

        // If HomeScreen directly takes HomeUiState and lambdas (alternative structure):
        // val previewState = getPreviewHomeUiState()
        // HomeScreen(
        //     uiState = previewState,
        //     onHandleTimeButtonPress = { /* no-op */ },
        //     onHandleTimeEditRequest = { /* no-op */ },
        //     onTimeEdited = { _, _, _ -> /* no-op */ },
        //     onDialogDismissed = { /* no-op */ },
        //     formatTimeToDisplay = { date -> date?.toString() ?: "N/A" }
        // )
    }
}

@Preview(showBackground = true, name = "HomeScreen Initial")
@Composable
fun HomeScreenPreview_Initial() {
    WorkItOutM3Theme {
        val initialVm = PreviewHomeViewModel().apply {
            // If PreviewHomeViewModel has a way to update its internal state directly:
            // (this.asMutableStateFlow()).value = getPreviewHomeUiState(enterTime = null, toLunchTime = null, totalWorkedTime = null, calculatedExitTime = null)
            // Or, more simply, create another preview state function:
            (this._uiState).value = getPreviewHomeUiState(
                enterTime = null,
                toLunchTime = null,
                fromLunchTime = null,
                exitTime = null,
                calculatedExitTime = null, // Or some initial calculation
                totalWorkedTime = null
            )
        }
        HomeScreen(homeViewModel = initialVm)
    }
}
