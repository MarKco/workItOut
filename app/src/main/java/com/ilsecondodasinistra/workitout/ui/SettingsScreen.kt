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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.isEmpty
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.collectLatest

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
            settingsViewModel.clearMessage() // Clear message after showing
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
            settingsViewModel.onShareIntentLaunched() // Reset the event
        }
    }


    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Ore di lavoro totali Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.15f)),
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    text = "Ore di lavoro totali:",
                    color = Color(0xFF9A4616),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    OutlinedTextField(
                        value = uiState.dailyHoursInputString, // Bind directly to the String state
                        onValueChange = { newValue -> settingsViewModel.onDailyHoursInputChange(newValue) },
                        label = { Text("Ore (es. 7.5)") }, // Added a label for clarity
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors( /* ... your colors ... */ ),
                    )
                    Button(
                        onClick = { settingsViewModel.saveDailyHours() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF782F04)),
                        shape = RoundedCornerShape(8.dp),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp),
                    ) {
                        Text(text = "Salva", color = Color.White, fontSize = 16.sp)
                    }
                }
            }
        }

        // Share and Clear History Buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceAround,
        ) {
            Button(
                onClick = { settingsViewModel.shareHistory() },
                modifier = Modifier
                    .weight(1f)
                    .height(60.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF009688)), // Teal-500
                shape = RoundedCornerShape(12.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp, pressedElevation = 4.dp),
            ) {
                Icon(Icons.Filled.Share, contentDescription = "Condividi", tint = Color.White)
                Spacer(Modifier.width(8.dp))
                Text(text = "Condividi", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
            Spacer(Modifier.width(16.dp))
            Button(
                onClick = { settingsViewModel.requestClearHistory() },
                modifier = Modifier
                    .weight(1f)
                    .height(60.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)), // Red-700
                shape = RoundedCornerShape(12.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp, pressedElevation = 4.dp),
            ) {
                Icon(Icons.Filled.Delete, contentDescription = "Pulisci", tint = Color.White)
                Spacer(Modifier.width(8.dp))
                Text(text = "Pulisci", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }

        // Animated message display (from original code, slightly adapted)
        AnimatedVisibility(
            visible = uiState.message.isNotBlank(), // Show if ViewModel message is set
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            Text(
                text = uiState.message,
                color = if (uiState.message.startsWith("Error") || uiState.message.startsWith("Errore")) MaterialTheme.colorScheme.error else Color(
                    0xFF9A4616
                ),
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        (if (uiState.message.startsWith("Error") || uiState.message.startsWith("Errore")) MaterialTheme.colorScheme.errorContainer else Color(
                            0xFF9A4616
                        ).copy(alpha = 0.2f))
                    )
                    .padding(12.dp),
                textAlign = TextAlign.Center
            )
        }
        Spacer(Modifier.height(16.dp))


        // History List Card
        Card(
            modifier = Modifier.fillMaxSize(), // Takes remaining space
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.1f)),
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    text = "Storico",
                    color = Color(0xFF9A4616),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                if (uiState.history.isEmpty()) {
                    Text(
                        text = "Nessuno storico disponibile.",
                        color = Color(0xFF9A4616).copy(alpha = 0.7f),
                        fontSize = 16.sp,
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(uiState.history, key = { it["id"].toString() + (it["enterTime"] as? Long ?: 0L) }) { record ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF9A4616).copy(alpha = 0.15f)),
                            ) {
                                Column(Modifier.padding(12.dp)) {
                                    Text(
                                        text = "Data: ${record["id"]}",
                                        color = Color(0xFF9A4616),
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(bottom = 4.dp),
                                    )
                                    Text(
                                        text = "Ingresso: ${settingsViewModel.formatLongTimestampToDisplay(record["enterTime"] as? Long)}",
                                        color = Color(0xFF9A4616).copy(alpha = 0.8f), fontSize = 14.sp,
                                    )
                                    Text(
                                        text = "In pausa: ${settingsViewModel.formatLongTimestampToDisplay(record["toLunchTime"] as? Long)}",
                                        color = Color(0xFF9A4616).copy(alpha = 0.8f), fontSize = 14.sp,
                                    )
                                    Text(
                                        text = "Fine pausa: ${settingsViewModel.formatLongTimestampToDisplay(record["fromLunchTime"] as? Long)}",
                                        color = Color(0xFF9A4616).copy(alpha = 0.8f), fontSize = 14.sp,
                                    )
                                    Text(
                                        text = "Uscita: ${settingsViewModel.formatLongTimestampToDisplay(record["exitTime"] as? Long)}",
                                        color = Color(0xFF9A4616).copy(alpha = 0.8f), fontSize = 14.sp,
                                    )
                                    Text(
                                        text = "Ore lavorate totali: ${record["totalWorkedTime"] ?: "N/A"}",
                                        color = Color(0xFF9A4616).copy(alpha = 0.8f), fontSize = 14.sp,
                                    )
                                    Text(
                                        text = "Ore giornaliere impostate: ${record["dailyHours"] ?: "N/A"}h",
                                        color = Color(0xFF9A4616).copy(alpha = 0.8f), fontSize = 14.sp,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Confirmation Dialog for Clearing History
    if (uiState.showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { settingsViewModel.cancelClearHistory() },
            title = { Text("Pulisci lo storico") },
            text = { Text("Sei sicuro di voler cancellare tutto lo storico? Questa azione non può essere annullata.") },
            confirmButton = {
                Button(
                    onClick = { settingsViewModel.confirmClearHistory() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                ) { Text("Conferma", color = Color.White) }
            },
            dismissButton = {
                Button(onClick = { settingsViewModel.cancelClearHistory() }) { Text("Annulla") }
            }
        )
    }
}


@Preview(showBackground = true, backgroundColor = 0xFFF0EAE2)
@Composable
fun SettingsScreenPreview() {
    // For preview, we might need a way to inject a ViewModel with preview data
    // or rely on the default ViewModel instantiation which will use the actual LocalHistoryRepository.
    // To make previews more hermetic, you could have a factory for ViewModel that allows injecting
    // a version with dummy data for previews.

    // Add some dummy data to the local repository for the preview
    // This will be picked up by the default ViewModel if LocalHistoryRepository is an object
    LaunchedEffect(Unit) {
        if (LocalHistoryRepository.history.isEmpty()) {
            LocalHistoryRepository.addDummyRecord(
                System.currentTimeMillis() - 86400000 * 2,
                System.currentTimeMillis() - (86400000 * 2 - 8 * 3600000),
                "7h 45m",
                8.0
            )
            LocalHistoryRepository.addDummyRecord(
                System.currentTimeMillis() - 86400000,
                System.currentTimeMillis() - (86400000 - 7.5 * 3600000).toLong(),
                "7h 30m",
                7.5
            )
        }
    }

    MaterialTheme {
        SettingsScreen()
    }
}