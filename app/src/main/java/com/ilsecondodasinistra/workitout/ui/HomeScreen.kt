package com.ilsecondodasinistra.workitout.ui

//import androidx.lifecycle.viewmodel.compose.viewModel // Already imported via IHomeViewModel
import IHomeViewModel
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleObserver
import com.ilsecondodasinistra.workitout.NOTIFICATION_CHANNEL_ID
import com.ilsecondodasinistra.workitout.NOTIFICATION_ID
import com.ilsecondodasinistra.workitout.ui.theme.WorkItOutM3Theme
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.collections.forEachIndexed
// Add this import
import com.ilsecondodasinistra.workitout.ui.PausePair

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    homeViewModel: IHomeViewModel
) {
    val uiState by homeViewModel.uiState.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val timePickerState = rememberTimePickerState(is24Hour = true)
    var showTimePickerDialog by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

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

    // Handle messages (Snackbar and Notifications)
    LaunchedEffect(uiState.message) {
        if (uiState.message.isNotEmpty()) {
            if (uiState.message.startsWith("NOTIFY_EXIT_TIME:")) {
                val notificationMessage = uiState.message.substringAfter("NOTIFY_EXIT_TIME:")
                showLocalNotification(context, "Ora di uscire!", "Il tuo orario di uscita previsto è $notificationMessage")
                homeViewModel.clearMessage() // Clear notification trigger message
            } else {
                scope.launch {
                    snackbarHostState.showSnackbar(
                        message = uiState.message,
                        duration = SnackbarDuration.Long
                    )
                }
                homeViewModel.clearMessage() // Clear regular message after showing
            }
        }
    }

    // Apply your M3 Theme
    WorkItOutM3Theme {
        Scaffold(
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
                        .padding(horizontal = 16.dp)
                        .verticalScroll(rememberScrollState())
                        .align(Alignment.TopCenter)
                        .padding(bottom = 180.dp), // Leave space for the summary card
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    TimeButtonM3(
                        modifier = Modifier.padding(vertical = 24.dp),
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
                            .fillMaxWidth(0.9f)
                            .height(48.dp)
                    ) {
                        Icon(Icons.Outlined.Add, contentDescription = "Aggiungi pausa")
                        Spacer(Modifier.width(8.dp))
                        Text("Aggiungi pausa")
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
                            .padding(bottom = 24.dp, top = 8.dp),
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
                Text(buttonType.text, style = MaterialTheme.typography.titleLarge)
                if (time != "N/A") {
                    Text(
                        modifier = Modifier.padding(top = 6.dp),
                        text = time,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
            if (time != "N/A") {
                IconButton(onClick = onEditClick, enabled = enabled) {
                    Icon(
                        Icons.Outlined.Edit,
                        contentDescription = "Modifica ${buttonType.text}",
                    )
                }
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
            .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline), RoundedCornerShape(12.dp))
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
        Text("Pausa ${index + 1}", modifier = Modifier.padding(start = 8.dp))
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
                    0,
                )
            }
        }
        Toast.makeText(context, "$title: $body (Notification permission needed)", Toast.LENGTH_LONG).show()
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
