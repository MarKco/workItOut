package com.ilsecondodasinistra.workitout.ui

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import com.ilsecondodasinistra.workitout.R
import com.ilsecondodasinistra.workitout.ui.theme.WorkItOutM3Theme
import kotlinx.coroutines.launch
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.WindowInsets
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ilsecondodasinistra.workitout.data.PausePair
import com.ilsecondodasinistra.workitout.domain.TimeFormats
import com.ilsecondodasinistra.workitout.domain.WorkTimeCalculator

@Composable
fun HomeRoute(viewModel: HomeViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    HomeScreen(uiState = uiState, actions = viewModel)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    uiState: HomeUiState,
    actions: HomeActions,
) {
    val context = LocalContext.current
    val timePickerState = rememberTimePickerState(is24Hour = true)
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(uiState.timePickerEvent) {
        uiState.timePickerEvent?.let { event ->
            timePickerState.hour = event.initialHour
            timePickerState.minute = event.initialMinute
        }
    }

    LaunchedEffect(uiState.message) {
        uiState.message?.let { message ->
            val text = message.resolve(context)
            scope.launch { snackbarHostState.showSnackbar(message = text, duration = SnackbarDuration.Short) }
            actions.messageShown()
        }
    }

    val expectedExitToast = stringResource(
        R.string.notification_exit_time_body,
        TimeFormats.time(uiState.calculatedExitTime),
    )
    LaunchedEffect(uiState.calculatedExitTime) {
        val exit = uiState.calculatedExitTime
        if (exit != null && exit > System.currentTimeMillis()) {
            Toast.makeText(context, expectedExitToast, Toast.LENGTH_SHORT).show()
        }
    }

    val alarmPermissionText = stringResource(R.string.alarm_permission_message)
    LaunchedEffect(uiState.alarmPermissionMissing) {
        if (uiState.alarmPermissionMissing) {
            Toast.makeText(context, alarmPermissionText, Toast.LENGTH_LONG).show()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                    .setData(Uri.fromParts("package", context.packageName, null))
                runCatching { context.startActivity(intent) }
            }
            actions.alarmPermissionHandled()
        }
    }

    // Apply your M3 Theme
    WorkItOutM3Theme {
        Scaffold(
            // Remove the topBar here to avoid duplicate toolbars
            contentWindowInsets = WindowInsets(0),
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
                        time = TimeFormats.time(uiState.enterTime),
                        onClick = { actions.press(ButtonType.Enter) },
                        onEditClick = { actions.requestTimeEdit(ButtonType.Enter) },
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
                            onToLunch = { actions.startPause(idx) },
                            onFromLunch = { actions.endPause(idx) },
                            onEditToLunch = { actions.requestPauseStartEdit(idx) },
                            onEditFromLunch = { actions.requestPauseEndEdit(idx) }
                        )
                    }
                    ElevatedButton(
                        onClick = { actions.addPause() },
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
                        time = TimeFormats.time(uiState.exitTime),
                        onClick = { actions.press(ButtonType.Exit) },
                        onEditClick = { actions.requestTimeEdit(ButtonType.Exit) },
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
                            SummaryTextRow(stringResource(R.string.estimated_exit), TimeFormats.time(uiState.calculatedExitTime))
                            SummaryTextRow(stringResource(R.string.total_today), uiState.totalWorkedTime ?: "N/A", isBold = true)
                        }
                    }
                }

                uiState.timePickerEvent?.let { pickerEvent ->
                    TimePickerDialog(
                        onDismiss = { actions.dismissTimePicker() },
                        onConfirm = {
                            actions.onTimeEdited(
                                type = pickerEvent.type,
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
                time = TimeFormats.time(pause.start),
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
                time = TimeFormats.time(pause.end),
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
            val durationMinutes = WorkTimeCalculator.pauseMinutes(pause)
            if (durationMinutes != null) {
                val totalMinutes = durationMinutes
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


@Preview(showBackground = true, name = "Home - session in progress")
@Composable
private fun HomeScreenInProgressPreview() {
    val now = System.currentTimeMillis()
    WorkItOutM3Theme {
        HomeScreen(
            uiState = HomeUiState(
                enterTime = now - 4 * 3_600_000L,
                pauses = listOf(PausePair(now - 2 * 3_600_000L, now - 90 * 60_000L)),
                calculatedExitTime = now + 4 * 3_600_000L,
            ),
            actions = HomeActions.None,
        )
    }
}

@Preview(showBackground = true, name = "Home - empty")
@Composable
private fun HomeScreenEmptyPreview() {
    WorkItOutM3Theme { HomeScreen(uiState = HomeUiState(), actions = HomeActions.None) }
}
