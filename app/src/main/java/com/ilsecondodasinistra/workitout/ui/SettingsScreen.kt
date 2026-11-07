package com.ilsecondodasinistra.workitout.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
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
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ilsecondodasinistra.workitout.data.WorkHistoryEntry
import com.ilsecondodasinistra.workitout.domain.TimeFormats
import com.ilsecondodasinistra.workitout.R
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(settingsViewModel: SettingsViewModel) {
    val uiState by settingsViewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val shareHistoryVia = stringResource(R.string.share_history_via)
    val errorCannotStartSharing = stringResource(R.string.error_cannot_start_sharing)

    LaunchedEffect(uiState.message) {
        uiState.message?.let { message ->
            val text = message.resolve(context)
            scope.launch { snackbarHostState.showSnackbar(message = text) }
            settingsViewModel.messageShown()
        }
    }

    LaunchedEffect(uiState.shareText) {
        uiState.shareText?.let { text ->
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                putExtra(Intent.EXTRA_TEXT, text)
                type = "text/plain"
            }
            try {
                context.startActivity(Intent.createChooser(sendIntent, shareHistoryVia))
            } catch (_: ActivityNotFoundException) {
                snackbarHostState.showSnackbar(message = errorCannotStartSharing)
            }
            settingsViewModel.onShareLaunched()
        }
    }

    val keyboardController = LocalSoftwareKeyboardController.current
    var textFieldValue by remember { mutableStateOf(TextFieldValue(uiState.dailyHoursInput)) }
    // Sync with ViewModel state if it changes externally
    LaunchedEffect(uiState.dailyHoursInput) {
        if (textFieldValue.text != uiState.dailyHoursInput) {
            textFieldValue = textFieldValue.copy(text = uiState.dailyHoursInput)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding) // Apply padding from Scaffold
                .padding(16.dp), // Maintain original screen horizontal padding
            verticalArrangement = Arrangement.spacedBy(16.dp) // Consistent spacing for direct children
        ) {
            // Daily Working Hours Section (No Card)
            Column { // Column to group daily hours elements
                Text(
                    text = stringResource(R.string.total_working_hours),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    TextField(
                        value = textFieldValue,
                        onValueChange = {
                            textFieldValue = it
                            settingsViewModel.onDailyHoursInputChange(it.text)
                        },
                        label = { Text(stringResource(R.string.hours_example)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp),
                        singleLine = true,
                    )
                    Button(
                        onClick = {
                            keyboardController?.hide()
                            settingsViewModel.saveDailyHours()
                        },
                        shape = MaterialTheme.shapes.medium,
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
                    ) {
                        Text(text = stringResource(R.string.save), style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
            // Spacer(Modifier.height(16.dp)) // Managed by main Column's verticalArrangement

            // Share and Clear History Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                // .padding(bottom = 16.dp), // Spacing handled by main Column's verticalArrangement
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                FilledTonalButton(
                    onClick = { settingsViewModel.shareHistory() },
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    shape = MaterialTheme.shapes.medium,
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 1.dp),
                ) {
                    Icon(
                        Icons.Filled.Share,
                        contentDescription = stringResource(R.string.share),
                        modifier = Modifier.size(ButtonDefaults.IconSize)
                    )
                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                    Text(text = stringResource(R.string.share), style = MaterialTheme.typography.labelLarge)
                }

                Button(
                    onClick = { settingsViewModel.requestClearHistory() },
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    shape = MaterialTheme.shapes.medium,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
                ) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = stringResource(R.string.clear),
                        modifier = Modifier.size(ButtonDefaults.IconSize)
                    )
                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                    Text(text = stringResource(R.string.clear), style = MaterialTheme.typography.labelLarge)
                }
            }
            // Spacer(Modifier.height(16.dp)) // Managed by main Column's verticalArrangement

            // History List Section (No Card)
            Text( // Title for the history section
                text = stringResource(R.string.history),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp), // Padding between title and list
            )

            if (uiState.history.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(16.dp), // Added weight here
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.no_history_available),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(), // Allow LazyColumn to take remaining space
                    contentPadding = PaddingValues(vertical = 8.dp), // Padding inside the list, top and bottom
                    verticalArrangement = Arrangement.spacedBy(12.dp), // Spacing between history items
                ) {
                    items(items = uiState.history, key = { it.id }) { entry ->
                        HistoryItemCard(entry = entry)
                    }
                }
            }
        }

        // Confirmation Dialog for Clearing History
        if (uiState.showClearConfirmDialog) {
            AlertDialog(
                onDismissRequest = { settingsViewModel.cancelClearHistory() },
                title = { Text(stringResource(R.string.clear_history), style = MaterialTheme.typography.headlineSmall) },
                text = { Text(stringResource(R.string.clear_history_confirmation), style = MaterialTheme.typography.bodyMedium) },
                confirmButton = {
                    TextButton(
                        onClick = { settingsViewModel.confirmClearHistory() },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) { Text(stringResource(R.string.confirm)) }
                },
                dismissButton = {
                    TextButton(onClick = { settingsViewModel.cancelClearHistory() }) { Text(stringResource(R.string.cancel)) }
                },
            )
        }
    }
}

@Composable
fun HistoryItemCard(entry: WorkHistoryEntry) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(
                text = stringResource(R.string.date, TimeFormats.date(entry.enterTime)),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
fun HistoryDetailText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
        modifier = Modifier.padding(vertical = 2.dp)
    )
}
