package com.ilsecondodasinistra.workitout.ui

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.ilsecondodasinistra.workitout.R
import com.ilsecondodasinistra.workitout.data.PausePair
import com.ilsecondodasinistra.workitout.domain.TimeFormats
import com.ilsecondodasinistra.workitout.domain.WorkTimeCalculator
import com.ilsecondodasinistra.workitout.ui.theme.WorkItOutM3Theme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val MS_PER_HOUR = 3_600_000L
private const val CLOCK_TICK_MS = 10_000L

/** Fixed-width digits so the numbers do not jitter while the clock runs. */
private fun TextStyle.tabular() = copy(fontFeatureSettings = "tnum")

@Composable
fun HomeRoute(viewModel: HomeViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Wall clock for the live progress. It only ticks while the screen is visible.
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                now = System.currentTimeMillis()
                delay(CLOCK_TICK_MS)
            }
        }
    }

    HomeScreen(uiState = uiState, actions = viewModel, now = now)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    uiState: HomeUiState,
    actions: HomeActions,
    now: Long = System.currentTimeMillis(),
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
        TimeFormats.timeShort(uiState.calculatedExitTime),
    )
    LaunchedEffect(uiState.calculatedExitTime) {
        val exit = uiState.calculatedExitTime
        if (exit != null && exit > System.currentTimeMillis()) {
            Toast.makeText(context, expectedExitToast, Toast.LENGTH_SHORT).show()
        }
    }

    val alarmPermissionText = stringResource(R.string.alarm_permission_message)
    val alarmSettingsLabel = stringResource(R.string.alarm_permission_settings_button)
    LaunchedEffect(uiState.alarmPermissionMissing) {
        if (uiState.alarmPermissionMissing) {
            actions.alarmPermissionHandled()
            scope.launch {
                val result = snackbarHostState.showSnackbar(
                    message = alarmPermissionText,
                    actionLabel = alarmSettingsLabel,
                    duration = SnackbarDuration.Long,
                )
                if (result == SnackbarResult.ActionPerformed && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                        .setData(Uri.fromParts("package", context.packageName, null))
                    runCatching { context.startActivity(intent) }
                }
            }
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Spacer(Modifier.height(4.dp))
                if (uiState.status(now) == DayStatus.Stale) StaleSessionBanner(uiState, actions)
                HeroCard(uiState = uiState, now = now)
                TimelineCard(uiState = uiState, actions = actions)
                Spacer(Modifier.height(4.dp))
            }
            ActionBar(
                uiState = uiState,
                actions = actions,
                now = now,
                modifier = Modifier.padding(top = 12.dp, bottom = 16.dp),
            )
        }

        uiState.timePickerEvent?.let { pickerEvent ->
            TimePickerDialog(
                onDismiss = { actions.dismissTimePicker() },
                onConfirm = {
                    actions.onTimeEdited(
                        type = pickerEvent.type,
                        hour = timePickerState.hour,
                        minute = timePickerState.minute,
                    )
                },
            ) {
                TimePicker(
                    state = timePickerState,
                    colors = TimePickerDefaults.colors(
                        clockDialColor = MaterialTheme.colorScheme.surfaceVariant,
                        periodSelectorBorderColor = MaterialTheme.colorScheme.primary,
                        selectorColor = MaterialTheme.colorScheme.primary,
                    ),
                )
            }
        }
    }
}

// --- Derived state --------------------------------------------------------------------------

private enum class DayStatus { NotStarted, Working, OnPause, Done, Stale }

private fun HomeUiState.openPause(): PausePair? = pauses.lastOrNull()?.takeIf { it.end == null }

private fun HomeUiState.status(now: Long): DayStatus = when {
    enterTime == null -> DayStatus.NotStarted
    exitTime != null -> DayStatus.Done
    WorkTimeCalculator.isStale(enterTime, now) -> DayStatus.Stale
    openPause()?.start != null -> DayStatus.OnPause
    else -> DayStatus.Working
}

// --- Forgotten session ----------------------------------------------------------------------

