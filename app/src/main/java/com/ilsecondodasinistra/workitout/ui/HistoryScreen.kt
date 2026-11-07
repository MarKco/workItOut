package com.ilsecondodasinistra.workitout.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ilsecondodasinistra.workitout.R
import com.ilsecondodasinistra.workitout.data.WorkHistoryEntry
import com.ilsecondodasinistra.workitout.domain.TimeFormats
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
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddEntry) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.add_day))
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                FilledTonalButton(onClick = viewModel::shareText, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.share))
                }
                FilledTonalButton(onClick = viewModel::shareCsv, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.export_csv))
                }
                FilledTonalButton(
                    onClick = viewModel::requestClear,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    ),
                ) {
                    Text(stringResource(R.string.clear))
                }
            }

            if (uiState.history.isEmpty()) {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(
                        text = stringResource(R.string.no_history_available),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(bottom = 88.dp), // room for the FAB
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(items = uiState.history, key = { it.id }) { entry ->
                        HistoryItemCard(entry = entry, onClick = { onEditEntry(entry.id) })
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

@Composable
fun HistoryItemCard(entry: WorkHistoryEntry, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(
                text = stringResource(R.string.date, TimeFormats.date(entry.enterTime)),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 6.dp),
            )
            HistoryDetailText(stringResource(R.string.entry_time, TimeFormats.time(entry.enterTime)))
            entry.pauses.forEach { pause ->
                HistoryDetailText(stringResource(R.string.pause_start, TimeFormats.time(pause.start)))
                HistoryDetailText(stringResource(R.string.pause_end, TimeFormats.time(pause.end)))
            }
            HistoryDetailText(stringResource(R.string.exit_time, TimeFormats.time(entry.exitTime)))
            HistoryDetailText(stringResource(R.string.total_worked_hours, entry.totalWorkedTime))
            HistoryDetailText(stringResource(R.string.daily_hours_set, entry.dailyHoursTarget))
        }
    }
}

@Composable
private fun HistoryDetailText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(vertical = 2.dp),
    )
}
