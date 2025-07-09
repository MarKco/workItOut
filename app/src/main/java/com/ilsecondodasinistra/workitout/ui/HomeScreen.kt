package com.ilsecondodasinistra.workitout.ui

//import androidx.lifecycle.viewmodel.compose.viewModel // Already imported via IHomeViewModel
// Add this import
import IHomeViewModel
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleObserver
import com.ilsecondodasinistra.workitout.R
import com.ilsecondodasinistra.workitout.ui.theme.WorkItOutM3Theme
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    homeViewModel: IHomeViewModel = androidx.lifecycle.viewmodel.compose.viewModel<HomeViewModel>(),
    resetRequested: Boolean = false,
    onResetHandled: () -> Unit = {}
) {
    val uiState by homeViewModel.uiState.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val timePickerState = rememberTimePickerState(is24Hour = true)
    var showTimePickerDialog by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Get string resources in Composable context
    val notificationTitle = stringResource(R.string.notification_exit_time_title)
    val notificationBodyTemplate = stringResource(R.string.notification_exit_time_body)

    // Observe lifecycle events to call ViewModel's onResume
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            // We only need to tell the ViewModel about the resume event,
            // as it implements DefaultLifecycleObserver and will call its own onResume.
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
        }
    }

    LaunchedEffect(uiState.timePickerEvent) {
        showTimePickerDialog = uiState.timePickerEvent != null
    }

    // Handle messages (Snackbar)
    LaunchedEffect(uiState.message) {
        if (uiState.message.isNotEmpty()) {
            scope.launch {
                snackbarHostState.showSnackbar(
                    message = uiState.message,
                    duration = SnackbarDuration.Short
                )
            }
            homeViewModel.clearMessage() // Clear regular message after showing
        }
    }

    // Handle reset request from parent
    LaunchedEffect(resetRequested) {
        if (resetRequested) {
            (homeViewModel as? HomeViewModel)?.resetAll(context)
            onResetHandled()
        }
    }

    val messageForAlarmPermission = stringResource(R.string.alarm_permission_message)
    val toastText = stringResource(R.string.notification_exit_time_body, homeViewModel.formatTimeToDisplay(uiState.calculatedExitTime))

    // Schedule alarm when calculatedExitTime changes and is valid
    LaunchedEffect(uiState.calculatedExitTime) {
        val calculatedExitTime = uiState.calculatedExitTime
        if (calculatedExitTime != null && calculatedExitTime.time > System.currentTimeMillis()) {
            Toast.makeText(
                context,
                toastText,
                Toast.LENGTH_SHORT
            ).show()

            // Check for exact alarm permission (Android 12+)
            val hasExactAlarmPermission = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                val alarmManager = context.getSystemService(android.content.Context.ALARM_SERVICE) as android.app.AlarmManager
                alarmManager.canScheduleExactAlarms()
            } else {
                true
            }
            if (hasExactAlarmPermission) {
                (homeViewModel as? HomeViewModel)?.scheduleExitAlarmIfNeeded(
                    context,
                    notificationTitle,
                    notificationBodyTemplate.format(homeViewModel.formatTimeToDisplay(calculatedExitTime))
                )
            } else {
                Toast.makeText(
                    context,
                    messageForAlarmPermission,
                    Toast.LENGTH_LONG
                ).show()
                // Redirect to exact alarm permission settings
                val intent = android.content.Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                context.startActivity(intent)
            }
        }
    }

    // Apply your M3 Theme
    WorkItOutM3Theme {
        Scaffold(
            // Remove the topBar here to avoid duplicate toolbars
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Scrollable area for buttons and pauses
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .align(Alignment.TopCenter)
                        .padding(bottom = 180.dp), // Leave space for the summary card
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    TimeButtonM3(
                        modifier = Modifier.padding(top = 4.dp, bottom = 16.dp),
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
                    // --- Multiple Pause Pairs UI ---
                    uiState.pauses.forEachIndexed { idx, pause ->
                        PausePairRow(
                            index = idx,
                            pause = pause,
                            enabled = uiState.enterTime != null,
                            onToLunch = { homeViewModel.handlePauseStart(idx) },
                            onFromLunch = { homeViewModel.handlePauseEnd(idx) },
                            onEditToLunch = { homeViewModel.handlePauseEditStart(idx) },
                            onEditFromLunch = { homeViewModel.handlePauseEditEnd(idx) }
                        )
                    }
                    ElevatedButton(
                        onClick = { homeViewModel.handleAddPause() },
                        enabled = uiState.enterTime != null && (uiState.pauses.isEmpty() || uiState.pauses.last().end != null),
                        shape = MaterialTheme.shapes.medium,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary,
                            contentColor = MaterialTheme.colorScheme.onSecondary
                        ),
                        modifier = Modifier
                            .padding(vertical = 8.dp)
                            .fillMaxWidth(0.9f)
                            .height(48.dp)
                    ) {
                        Icon(Icons.Outlined.Add, contentDescription = stringResource(R.string.add_pause))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.add_pause))
                    }
                    // --- End Multiple Pause Pairs UI ---
                    TimeButtonM3(
                        modifier = Modifier.padding(vertical = 24.dp),
                        buttonType = ButtonType.Exit,
                        time = homeViewModel.formatTimeToDisplay(uiState.exitTime),
                        onClick = { homeViewModel.handleTimeButtonPress(ButtonType.Exit) },
                        onEditClick = { homeViewModel.handleTimeEditRequest(ButtonType.Exit) },
                        enabled = uiState.enterTime != null,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.tertiary,
                            contentColor = MaterialTheme.colorScheme.onTertiary
                        )
                    )
                }
                // Fixed summary card at the bottom
                Box(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.background)
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 32.dp, top = 16.dp),
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth(),
                            horizontalAlignment = Alignment.Start
                        ) {
                            SummaryTextRow(stringResource(R.string.daily_hours), "${"%.1f".format(uiState.dailyHours)}h")
                            SummaryTextRow(stringResource(R.string.estimated_exit), homeViewModel.formatTimeToDisplay(uiState.calculatedExitTime))
                            SummaryTextRow(stringResource(R.string.total_today), uiState.totalWorkedTime ?: "N/A", isBold = true)
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
                            colors = TimePickerDefaults.colors(
                                clockDialColor = MaterialTheme.colorScheme.surfaceVariant,
                                periodSelectorBorderColor = MaterialTheme.colorScheme.primary,
                                selectorColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
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
    modifier: Modifier = Modifier,
    buttonType: ButtonType,
    time: String,
    onClick: () -> Unit,
    onEditClick: () -> Unit,
    enabled: Boolean = true,
    colors: ButtonColors = ButtonDefaults.elevatedButtonColors()
) {
    ElevatedButton(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth(0.9f)
            .height(96.dp),
        shape = MaterialTheme.shapes.medium,
        colors = colors,
        elevation = ButtonDefaults.elevatedButtonElevation(defaultElevation = 4.dp, pressedElevation = 2.dp),
        enabled = enabled
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(horizontalAlignment = Alignment.Start) {
                Text(stringResource(buttonType.textResId), style = MaterialTheme.typography.titleLarge)
                if (time != "N/A") {
                    Text(
                        modifier = Modifier.padding(top = 6.dp),
                        text = time,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
            // IconButton will now always be part of the composition
            IconButton(onClick = onEditClick, enabled = enabled) {
                Icon(
                    Icons.Outlined.Edit,
                    contentDescription = stringResource(R.string.edit_button_description, stringResource(buttonType.textResId)),
                )
            }
        }
    }
}

@Composable
fun PausePairRow(
    index: Int,
    pause: PausePair,
    enabled: Boolean,
    onToLunch: () -> Unit,
    onFromLunch: () -> Unit,
    onEditToLunch: () -> Unit,
    onEditFromLunch: () -> Unit
) {
    Row(
        modifier = Modifier
            .padding(vertical = 16.dp)
            .fillMaxWidth(0.9f)
            .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.8f)), RoundedCornerShape(12.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            TimeButtonM3(
                buttonType = ButtonType.ToLunch,
                time = pause.start?.let { SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(it) } ?: "N/A",
                onClick = onToLunch,
                onEditClick = onEditToLunch,
                enabled = enabled,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            )
            TimeButtonM3(
                buttonType = ButtonType.FromLunch,
                time = pause.end?.let { SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(it) } ?: "N/A",
                onClick = onFromLunch,
                onEditClick = onEditFromLunch,
                enabled = enabled,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            )
        }
        Column(
            horizontalAlignment = Alignment.End,
            modifier = Modifier.padding(start = 8.dp)
        ) {
            Text(stringResource(R.string.pause_number, index + 1))
            if (pause.durationMinutes != null && pause.durationMinutes > 0) {
                val totalMinutes = pause.durationMinutes
                val durationText = if (totalMinutes < 60) {
                    "$totalMinutes ${stringResource(R.string.minutes_short)}"
                } else {
                    val hours = totalMinutes / 60
                    val minutes = totalMinutes % 60
                    if (minutes == 0L) {
                        "${hours}${stringResource(R.string.hours_short)}"
                    } else {
                        stringResource(R.string.hours_minutes_format, hours, minutes)
                    }
                }
                Text(
                    text = stringResource(R.string.duration, durationText),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@Preview(showBackground = true, name = "HomeScreen Logged In")
@Composable
fun HomeScreenPreview_LoggedIn() {
    WorkItOutM3Theme {
        HomeScreen(homeViewModel = PreviewHomeViewModel())
    }
}

@Preview(showBackground = true, name = "HomeScreen Initial")
@Composable
fun HomeScreenPreview_Initial() {
    WorkItOutM3Theme {
        val initialVm = PreviewHomeViewModel().apply {
            (this._uiState).value = getPreviewHomeUiState(
                enterTime = null,
                pauses = emptyList(), // Corrected
                exitTime = null,
                calculatedExitTime = null,
                totalWorkedTime = null
            )
        }
        HomeScreen(homeViewModel = initialVm)
    }
}
