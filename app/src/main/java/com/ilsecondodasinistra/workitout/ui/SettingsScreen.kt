package com.ilsecondodasinistra.workitout.ui

import android.content.Intent
import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ilsecondodasinistra.workitout.ui.theme.WorkItOutM3Theme
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settingsViewModel: SettingsViewModel = viewModel()
) {
    val uiState by settingsViewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Handle messages
    LaunchedEffect(uiState.message) {
        if (uiState.message.isNotEmpty()) {
            scope.launch {
                snackbarHostState.showSnackbar(message = uiState.message)
            }
            settingsViewModel.clearMessage()
        }
    }

    // Handle share intent event
    LaunchedEffect(uiState.shareIntentEvent) {
        uiState.shareIntentEvent?.let { event ->
            try {
                val sendIntent: Intent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, event.textToShare)
                    type = "text/plain"
                }
                val shareIntent = Intent.createChooser(sendIntent, "Condividi storico tramite...")
                context.startActivity(shareIntent)
            } catch (e: Exception) {
                Log.e("SettingsScreen", "Error sharing history: ${e.message}", e)
                scope.launch {
                    snackbarHostState.showSnackbar(message = "Impossibile avviare la condivisione.")
                }
            }
            settingsViewModel.onShareIntentLaunched()
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
                    text = "Ore di lavoro totali:",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    OutlinedTextField(
                        value = uiState.dailyHoursInputString,
                        onValueChange = { settingsViewModel.onDailyHoursInputChange(it) },
                        label = { Text("Ore (es. 7.5)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp),
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            disabledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                            focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                            unfocusedIndicatorColor = MaterialTheme.colorScheme.outline,
                            focusedLabelColor = MaterialTheme.colorScheme.primary,
                            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            cursorColor = MaterialTheme.colorScheme.primary,
                        )
                    )
                    Button(
                        onClick = { settingsViewModel.saveDailyHours() },
                        shape = MaterialTheme.shapes.medium,
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
                    ) {
                        Text(text = "Salva", style = MaterialTheme.typography.labelLarge)
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
                        contentDescription = "Condividi",
                        modifier = Modifier.size(ButtonDefaults.IconSize)
                    )
                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                    Text(text = "Condividi", style = MaterialTheme.typography.labelLarge)
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
                        contentDescription = "Pulisci",
                        modifier = Modifier.size(ButtonDefaults.IconSize)
                    )
                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                    Text(text = "Pulisci", style = MaterialTheme.typography.labelLarge)
                }
            }
            // Spacer(Modifier.height(16.dp)) // Managed by main Column's verticalArrangement

            // History List Section (No Card)
            Text( // Title for the history section
                text = "Storico",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp), // Padding between title and list
            )

            if (uiState.history.isEmpty()) {
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth().padding(16.dp), // Added weight here
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Nessuno storico disponibile.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(), // Allow LazyColumn to take remaining space
                    contentPadding = PaddingValues(vertical = 8.dp), // Padding inside the list, top and bottom
                    verticalArrangement = Arrangement.spacedBy(12.dp), // Spacing between history items
                ) {
                    items(uiState.history, key = { it["id"].toString() + (it["enterTime"] as? Long ?: 0L) }) { record ->
                        HistoryItemCard(record = record, settingsViewModel = settingsViewModel)
                    }
                }
            }
        }

        // Confirmation Dialog for Clearing History
        if (uiState.showClearConfirmDialog) {
            AlertDialog(
                onDismissRequest = { settingsViewModel.cancelClearHistory() },
                title = { Text("Pulisci lo storico", style = MaterialTheme.typography.headlineSmall) },
                text = { Text("Sei sicuro di voler cancellare tutto lo storico? Questa azione non può essere annullata.", style = MaterialTheme.typography.bodyMedium) },
                confirmButton = {
                    TextButton(
                        onClick = { settingsViewModel.confirmClearHistory() },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) { Text("Conferma") }
                },
                dismissButton = {
                    TextButton(onClick = { settingsViewModel.cancelClearHistory() }) { Text("Annulla") }
                },
            )
        }
    }
}

@Composable
fun HistoryItemCard(record: Map<String, Any?>, settingsViewModel: SettingsViewModel) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.background
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.padding(12.dp)) {
            val enterTime = record["enterTime"] as? Long
            val dateString = enterTime?.let {
                java.text.SimpleDateFormat("dd-MM-yyyy", java.util.Locale.getDefault()).format(java.util.Date(it))
            } ?: (record["id"]?.toString() ?: "N/A")
            Text(
                text = "Data: $dateString",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 6.dp),
            )
            HistoryDetailText("Ingresso: ${settingsViewModel.formatLongTimestampToDisplay(record["enterTime"] as? Long)}")
            HistoryDetailText("In pausa: ${settingsViewModel.formatLongTimestampToDisplay(record["toLunchTime"] as? Long)}")
            HistoryDetailText("Fine pausa: ${settingsViewModel.formatLongTimestampToDisplay(record["fromLunchTime"] as? Long)}")
            HistoryDetailText("Uscita: ${settingsViewModel.formatLongTimestampToDisplay(record["exitTime"] as? Long)}")
            HistoryDetailText("Ore lavorate totali: ${record["totalWorkedTime"] ?: "N/A"}")
            HistoryDetailText("Ore giornaliere impostate: ${record["dailyHours"] ?: "N/A"}h")
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


@Preview(showBackground = true)
@Composable
fun SettingsScreenPreview() {
    WorkItOutM3Theme {
        Surface(color = MaterialTheme.colorScheme.background) {
            SettingsScreen()
        }
    }
}
