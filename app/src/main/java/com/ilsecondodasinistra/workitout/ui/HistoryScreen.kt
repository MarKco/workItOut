package com.ilsecondodasinistra.workitout.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ilsecondodasinistra.workitout.R
import com.ilsecondodasinistra.workitout.data.WorkHistoryEntry
import com.ilsecondodasinistra.workitout.domain.TimeFormats
import com.ilsecondodasinistra.workitout.domain.WorkTimeCalculator
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import kotlinx.coroutines.launch

@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel,
    onEditEntry: (id: String) -> Unit,
    onAddEntry: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val shareTextTitle = stringResource(R.string.share_history_via)
    val shareCsvTitle = stringResource(R.string.csv_share_title)

    LaunchedEffect(uiState.message) {
        uiState.message?.let { message ->
            val text = message.resolve(context)
            scope.launch { snackbarHostState.showSnackbar(message = text) }
            viewModel.messageShown()
        }
    }

    val deletedText = stringResource(R.string.history_day_deleted)
    val undoLabel = stringResource(R.string.undo)
    LaunchedEffect(uiState.undoDelete) {
        if (uiState.undoDelete == null) return@LaunchedEffect
        val result = snackbarHostState.showSnackbar(
            message = deletedText,
            actionLabel = undoLabel,
            duration = SnackbarDuration.Short,
        )
        if (result == SnackbarResult.ActionPerformed) viewModel.undoDelete() else viewModel.undoDeleteHandled()
    }

    LaunchedEffect(uiState.shareText, uiState.shareCsv) {
        uiState.shareText?.let { text ->
            if (!context.shareText(text, shareTextTitle)) viewModel.showShareError()
        }
        uiState.shareCsv?.let { csv ->
            if (!context.shareCsvFile(csv, "workitout-history.csv", shareCsvTitle)) viewModel.showShareError()
        }
        if (uiState.shareText != null || uiState.shareCsv != null) viewModel.onShareLaunched()
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddEntry,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.add_day))
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
        ) {
            HistoryHeader(
                dayCount = uiState.history.size,
                onShare = viewModel::shareText,
                onCsv = viewModel::shareCsv,
                onClear = viewModel::requestClear,
            )

            if (uiState.history.isEmpty()) {
                EmptyHistory(Modifier.weight(1f).fillMaxWidth())
            } else {
                val months = remember(uiState.history) { uiState.history.groupBy { monthOf(it) } }
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(bottom = 96.dp), // room for the FAB
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    months.forEach { (month, entries) ->
                        stickyHeader(key = "month-$month") { MonthHeader(month) }
                        items(items = entries, key = { it.id }) { entry ->
                            SwipeToDeleteRow(onDelete = { viewModel.deleteEntry(entry.id) }) {
                                HistoryItemCard(entry = entry, onClick = { onEditEntry(entry.id) })
                            }
                        }
                    }
                }
            }
        }

        if (uiState.showClearConfirmDialog) {
            AlertDialog(
                onDismissRequest = viewModel::cancelClear,
                title = { Text(stringResource(R.string.clear_history), style = MaterialTheme.typography.headlineSmall) },
                text = { Text(stringResource(R.string.clear_history_confirmation)) },
                confirmButton = {
                    TextButton(
                        onClick = viewModel::confirmClear,
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    ) { Text(stringResource(R.string.confirm)) }
                },
                dismissButton = { TextButton(onClick = viewModel::cancelClear) { Text(stringResource(R.string.cancel)) } },
            )
        }
    }
}

private fun monthOf(entry: WorkHistoryEntry): YearMonth =
    YearMonth.from(Instant.ofEpochMilli(entry.enterTime).atZone(ZoneId.systemDefault()))

@Composable
private fun HistoryHeader(dayCount: Int, onShare: () -> Unit, onCsv: () -> Unit, onClear: () -> Unit) {
    var menuOpen by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = pluralStringResource(R.plurals.history_days, dayCount, dayCount),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp),
        )
        Box {
            IconButton(onClick = { menuOpen = true }) {
                Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.more_options))
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.share)) },
                    leadingIcon = { Icon(Icons.Filled.Share, contentDescription = null) },
                    onClick = { menuOpen = false; onShare() },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.export_csv)) },
                    leadingIcon = { Icon(Icons.Filled.Share, contentDescription = null) },
                    onClick = { menuOpen = false; onCsv() },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.clear_history), color = MaterialTheme.colorScheme.error) },
                    leadingIcon = {
                        Icon(Icons.Filled.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    },
                    onClick = { menuOpen = false; onClear() },
                )
            }
        }
    }
}

@Composable
private fun EmptyHistory(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_history),
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.outline,
        )
        Text(
            text = stringResource(R.string.no_history_available),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 16.dp),
        )
        Text(
            text = stringResource(R.string.history_empty_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun MonthHeader(month: YearMonth) {
    val locale = LocalConfiguration.current.locales[0]
    val text = month.format(DateTimeFormatter.ofPattern("LLLL yyyy", locale))
        .replaceFirstChar { it.titlecase(locale) }
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(start = 4.dp, top = 12.dp, bottom = 6.dp),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeToDeleteRow(onDelete: () -> Unit, content: @Composable () -> Unit) {
    val state = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onDelete()
                true
            } else {
                false
            }
        },
    )
    SwipeToDismissBox(
        state = state,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(MaterialTheme.shapes.large)
                    .background(MaterialTheme.colorScheme.errorContainer)
                    .padding(end = 24.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = stringResource(R.string.delete),
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
        },
    ) { content() }
}

@Composable
fun HistoryItemCard(entry: WorkHistoryEntry, onClick: () -> Unit) {
    val zone = ZoneId.systemDefault()
    val locale = LocalConfiguration.current.locales[0]
    val date = Instant.ofEpochMilli(entry.enterTime).atZone(zone).toLocalDate()
    val worked = WorkTimeCalculator.workedMillis(entry.enterTime, entry.exitTime, entry.pauses) ?: 0L
    val balance = worked - (entry.dailyHoursTarget * 3_600_000).toLong()
    val pauseMillis = WorkTimeCalculator.totalPauseMillis(entry.pauses)

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Column(
                modifier = Modifier
                    .size(56.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(16.dp)),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = date.dayOfWeek.getDisplayName(TextStyle.SHORT, locale).uppercase(locale).trimEnd('.'),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f),
                )
                Text(
                    text = date.dayOfMonth.toString(),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${TimeFormats.timeShort(entry.enterTime)} → ${TimeFormats.timeShort(entry.exitTime)}",
                    style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum"),
                )
                if (pauseMillis > 0) {
                    Text(
                        text = stringResource(R.string.history_breaks, WorkTimeCalculator.formatDuration(pauseMillis)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = WorkTimeCalculator.formatDuration(worked),
                    style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum"),
                    fontWeight = FontWeight.Bold,
                )
                BalanceChip(balance)
            }
        }
    }
}

/** "+10m" in green or "-5m" in red; nothing when the day hit its target exactly. */
@Composable
private fun BalanceChip(balanceMillis: Long) {
    val text = WorkTimeCalculator.formatBalance(balanceMillis)
    if (balanceMillis == 0L || text == "0m") return
    val positive = balanceMillis > 0
    Surface(
        shape = CircleShape,
        color = if (positive) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.errorContainer,
        contentColor = if (positive) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onErrorContainer,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
        )
    }
}
