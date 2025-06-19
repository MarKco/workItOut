package com.ilsecondodasinistra.workitout.ui

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.util.Log
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
import androidx.compose.material.icons.filled.Delete // Added for Clear History Dialog
import androidx.compose.material.icons.filled.Share // Added for Share History Dialog
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Constants for SharedPreferences
private const val PREFS_NAME = "workitout_settings_prefs"
private const val KEY_DAILY_HOURS = "daily_hours"

// --- Mock Data for Preview and In-Memory History ---
// In a real app, this would be managed by a ViewModel and potentially Room
object LocalHistoryRepository {
    private val _history = mutableStateListOf<Map<String, Any?>>()
    val history: List<Map<String, Any?>> get() = _history.toList()

    fun addRecord(record: Map<String, Any?>) {
        // Add to the beginning to keep it sorted by most recent
        _history.add(0, record.toMutableMap().apply { put("id", SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(getTimestampFromRecord(record)))) })
    }

    fun clearHistory() {
        _history.clear()
    }

    // Helper to get a consistent timestamp for sorting/ID from various potential time fields
    private fun getTimestampFromRecord(record: Map<String, Any?>): Long {
        return (record["enterTime"] as? Long)
            ?: (record["toLunchTime"] as? Long)
            ?: (record["fromLunchTime"] as? Long)
            ?: (record["exitTime"] as? Long)
            ?: System.currentTimeMillis()
    }

    // Example function to add some dummy data for testing
    fun addDummyRecord(enterTime: Long, exitTime: Long, totalWorked: String, dailyHoursSet: Double) {
        addRecord(
            mapOf(
                "enterTime" to enterTime,
                "toLunchTime" to enterTime + 3600000, // +1 hour
                "fromLunchTime" to enterTime + 7200000, // +2 hours
                "exitTime" to exitTime,
                "totalWorkedTime" to totalWorked,
                "dailyHours" to dailyHoursSet
            )
        )
    }
}
// --- End Mock Data ---


