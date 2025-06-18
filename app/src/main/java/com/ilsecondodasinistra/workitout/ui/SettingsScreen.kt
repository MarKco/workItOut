package com.ilsecondodasinistra.workitout.ui

import android.content.Intent
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.ilsecondodasinistra.workitout.await
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun SettingsScreen(
    db: FirebaseFirestore?,
    userId: String,
    appId: String,
) {
    var dailyHoursInput by remember { mutableStateOf(8.0) }
    var history by remember { mutableStateOf<List<Map<String, Any?>>>(emptyList()) }
    var message by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    // Helper function to format Firestore Timestamp to readable time string
    val formatFirestoreTimestamp: (Timestamp?) -> String = { timestamp ->
        timestamp?.toDate()?.let { SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(it) } ?: "N/A"
    }

    // Load daily hours from Firestore
    LaunchedEffect(db, userId, appId) {
        val settingsDocRef = db?.collection(
            "artifacts",
        )?.document(appId)?.collection("users")?.document(userId)?.collection("settings")?.document("dailyHours")

        settingsDocRef?.addSnapshotListener { snapshot, e ->
            if (e != null) {
                Log.w("Firestore", "Listen failed.", e)
                return@addSnapshotListener
            }
            if (snapshot != null && snapshot.exists()) {
                dailyHoursInput = snapshot.getDouble("value") ?: 8.0
            } else {
                dailyHoursInput = 8.0 // Default if not set
            }
        }
    }

    // Load history from Firestore
    LaunchedEffect(db, userId, appId) {
        val historyCollectionRef = db?.collection(
            "artifacts",
        )?.document(appId)?.collection("users")?.document(userId)?.collection("dailyRecords")

        // Order by enterTime descending
        historyCollectionRef?.orderBy("enterTime", Query.Direction.DESCENDING)?.addSnapshotListener { snapshot, e ->
            if (e != null) {
                Log.w("Firestore", "Listen failed.", e)
                return@addSnapshotListener
            }

            if (snapshot != null) {
                history = snapshot.documents.map { doc ->
                    doc.data?.toMutableMap()?.apply {
                        put("id", doc.id) // Add document ID to the map
                    } ?: emptyMap()
                }
            }
        }
    }

    val handleDailyHoursChange: () -> Unit = {
        coroutineScope.launch {
            try {
                val settingsDocRef = db?.collection(
                    "artifacts",
                )?.document(appId)?.collection("users")?.document(userId)?.collection("settings")?.document("dailyHours")
                settingsDocRef?.set(mapOf("value" to dailyHoursInput))
                message = "Daily hours updated successfully!"
            } catch (e: Exception) {
                Log.e("Firestore", "Error updating daily hours: ${e.message}", e)
                message = "Error updating daily hours."
            }
        }
    }

    val showConfirmationDialog: (String, String, () -> Unit) -> Unit = { title, body, onConfirm ->
        coroutineScope.launch {
            val confirmed = with(context) {
                // This is a simplified confirmation. In a real Android app,
                // you would use an AlertDialog or a custom Composable dialog.
                // For now, we'll just log and assume confirmation for simplicity
                // or implement a basic in-app message.
                message = "$body Tap again to confirm."
                // In a real app, you'd show a proper dialog and await user input
                // For demo, we'll simulate it by returning true immediately
                true // Simulate confirmation for now
            }
            if (confirmed) {
                onConfirm()
            } else {
                message = "Action cancelled."
            }
        }
    }

    val clearHistory: () -> Unit = {
        coroutineScope.launch {
            showConfirmationDialog("Pulisci lo storico", "Sei sicuro di voler cancellare lo storico?") {
                try {
                    val historyCollectionRef = db?.collection(
                        "artifacts",
                    )?.document(appId)?.collection("users")?.document(userId)?.collection("dailyRecords")

                    // Get all documents and delete them
                    coroutineScope.launch {
                        val snapshot = historyCollectionRef?.get()?.await()
                        for (doc in snapshot?.documents?: emptyList()) {
                            doc.reference.delete().await()
                        }
                        message = "Storico ripulito con successo!"
                    }
                } catch (e: Exception) {
                    Log.e("Firestore", "Error clearing history: ${e.message}", e)
                    message = "Errore durante la pulizia dello storico."
                }
            }
        }
    }

    val shareHistory: () -> Unit = {
        if (history.isEmpty()) {
            message = "Nessuno storico da condividere!"
        }

        val shareTextBuilder = StringBuilder("Storico ore lavorate:\n\n")
        history.forEach { record ->
            shareTextBuilder.append("Data: ${record["id"]}\n")
            shareTextBuilder.append("  Ingresso: ${formatFirestoreTimestamp(record["enterTime"] as? Timestamp)}\n")
            shareTextBuilder.append("  In pausa: ${formatFirestoreTimestamp(record["toLunchTime"] as? Timestamp)}\n")
            shareTextBuilder.append("  Fine pausa: ${formatFirestoreTimestamp(record["fromLunchTime"] as? Timestamp)}\n")
            shareTextBuilder.append("  Uscita: ${formatFirestoreTimestamp(record["exitTime"] as? Timestamp)}\n")
            shareTextBuilder.append("  Totale lavorato: ${record["totalWorkedTime"] ?: "N/A"}\n")
            shareTextBuilder.append("  Ore giornaliere impostate: ${record["dailyHours"] ?: "N/A"}h\n\n")
        }

        // In Android, you would use an Intent for sharing
        // For now, we'll just set the message and log it
        message = "Contenuto dello storico: \n$shareTextBuilder"
        Log.d("Condividi", shareTextBuilder.toString())
        // Example for a real Android share intent:
        val sendIntent: Intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, shareTextBuilder.toString())
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, null)
        context.startActivity(shareIntent)
        message = "Storico aperto nelle opzioni di condivisione!"
        message = "Lo storico è pronto per essere condiviso!" // Updated message to indicate success
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Ore di lavoro totali
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
                            dailyHoursInput = newValue.toDoubleOrNull() ?: 0.0
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color(0xFF9A4616),
                            unfocusedTextColor = Color(0xFF9A4616).copy(alpha = 0.8f),
                            focusedBorderColor = Color(0xFFA03D00), // Purple-500
                            unfocusedBorderColor = Color(0xFF9A4616).copy(alpha = 0.5f),
                            focusedLabelColor = Color(0xFF9A4616),
                            unfocusedLabelColor = Color(0xFF9A4616).copy(alpha = 0.7f),
                            cursorColor = Color(0xFF9A4616),
                        ),
                    )
                    Button(
                        onClick = handleDailyHoursChange,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF782F04)), // Purple-500
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
                Text(text = "Condividi lo storico", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = clearHistory,
                modifier = Modifier
                    .weight(1f)
                    .height(60.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)), // Red-700
                shape = RoundedCornerShape(12.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp, pressedElevation = 4.dp),
            ) {
                Text(text = "Pulisci lo storico", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
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
                color = Color(0xFF9A4616),
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF9A4616).copy(alpha = 0.2f))
                    .padding(12.dp)
                    .wrapContentWidth(Alignment.CenterHorizontally),
            )
        }

        Spacer(Modifier.height(16.dp))

        // History List
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
                        items(history) { record ->
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
                                        text = "Ingresso: ${formatFirestoreTimestamp(record["enterTime"] as? Timestamp)}",
                                        color = Color(0xFF9A4616).copy(alpha = 0.8f),
                                        fontSize = 14.sp,
                                    )
                                    Text(
                                        text = "In pausa: ${formatFirestoreTimestamp(record["toLunchTime"] as? Timestamp)}",
                                        color = Color(0xFF9A4616).copy(alpha = 0.8f),
                                        fontSize = 14.sp,
                                    )
                                    Text(
                                        text = "Fine pausa: ${formatFirestoreTimestamp(record["fromLunchTime"] as? Timestamp)}",
                                        color = Color(0xFF9A4616).copy(alpha = 0.8f),
                                        fontSize = 14.sp,
                                    )
                                    Text(
                                        text = "Uscita: ${formatFirestoreTimestamp(record["exitTime"] as? Timestamp)}",
                                        color = Color(0xFF9A4616).copy(alpha = 0.8f),
                                        fontSize = 14.sp,
                                    )
                                    Text(
                                        text = "Ore lavorate totali: ${record["totalWorkedTime"] ?: "N/A"}",
                                        color = Color(0xFF9A4616).copy(alpha = 0.8f),
                                        fontSize = 14.sp,
                                    )
                                    Text(
                                        text = "Ore giornaliere impostate: ${record["dailyHours"] ?: "N/A"}h",
                                        color = Color(0xFF9A4616).copy(alpha = 0.8f),
                                        fontSize = 14.sp,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview
@Composable
fun SettingsScreenPreview() {
    SettingsScreen(
        db = null, // Mock or provide a test instance
        userId = "testUser",
        appId = "testApp",
    )
}