@Composable
private fun StaleSessionBanner(uiState: HomeUiState, actions: HomeActions) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
        ),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Filled.Warning, contentDescription = null)
                Text(
                    text = stringResource(R.string.stale_session_title, TimeFormats.date(uiState.enterTime)),
                    style = MaterialTheme.typography.titleSmall,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { actions.requestTimeEdit(ButtonType.Exit) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                    ),
                ) { Text(stringResource(R.string.stale_set_exit)) }
                TextButton(
                    onClick = actions::discardSession,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onErrorContainer),
                ) { Text(stringResource(R.string.stale_discard)) }
            }
        }
    }
}

// --- Hero -----------------------------------------------------------------------------------

@Composable
private fun HeroCard(uiState: HomeUiState, now: Long) {
    val status = uiState.status(now)
    val stale = status == DayStatus.Stale
    val enter = uiState.enterTime
    val targetMillis = (uiState.dailyHours * MS_PER_HOUR).toLong()
    val workedMillis = when {
        enter == null || stale -> 0L
        uiState.exitTime != null -> WorkTimeCalculator.workedMillis(enter, uiState.exitTime, uiState.pauses) ?: 0L
        else -> WorkTimeCalculator.workedSoFarMillis(enter, now, uiState.pauses)
    }
    val progress = if (targetMillis > 0) (workedMillis.toFloat() / targetMillis).coerceIn(0f, 1f) else 0f
    val overtime = targetMillis in 1..<workedMillis

    // The estimate is based on closed pauses only; a break in progress pushes the exit further.
    val openPauseMillis = uiState.openPause()?.start
        ?.takeIf { uiState.exitTime == null }
        ?.let { (now - it).coerceAtLeast(0) } ?: 0L
    val exitMillis = if (stale) null else uiState.exitTime ?: uiState.calculatedExitTime?.plus(openPauseMillis)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            StatusPill(status)
            ProgressRing(
                progress = progress,
                overtime = overtime,
                workedText = WorkTimeCalculator.formatDuration(workedMillis),
                targetText = stringResource(
                    R.string.hero_of_target,
                    WorkTimeCalculator.formatDuration(targetMillis),
                ),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                HeroStat(
                    label = stringResource(
                        if (status == DayStatus.Done) R.string.button_exit else R.string.hero_estimated_exit,
                    ),
                    value = TimeFormats.timeShort(exitMillis),
                    modifier = Modifier.weight(1f),
                )
                Box(
                    Modifier
                        .width(1.dp)
                        .fillMaxHeight()
                        .padding(vertical = 4.dp)
                        .background(MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f)),
                )
                HeroStat(
                    label = stringResource(if (overtime) R.string.hero_overtime else R.string.hero_remaining),
                    value = if (enter == null || stale) {
                        TimeFormats.PLACEHOLDER
                    } else if (overtime) {
                        WorkTimeCalculator.formatBalance(workedMillis - targetMillis)
                    } else {
                        WorkTimeCalculator.formatDuration((targetMillis - workedMillis).coerceAtLeast(0))
                    },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun StatusPill(status: DayStatus) {
    val (textRes, container, content) = when (status) {
        DayStatus.NotStarted -> Triple(
            R.string.status_not_started,
            MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
            MaterialTheme.colorScheme.onSurfaceVariant,
        )
        DayStatus.Working -> Triple(
            R.string.status_working,
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.onPrimary,
        )
        DayStatus.OnPause -> Triple(
            R.string.status_on_pause,
            MaterialTheme.colorScheme.tertiary,
            MaterialTheme.colorScheme.onTertiary,
        )
        DayStatus.Stale -> Triple(
            R.string.status_stale,
            MaterialTheme.colorScheme.error,
            MaterialTheme.colorScheme.onError,
        )
        DayStatus.Done -> Triple(
            R.string.status_done,
            MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
            MaterialTheme.colorScheme.onSurface,
        )
    }
    Surface(shape = CircleShape, color = container, contentColor = content) {
        Text(
            text = stringResource(textRes),
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
        )
    }
}

@Composable
private fun ProgressRing(progress: Float, overtime: Boolean, workedText: String, targetText: String) {
    val animated by animateFloatAsState(progress, animationSpec = tween(durationMillis = 700), label = "ring")
    val track = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.14f)
    val bar = if (overtime) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary

    Box(modifier = Modifier.size(212.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val strokeWidth = 16.dp.toPx()
            val inset = strokeWidth / 2
            val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
            val topLeft = Offset(inset, inset)
            drawArc(track, 0f, 360f, false, topLeft, arcSize, style = Stroke(strokeWidth))
            if (animated > 0f) {
                drawArc(
                    color = bar,
                    startAngle = -90f,
                    sweepAngle = 360f * animated,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(strokeWidth, cap = StrokeCap.Round),
                )
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = workedText,
                style = MaterialTheme.typography.headlineLarge.tabular(),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Text(
                text = targetText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
            )
        }
    }
}

@Composable
private fun HeroStat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge.tabular(),
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}

// --- Timeline -------------------------------------------------------------------------------

@Composable
private fun TimelineCard(uiState: HomeUiState, actions: HomeActions) {
    val hasEntered = uiState.enterTime != null
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            TimelineRow(isFirst = true, isLast = false, active = hasEntered) {
                Text(
                    text = stringResource(R.string.button_enter),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                TimeChip(
                    text = TimeFormats.timeShort(uiState.enterTime),
                    onClick = { actions.requestTimeEdit(ButtonType.Enter) },
                    contentDescription = stringResource(
                        R.string.edit_button_description,
                        stringResource(R.string.button_enter),
                    ),
                )
            }
            uiState.pauses.forEachIndexed { index, pause ->
                TimelineRow(isFirst = false, isLast = false, active = pause.end != null) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.pause_number, index + 1),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        pauseDurationText(pause)?.let { duration ->
                            Text(
                                text = duration,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        TimeChip(
                            text = TimeFormats.timeShort(pause.start),
                            onClick = { actions.requestPauseStartEdit(index) },
                            contentDescription = stringResource(
                                R.string.edit_button_description,
                                stringResource(R.string.button_to_lunch),
                            ),
                        )
                        Text("→", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        TimeChip(
                            text = TimeFormats.timeShort(pause.end),
                            onClick = { actions.requestPauseEndEdit(index) },
                            contentDescription = stringResource(
                                R.string.edit_button_description,
                                stringResource(R.string.button_from_lunch),
                            ),
                        )
                    }
                }
            }
            val exitDone = uiState.exitTime != null
            TimelineRow(isFirst = false, isLast = true, active = exitDone) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.button_exit),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (!exitDone && uiState.calculatedExitTime != null) {
                        Text(
                            text = stringResource(R.string.timeline_estimated),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        )
                    }
                }
                TimeChip(
                    text = TimeFormats.timeShort(uiState.exitTime ?: uiState.calculatedExitTime),
                    onClick = { actions.requestTimeEdit(ButtonType.Exit) },
                    enabled = hasEntered,
                    muted = !exitDone,
                    contentDescription = stringResource(
                        R.string.edit_button_description,
                        stringResource(R.string.button_exit),
                    ),
                )
            }
        }
    }
}

