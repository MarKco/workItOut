package com.ilsecondodasinistra.workitout.ui

import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.* // Ensure this is the M3 import
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ilsecondodasinistra.workitout.ui.theme.WorkItOutM3Theme // Import your theme

@OptIn(ExperimentalMaterial3Api::class) // Keep if using experimental M3 APIs
@Composable
fun SettingsScreen(
    settingsViewModel: SettingsViewModel = viewModel()
) {
    val uiState by settingsViewModel.uiState.collectAsState()
    val context = LocalContext.current

    // Handle messages
    LaunchedEffect(uiState.message) {
        if (uiState.message.isNotEmpty()) {
            Toast.makeText(context, uiState.message, Toast.LENGTH_SHORT).show()
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
                Toast.makeText(context, "Impossibile avviare la condivisione.", Toast.LENGTH_SHORT).show()
            }
            settingsViewModel.onShareIntentLaunched()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
        // Padding is likely handled by the parent layout in WorkItOutAppEntry,
        // but if this screen is used standalone, add .padding(16.dp) here.
        // For now, assuming parent handles general screen padding.
    ) {
        // Daily Working Hours Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            shape = MaterialTheme.shapes.large, // Use M3 shape
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant // M3 themed surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    text = "Ore di lavoro totali:",
                    style = MaterialTheme.typography.titleLarge, // M3 Typography
                    color = MaterialTheme.colorScheme.onSurfaceVariant, // M3 themed text color
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
                        // TextFieldDefaults.outlinedTextFieldColors will use theme colors by default
                        // You can customize further if needed:
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
                        // ButtonDefaults.buttonColors will use theme colors by default
                        // (primary container, onPrimary content)
                        // colors = ButtonDefaults.buttonColors(
                        // containerColor = MaterialTheme.colorScheme.primary,
                        // contentColor = MaterialTheme.colorScheme.onPrimary
                        // ),
                        shape = MaterialTheme.shapes.medium, // M3 Shape
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
                    ) {
                        Text(text = "Salva", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }

        // Share and Clear History Buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp), // Consistent spacing
        ) {
            // Share Button (FilledTonalButton for less emphasis than primary action)
            FilledTonalButton( // M3 alternative for secondary actions
                onClick = { settingsViewModel.shareHistory() },
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp), // Standard M3 button height
                shape = MaterialTheme.shapes.medium,
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 1.dp),
                // colors = ButtonDefaults.filledTonalButtonColors(
                // containerColor = MaterialTheme.colorScheme.secondaryContainer,
                // contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                // )
            ) {
                Icon(
                    Icons.Filled.Share,
                    contentDescription = "Condividi",
                    modifier = Modifier.size(ButtonDefaults.IconSize)
                )
                Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                Text(text = "Condividi", style = MaterialTheme.typography.labelLarge)
            }

            // Clear Button (Error color for destructive action)
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

        // Animated message display
        AnimatedVisibility(
            visible = uiState.message.isNotBlank(),
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            val isErrorMessage = uiState.message.startsWith("Error", ignoreCase = true) ||
                    uiState.message.startsWith("Errore", ignoreCase = true)
            Text(
                text = uiState.message,
                color = if (isErrorMessage) MaterialTheme.colorScheme.onErrorContainer
                else MaterialTheme.colorScheme.onPrimaryContainer, // Or onSecondaryContainer
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.medium)
                    .background(
                        if (isErrorMessage) MaterialTheme.colorScheme.errorContainer
                        else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f) // Or secondaryContainer
                    )
                    .padding(12.dp),
                textAlign = TextAlign.Center
            )
        }
        Spacer(Modifier.height(16.dp)) // Add space if message is shown

        // History List Card
        Text( // Title for the history section
            text = "Storico",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary, // Use primary for section titles
            modifier = Modifier.padding(bottom = 8.dp),
        )
        Card(
            modifier = Modifier.fillMaxSize(),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface // Main surface for list
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            if (uiState.history.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
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
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp), // Padding inside the list card
                    verticalArrangement = Arrangement.spacedBy(12.dp), // Spacing between history items
                ) {
                    items(uiState.history, key = { it["id"].toString() + (it["enterTime"] as? Long ?: 0L) }) { record ->
                        HistoryItemCard(record = record, settingsViewModel = settingsViewModel)
                    }
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
                TextButton( // Use TextButton for dialog actions as per M3
                    onClick = { settingsViewModel.confirmClearHistory() },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Conferma") }
            },
            dismissButton = {
                TextButton(onClick = { settingsViewModel.cancelClearHistory() }) { Text("Annulla") }
            },
            // containerColor = MaterialTheme.colorScheme.surface, // Dialogs use surface color
            // titleContentColor = MaterialTheme.colorScheme.onSurface,
            // textContentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun HistoryItemCard(record: Map<String, Any?>, settingsViewModel: SettingsViewModel) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium, // M3 shape for list items
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f) // Slightly different from main list card
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(
                text = "Data: ${record["id"]}",
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
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f), // Slightly muted for details
        modifier = Modifier.padding(vertical = 2.dp)
    )
}


@Preview(showBackground = true)
@Composable
fun SettingsScreenPreview() {
    WorkItOutM3Theme { // Wrap preview in your M3 Theme
        Surface(color = MaterialTheme.colorScheme.background) { // Add surface for background in preview
            SettingsScreen()
        }
    }
}