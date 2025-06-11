package com.ilsecondodasinistra.workitout.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.ilsecondodasinistra.workitout.NOTIFICATION_CHANNEL_ID
import com.ilsecondodasinistra.workitout.NOTIFICATION_ID
import com.ilsecondodasinistra.workitout.await
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.collections.set

@Composable
fun HomeScreen(
    db: FirebaseFirestore,
    userId: String,
    appId: String,
) {
    var enterTime by remember { mutableStateOf<Date?>(null) }
    var toLunchTime by remember { mutableStateOf<Date?>(null) }
    var fromLunchTime by remember { mutableStateOf<Date?>(null) }
    var exitTime by remember { mutableStateOf<Date?>(null) }
    var calculatedExitTime by remember { mutableStateOf<Date?>(null) }
    var totalWorkedTime by remember { mutableStateOf<String?>(null) }
    var dailyHours by remember { mutableStateOf(8.0) }
    var message by remember { mutableStateOf("") }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Helper function to format a Date object to a readable time string
    val formatTime: (Date?) -> String = { date ->
        date?.let { SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(it) } ?: "N/A"
    }

    // Helper function to format a Date object to YYYY-MM-DD string
    val formatDateForDocId: (Date) -> String = { date ->
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(date)
    }

    // Show a local notification
    val showLocalNotification: (String, String) -> Unit = { title, body ->
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            val builder =
                NotificationCompat
                    .Builder(context, NOTIFICATION_CHANNEL_ID)
                    .setSmallIcon(android.R.drawable.ic_lock_idle_alarm) // Simple icon
                    .setContentTitle(title)
                    .setContentText(body)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)

            with(NotificationManagerCompat.from(context)) {
                notify(NOTIFICATION_ID, builder.build())
            }
        } else {
            message = "🔔 Notifica: $body" // Fallback to in-app message
            // Ask for notification permission if not granted
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ActivityCompat.requestPermissions(
                    context as androidx.activity.ComponentActivity,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    0,
                )
            }
            else {
                Log.w("Notification", "POST_NOTIFICATIONS permission not granted.")
            }
        }
    }

    // Calculate the expected exit time
    val calculateExpectedExitTime: () -> Unit = {
        if (enterTime != null && dailyHours > 0) {
            val enterMs = enterTime!!.time
            val totalWorkMilliseconds = (dailyHours * 60 * 60 * 1000).toLong()
            var lunchBreakMilliseconds = 0L

            if (toLunchTime != null && fromLunchTime != null) {
                lunchBreakMilliseconds = fromLunchTime!!.time - toLunchTime!!.time
            } else if (toLunchTime != null && fromLunchTime == null) {
                // Assume 1 hour lunch if "To Lunch" is pressed but not "From Lunch"
                lunchBreakMilliseconds = 60 * 60 * 1000L
            }

            val newCalculatedExitMs = enterMs + totalWorkMilliseconds + lunchBreakMilliseconds
            val newCalculatedExitTime = Date(newCalculatedExitMs)
            calculatedExitTime = newCalculatedExitTime

            if (newCalculatedExitTime.time <= System.currentTimeMillis()) {
                showLocalNotification("Ora di uscire!", "Il tuo orario previsto di uscita è ${formatTime(newCalculatedExitTime)}.")
            }
        } else {
            calculatedExitTime = null
        }
    }

    // Calculate total worked time
    val calculateTotalWorkedTime: () -> Unit = {
        if (enterTime != null && exitTime != null) {
            var totalMs = exitTime!!.time - enterTime!!.time
            var lunchMs = 0L

            if (toLunchTime != null && fromLunchTime != null) {
                lunchMs = fromLunchTime!!.time - toLunchTime!!.time
            }
            totalMs -= lunchMs

            val totalHours = TimeUnit.MILLISECONDS.toHours(totalMs)
            val totalMinutes = TimeUnit.MILLISECONDS.toMinutes(totalMs) % 60
            val totalSeconds = TimeUnit.MILLISECONDS.toSeconds(totalMs) % 60

            totalWorkedTime = "${totalHours}h ${totalMinutes}m ${totalSeconds}s"
        } else {
            totalWorkedTime = null
        }
    }

    // Load today's data and daily hours from Firestore
    LaunchedEffect(db, userId, appId) {
        val todayDocId = formatDateForDocId(Date())
        val dailyRecordRef =
            db
                .collection(
                    "artifacts",
                ).document(appId)
                .collection("users")
                .document(userId)
                .collection("dailyRecords")
                .document(todayDocId)

        val settingsDocRef =
            db
                .collection(
                    "artifacts",
                ).document(appId)
                .collection("users")
                .document(userId)
                .collection("settings")
                .document("dailyHours")

        // Listen for daily record changes
        dailyRecordRef.addSnapshotListener { snapshot, e ->
            if (e != null) {
                Log.w("Firestore", "Listen failed.", e)
                return@addSnapshotListener
            }

            if (snapshot != null && snapshot.exists()) {
                val data = snapshot.data
                enterTime = (data?.get("enterTime") as? Timestamp)?.toDate()
                toLunchTime = (data?.get("toLunchTime") as? Timestamp)?.toDate()
                fromLunchTime = (data?.get("fromLunchTime") as? Timestamp)?.toDate()
                exitTime = (data?.get("exitTime") as? Timestamp)?.toDate()
                calculatedExitTime = (data?.get("calculatedExitTime") as? Timestamp)?.toDate()
                totalWorkedTime = data?.get("totalWorkedTime") as? String
            } else {
                Log.d("Firestore", "Current daily record: null")
                // Reset states if no record for today
                enterTime = null
                toLunchTime = null
                fromLunchTime = null
                exitTime = null
                calculatedExitTime = null
                totalWorkedTime = null
            }
            calculateExpectedExitTime() // Recalculate after data load
            calculateTotalWorkedTime()
        }

        // Listen for settings changes (daily hours)
        settingsDocRef.addSnapshotListener { snapshot, e ->
            if (e != null) {
                Log.w("Firestore", "Listen failed.", e)
                return@addSnapshotListener
            }

            if (snapshot != null && snapshot.exists()) {
                dailyHours = snapshot.getDouble("value") ?: 8.0
            } else {
                dailyHours = 8.0 // Default if not set
            }
            calculateExpectedExitTime() // Recalculate if daily hours change
        }

        // Polling to check for exit time notification every second
        while (true) {
            delay(1000L) // Wait for 1 second
            if (calculatedExitTime != null && calculatedExitTime!!.time <= System.currentTimeMillis()) {
                showLocalNotification("Ora di uscire!", "Il tuo orario di uscita è ${formatTime(calculatedExitTime)}.")
                calculatedExitTime = null // To prevent repeated notifications
            }
        }
    }

    // Effect to recalculate when dependencies change
    LaunchedEffect(enterTime, toLunchTime, fromLunchTime, exitTime, dailyHours) {
        calculateExpectedExitTime()
        calculateTotalWorkedTime()
    }

    val handleButtonPress: (String) -> Unit = { buttonType ->
        val now = Date()
        message = "" // Clear previous messages

        val todayDocId = formatDateForDocId(now)
        val dailyRecordRef =
            db
                .collection(
                    "artifacts",
                ).document(appId)
                .collection("users")
                .document(userId)
                .collection("dailyRecords")
                .document(todayDocId)

        coroutineScope.launch {
            try {
                // Get current data to merge correctly
                val currentData = dailyRecordRef.get().await().data ?: mutableMapOf()

                when (buttonType) {
                    "Ingresso" -> {
                        currentData["enterTime"] = now
                        currentData["exitTime"] = null // Reset exit time if re-entering
                        currentData["fromLunchTime"] = null // Reset exit time if re-entering
                        currentData["toLunchTime"] = null // Reset exit time if re-entering
                        currentData["totalWorkedTime"] = null // Reset total worked time
                        message = "Entered work."
                    }

                    "In pausa" -> {
                        if (enterTime == null) {
                            message = "Devi prima entrare in servizio!"
                            return@launch
                        }
                        currentData["toLunchTime"] = now
                        message = "A pranzo."
                    }

                    "Fine pausa" -> {
                        if (toLunchTime == null) {
                            message = "Ricordati di premere prima \"In pausa\"!"
                            return@launch
                        }
                        currentData["fromLunchTime"] = now
                        message = "Ritorno dalla pausa."
                    }

                    "Uscita" -> {
                        if (enterTime == null) {
                            message = "Devi prima entrare in servizio!"
                            return@launch
                        }
                        currentData["exitTime"] = now
                        message = "Calcolo del tempo totale..."
                    }
                }

                // Update calculatedExitTime and totalWorkedTime just before saving if relevant
                // These are recalculated in the LaunchedEffect based on the loaded state,
                // but setting them here ensures they are part of the transaction.
                calculatedExitTime?.let { currentData["calculatedExitTime"] = it }
                totalWorkedTime?.let { currentData["totalWorkedTime"] = it }

                dailyRecordRef.set(currentData) // Use set with merge implicitly
                message = "Dati salvati con successo!"
            } catch (e: Exception) {
                Log.e("Firestore", "Error saving data: ${e.message}", e)
                message = "Errore nel salvataggio: ${e.message}"
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Buttons
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceAround,
            ) {
                TimeButton(
                    text = "Ingresso",
                    time = formatTime(enterTime),
                    buttonColor = Color(0xFF4CAF50), // Green-500
                    onClick = { handleButtonPress("Ingresso") },
                )
                Spacer(Modifier.width(8.dp))
                TimeButton(
                    text = "In pausa",
                    time = formatTime(toLunchTime),
                    buttonColor = Color(0xFFFFC107), // Yellow-500
                    onClick = { handleButtonPress("In pausa") },
                )
            }
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceAround,
            ) {
                TimeButton(
                    text = "Fine pausa",
                    time = formatTime(fromLunchTime),
                    buttonColor = Color(0xFF2196F3), // Blue-500
                    onClick = { handleButtonPress("Fine pausa") },
                )
                Spacer(Modifier.width(8.dp))
                TimeButton(
                    text = "Uscita",
                    time = formatTime(exitTime),
                    buttonColor = Color(0xFFF44336), // Red-500
                    onClick = { handleButtonPress("Uscita") },
                )
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
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White.copy(alpha = 0.2f))
                        .padding(12.dp)
                        .wrapContentWidth(Alignment.CenterHorizontally),
            )
        }

        Spacer(Modifier.height(16.dp))

        // Info display
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.15f))
                    .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Orario giornaliero: ${dailyHours}h",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            Text(
                text = "Orario di uscita stimato: ${formatTime(calculatedExitTime)}",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            Text(
                text = "Totale ore di oggi: ${totalWorkedTime ?: "N/A"}",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}