@Composable
private fun TimelineRow(
    isFirst: Boolean,
    isLast: Boolean,
    active: Boolean,
    content: @Composable RowScope.() -> Unit,
) {
    val lineColor = MaterialTheme.colorScheme.outlineVariant
    val dotColor = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .width(28.dp)
                .fillMaxHeight()
                .drawBehind {
                    val x = size.width / 2
                    val y = size.height / 2
                    val stroke = 2.dp.toPx()
                    if (!isFirst) drawLine(lineColor, Offset(x, 0f), Offset(x, y), stroke)
                    if (!isLast) drawLine(lineColor, Offset(x, y), Offset(x, size.height), stroke)
                    if (active) {
                        drawCircle(dotColor, radius = 6.dp.toPx(), center = Offset(x, y))
                    } else {
                        // Hollow dot: not recorded yet.
                        drawCircle(dotColor, radius = 5.dp.toPx(), center = Offset(x, y), style = Stroke(2.dp.toPx()))
                    }
                },
        )
        Row(
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 56.dp)
                .padding(start = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = content,
        )
    }
}

@Composable
private fun TimeChip(
    text: String,
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    muted: Boolean = false,
) {
    val container = if (muted) Color.Transparent else MaterialTheme.colorScheme.secondaryContainer
    val content = if (muted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSecondaryContainer
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = MaterialTheme.shapes.medium,
        color = container,
        contentColor = content,
        modifier = modifier
            .heightIn(min = 44.dp)
            .semantics { this.contentDescription = contentDescription },
    ) {
        Box(modifier = Modifier.padding(horizontal = 14.dp), contentAlignment = Alignment.Center) {
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium.tabular(),
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun pauseDurationText(pause: PausePair): String? {
    val totalMinutes = WorkTimeCalculator.pauseMinutes(pause) ?: return null
    val durationText = if (totalMinutes < 60) {
        "$totalMinutes ${stringResource(R.string.minutes_short)}"
    } else {
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        if (minutes == 0L) {
            "$hours${stringResource(R.string.hours_short)}"
        } else {
            stringResource(R.string.hours_minutes_format, hours, minutes)
        }
    }
    return stringResource(R.string.duration, durationText)
}

// --- Actions --------------------------------------------------------------------------------

@Composable
private fun ActionBar(uiState: HomeUiState, actions: HomeActions, now: Long, modifier: Modifier = Modifier) {
    val haptics = LocalHapticFeedback.current
    val status = uiState.status(now)
    val open = uiState.openPause()

    val primaryLabel: Int
    val onPrimary: () -> Unit
    when (status) {
        DayStatus.NotStarted, DayStatus.Done, DayStatus.Stale -> {
            primaryLabel = R.string.button_enter
            onPrimary = { actions.press(ButtonType.Enter) }
        }
        DayStatus.OnPause -> {
            primaryLabel = R.string.action_end_pause
            onPrimary = { actions.endPause(uiState.pauses.lastIndex) }
        }
        DayStatus.Working -> {
            primaryLabel = R.string.action_start_pause
            onPrimary = {
                // A leftover empty pause (added by an older version) is filled instead of duplicated.
                if (open != null) actions.startPause(uiState.pauses.lastIndex) else actions.startNewPause()
            }
        }
    }
    val inSession = status == DayStatus.Working || status == DayStatus.OnPause

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Button(
            onClick = {
                haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                onPrimary()
            },
            modifier = Modifier
                .weight(1f)
                .height(64.dp),
            shape = MaterialTheme.shapes.large,
        ) {
            if (status == DayStatus.Working) {
                Icon(painterResource(R.drawable.ic_pause), contentDescription = null)
            } else {
                Icon(Icons.Filled.PlayArrow, contentDescription = null)
            }
            Spacer(Modifier.width(8.dp))
            Text(stringResource(primaryLabel), style = MaterialTheme.typography.titleMedium)
        }
        if (inSession) {
            FilledTonalButton(
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                    actions.press(ButtonType.Exit)
                },
                modifier = Modifier.height(64.dp),
                shape = MaterialTheme.shapes.large,
            ) {
                Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.button_exit), style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

// --- Previews -------------------------------------------------------------------------------

@Preview(showBackground = true, name = "Home - working")
@Composable
private fun HomeScreenWorkingPreview() {
    val now = System.currentTimeMillis()
    WorkItOutM3Theme {
        HomeScreen(
            uiState = HomeUiState(
                enterTime = now - 5 * MS_PER_HOUR,
                pauses = listOf(PausePair(now - 3 * MS_PER_HOUR, now - 2 * MS_PER_HOUR)),
                calculatedExitTime = now + 4 * MS_PER_HOUR,
            ),
            actions = HomeActions.None,
            now = now,
        )
    }
}

@Preview(showBackground = true, name = "Home - on break")
@Composable
private fun HomeScreenOnBreakPreview() {
    val now = System.currentTimeMillis()
    WorkItOutM3Theme {
        HomeScreen(
            uiState = HomeUiState(
                enterTime = now - 4 * MS_PER_HOUR,
                pauses = listOf(PausePair(now - 20 * 60_000L, null)),
                calculatedExitTime = now + 4 * MS_PER_HOUR,
            ),
            actions = HomeActions.None,
            now = now,
        )
    }
}

@Preview(showBackground = true, name = "Home - forgotten session")
@Composable
private fun HomeScreenStalePreview() {
    val now = System.currentTimeMillis()
    WorkItOutM3Theme {
        HomeScreen(
            uiState = HomeUiState(enterTime = now - 30 * MS_PER_HOUR, calculatedExitTime = now - 22 * MS_PER_HOUR),
            actions = HomeActions.None,
            now = now,
        )
    }
}

@Preview(showBackground = true, name = "Home - empty")
@Composable
private fun HomeScreenEmptyPreview() {
    WorkItOutM3Theme { HomeScreen(uiState = HomeUiState(), actions = HomeActions.None) }
}