@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val sharedPreferences = remember { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }

    var dailyHoursInput by remember { mutableStateOf(sharedPreferences.getFloat(KEY_DAILY_HOURS, 8.0f).toDouble()) }
    var history by remember { mutableStateOf(LocalHistoryRepository.history) } // Use our local repo
    var message by remember { mutableStateOf("") }
    var showClearConfirmDialog by remember { mutableStateOf(false) } // For clear history confirmation

    val coroutineScope = rememberCoroutineScope()

    // Helper function to format Long Timestamp to readable time string
    val formatLongTimestamp: (Long?) -> String = { timestamp ->
        timestamp?.let { SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(it)) } ?: "N/A"
    }

    // Load daily hours initially (already done in remember initial value)
    // and observe changes to history (which will be manual in this local version)

    val handleDailyHoursChange: () -> Unit = {
        coroutineScope.launch {
            try {
                with(sharedPreferences.edit()) {
                    putFloat(KEY_DAILY_HOURS, dailyHoursInput.toFloat())
                    apply()
                }
                message = "Daily hours updated successfully!"
                Log.d("SettingsScreen", "Daily hours saved: $dailyHoursInput")
            } catch (e: Exception) {
                Log.e("SettingsScreen", "Error updating daily hours: ${e.message}", e)
                message = "Error updating daily hours."
            }
        }
    }

    val clearHistoryAction: () -> Unit = {
        coroutineScope.launch {
            try {
                LocalHistoryRepository.clearHistory()
                history = LocalHistoryRepository.history // Refresh the local state
                message = "Storico ripulito con successo!"
                Log.d("SettingsScreen", "History cleared.")
            } catch (e: Exception) {
                Log.e("SettingsScreen", "Error clearing history: ${e.message}", e)
                message = "Errore durante la pulizia dello storico."
            }
        }
    }

    val shareHistory: () -> Unit = {
        if (history.isEmpty()) {
            message = "Nessuno storico da condividere!"
        }
        else {
            val shareTextBuilder = StringBuilder("Storico ore lavorate:\n\n")
            history.forEach { record ->
                // The 'id' for local history is the date string we generate in LocalHistoryRepository
                shareTextBuilder.append("Data: ${record["id"]}\n")
                shareTextBuilder.append("  Ingresso: ${formatLongTimestamp(record["enterTime"] as? Long)}\n")
                shareTextBuilder.append("  In pausa: ${formatLongTimestamp(record["toLunchTime"] as? Long)}\n")
                shareTextBuilder.append("  Fine pausa: ${formatLongTimestamp(record["fromLunchTime"] as? Long)}\n")
                shareTextBuilder.append("  Uscita: ${formatLongTimestamp(record["exitTime"] as? Long)}\n")
                shareTextBuilder.append("  Totale lavorato: ${record["totalWorkedTime"] ?: "N/A"}\n")
                shareTextBuilder.append("  Ore giornaliere impostate: ${record["dailyHours"] ?: "N/A"}h\n\n")
            }

            message = "Contenuto dello storico: \n$shareTextBuilder" // Keep this for local log
            Log.d("Condividi", shareTextBuilder.toString())

            try {
                val sendIntent: Intent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, shareTextBuilder.toString())
                    type = "text/plain"
                }
                val shareIntent = Intent.createChooser(sendIntent, "Condividi storico tramite...")
                context.startActivity(shareIntent)
                message = "Lo storico è pronto per essere condiviso!"
            } catch (e: Exception) {
                Log.e("SettingsScreen", "Error sharing history: ${e.message}", e)
                message = "Impossibile avviare la condivisione."
            }
        }
    }

    // --- UI Code (Mostly the same, with minor adjustments for local data) ---
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
                        value = dailyHoursInput.toString(),
                        onValueChange = { newValue ->
                            // Allow up to one decimal place for hours like 7.5
                            val cleanedValue = newValue.filter { it.isDigit() || it == '.' }
                            if (cleanedValue.count { it == '.' } <= 1) {
                                dailyHoursInput = cleanedValue.toDoubleOrNull() ?: 0.0
                            }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color(0xFF9A4616),
                            unfocusedTextColor = Color(0xFF9A4616).copy(alpha = 0.8f),
                            focusedBorderColor = Color(0xFFA03D00),
                            unfocusedBorderColor = Color(0xFF9A4616).copy(alpha = 0.5f),
                            focusedLabelColor = Color(0xFF9A4616),
                            unfocusedLabelColor = Color(0xFF9A4616).copy(alpha = 0.7f),
                            cursorColor = Color(0xFF9A4616),
                        ),
                    )
                    Button(
                        onClick = handleDailyHoursChange,
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
                onClick = shareHistory,
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
                onClick = { showClearConfirmDialog = true }, // Show confirmation dialog
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

        // Message display
        AnimatedVisibility(
            visible = message.isNotBlank(),
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            Text(
                text = message,
                color = if (message.startsWith("Error") || message.startsWith("Errore")) MaterialTheme.colorScheme.error else Color(
                    0xFF9A4616
                ),
                fontSize = 16.sp, // Reduced size for better fit
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        (if (message.startsWith("Error") || message.startsWith("Errore")) MaterialTheme.colorScheme.errorContainer else Color(
                            0xFF9A4616
                        ).copy(alpha = 0.2f))
                    )
                    .padding(12.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }

        Spacer(Modifier.height(16.dp))

        // History List Card
        Card(
            modifier = Modifier.fillMaxSize(),
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
                if (history.isEmpty()) {
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
                        items(history, key = { it["id"].toString() + (it["enterTime"] as? Long ?: 0L) + (it["exitTime"] as? Long ?: 0L) }) { record -> // Added key for stability
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
                                        text = "Ingresso: ${formatLongTimestamp(record["enterTime"] as? Long)}",
                                        color = Color(0xFF9A4616).copy(alpha = 0.8f), fontSize = 14.sp,
                                    )
                                    Text(
                                        text = "In pausa: ${formatLongTimestamp(record["toLunchTime"] as? Long)}",
                                        color = Color(0xFF9A4616).copy(alpha = 0.8f), fontSize = 14.sp,
                                    )
                                    Text(
                                        text = "Fine pausa: ${formatLongTimestamp(record["fromLunchTime"] as? Long)}",
                                        color = Color(0xFF9A4616).copy(alpha = 0.8f), fontSize = 14.sp,
                                    )
                                    Text(
                                        text = "Uscita: ${formatLongTimestamp(record["exitTime"] as? Long)}",
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
    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            title = { Text("Pulisci lo storico") },
            text = { Text("Sei sicuro di voler cancellare tutto lo storico? Questa azione non può essere annullata.") },
            confirmButton = {
                Button(
                    onClick = {
                        clearHistoryAction()
                        showClearConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                ) { Text("Conferma", color = Color.White) }
            },
            dismissButton = {
                Button(onClick = { showClearConfirmDialog = false }) { Text("Annulla") }
            }
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF0EAE2) // Example background color for preview
@Composable
fun SettingsScreenPreview() {
    // Add some dummy data to the local repository for the preview
    LaunchedEffect(Unit) { // Use LaunchedEffect to add data once for the preview
        if (LocalHistoryRepository.history.isEmpty()) { // Add only if empty to avoid duplicates on recomposition
            LocalHistoryRepository.addDummyRecord(
                System.currentTimeMillis() - 86400000 * 2, // 2 days ago
                System.currentTimeMillis() - (86400000 * 2 - 8 * 3600000), // ~8 hours later
                "7h 45m",
                8.0
            )
            LocalHistoryRepository.addDummyRecord(
                System.currentTimeMillis() - 86400000, // yesterday
                System.currentTimeMillis() - (86400000 - 7.5 * 3600000).toLong(), // ~7.5 hours later
                "7h 30m",
                7.5
            )
        }
    }
    MaterialTheme { // Wrap in MaterialTheme for previews to get default styling
        SettingsScreen()
    }
}
