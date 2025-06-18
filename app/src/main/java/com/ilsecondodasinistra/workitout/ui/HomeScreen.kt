package com.ilsecondodasinistra.workitout.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
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
import androidx.compose.ui.tooling.preview.Preview
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
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    db: FirebaseFirestore,
    userId: String,
    appId: String,
) {
    Log.d("HomeScreen", "Initializing HomeScreen")

    var enterTime by remember { mutableStateOf<Date?>(null) }
    var toLunchTime by remember { mutableStateOf<Date?>(null) }
    var fromLunchTime by remember { mutableStateOf<Date?>(null) }
    var exitTime by remember { mutableStateOf<Date?>(null) }
    var calculatedExitTime by remember { mutableStateOf<Date?>(null) }
    var totalWorkedTime by remember { mutableStateOf<String?>(null) }
    var dailyHours by remember { mutableDoubleStateOf(8.0) }
    var message by remember { mutableStateOf("") }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val timePickerState = rememberTimePickerState(
        initialHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY),
        initialMinute = Calendar.getInstance().get(Calendar.MINUTE),
        is24Hour = true,
    )

    val timePickerStateDialog = remember {
        mutableStateOf(
            TimePickerDialogState(
                state = timePickerState,
                type = ButtonType.Enter,
                isVisible = false
            )
        )
    }

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
            } else {
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
            db?.collection("artifacts")
                ?.document(appId)
                ?.collection("users")
                ?.document(userId)
                ?.collection("dailyRecords")
                ?.document(todayDocId)

        val settingsDocRef =
            db
                ?.collection(
                    "artifacts",
                )
                ?.document(appId)
                ?.collection("users")
                ?.document(userId)
                ?.collection("settings")
                ?.document("dailyHours")

        // Listen for daily record changes
        dailyRecordRef?.addSnapshotListener { snapshot, e ->
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
        settingsDocRef?.addSnapshotListener { snapshot, e ->
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

    val handleButtonPress: (ButtonType, Date) -> Unit = { buttonType, dateTime ->
        message = "" // Clear previous messages

        val todayDocId = formatDateForDocId(dateTime)
        val dailyRecordRef =
            db
                ?.collection(
                    "artifacts",
                )
                ?.document(appId)
                ?.collection("users")
                ?.document(userId)
                ?.collection("dailyRecords")
                ?.document(todayDocId)

        coroutineScope.launch {
            try {
                // Get current data to merge correctly
                val currentData = dailyRecordRef?.get()?.await()?.data ?: mutableMapOf()

                when (buttonType) {
                    ButtonType.Enter -> {
                        currentData["enterTime"] = dateTime
                        currentData["exitTime"] = null // Reset exit time if re-entering
                        currentData["fromLunchTime"] = null // Reset exit time if re-entering
                        currentData["toLunchTime"] = null // Reset exit time if re-entering
                        currentData["totalWorkedTime"] = null // Reset total worked time
                        message = "Entered work."
                    }

                    ButtonType.ToLunch -> {
                        if (enterTime == null) {
                            message = "Devi prima entrare in servizio!"
                            return@launch
                        }
                        currentData["toLunchTime"] = dateTime
                        message = "A pranzo."
                    }

                    ButtonType.FromLunch -> {
                        if (toLunchTime == null) {
                            message = "Ricordati di premere prima \"In pausa\"!"
                            return@launch
                        }
                        currentData["fromLunchTime"] = dateTime
                        message = "Ritorno dalla pausa."
                    }

                    ButtonType.Exit -> {
                        if (enterTime == null) {
                            message = "Devi prima entrare in servizio!"
                            return@launch
                        }
                        currentData["exitTime"] = dateTime
                    }
                }

                // Update calculatedExitTime and totalWorkedTime just before saving if relevant
                // These are recalculated in the LaunchedEffect based on the loaded state,
                // but setting them here ensures they are part of the transaction.
                calculatedExitTime?.let { currentData["calculatedExitTime"] = it }
                totalWorkedTime?.let { currentData["totalWorkedTime"] = it }

                dailyRecordRef?.set(currentData) // Use set with merge implicitly
//                message = "Dati salvati con successo!"
            } catch (e: Exception) {
                Log.e("Firestore", "Error saving data: ${e.message}", e)
                message = "Errore nel salvataggio: ${e.message}"
            }
        }
    }

    val handleEditClick: (ButtonType) -> Unit = { editKey ->
        when (editKey) {
            ButtonType.Enter -> {
                timePickerState.minute = enterTime?.let { Calendar.getInstance().apply { time = it }.get(Calendar.MINUTE) } ?: 0
                timePickerState.hour = enterTime?.let { Calendar.getInstance().apply { time = it }.get(Calendar.HOUR_OF_DAY) } ?: 0
                timePickerStateDialog.value = TimePickerDialogState(
                    state = timePickerState,
                    type = ButtonType.Enter,
                    isVisible = true,
                )
            }

            ButtonType.ToLunch -> {
                timePickerState.minute = toLunchTime?.let { Calendar.getInstance().apply { time = it }.get(Calendar.MINUTE) } ?: 0
                timePickerState.hour = toLunchTime?.let { Calendar.getInstance().apply { time = it }.get(Calendar.HOUR_OF_DAY) } ?: 0
                timePickerStateDialog.value = TimePickerDialogState(
                    state = timePickerState,
                    type = ButtonType.ToLunch,
                    isVisible = true,
                )
            }

            ButtonType.FromLunch -> {
                timePickerState.minute = fromLunchTime?.let { Calendar.getInstance().apply { time = it }.get(Calendar.MINUTE) } ?: 0
                timePickerState.hour = fromLunchTime?.let { Calendar.getInstance().apply { time = it }.get(Calendar.HOUR_OF_DAY) } ?: 0
                timePickerStateDialog.value = TimePickerDialogState(
                    state = timePickerState,
                    type = ButtonType.FromLunch,
                    isVisible = true,
                )
            }

            ButtonType.Exit -> {
                timePickerState.minute = exitTime?.let { Calendar.getInstance().apply { time = it }.get(Calendar.MINUTE) } ?: 0
                timePickerState.hour = exitTime?.let { Calendar.getInstance().apply { time = it }.get(Calendar.HOUR_OF_DAY) } ?: 0
                timePickerStateDialog.value = TimePickerDialogState(
                    state = timePickerState,
                    type = ButtonType.Exit,
                    isVisible = true,
                )
            }
        }
    }

    Box {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                modifier = Modifier
                    .wrapContentWidth()
                    .weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceAround,
            ) {
                TimeButton(
                    text = ButtonType.Enter.text,
                    time = formatTime(enterTime),
                    buttonColor = Color(0xFF4CAF50), // Green-500
                    onClick = { handleButtonPress(ButtonType.Enter, Date()) },
                    onEditClick = { handleEditClick(ButtonType.Enter) },
                )
                TimeButton(
                    text = ButtonType.ToLunch.text,
                    time = formatTime(toLunchTime),
                    buttonColor = Color(0xFFFFC107), // Yellow-500
                    onClick = { handleButtonPress(ButtonType.ToLunch, Date()) },
                    onEditClick = { handleEditClick(ButtonType.ToLunch) },
                )
                TimeButton(
                    text = ButtonType.FromLunch.text,
                    time = formatTime(fromLunchTime),
                    buttonColor = Color(0xFF2196F3), // Blue-500
                    onClick = { handleButtonPress(ButtonType.FromLunch, Date()) },
                    onEditClick = { handleEditClick(ButtonType.FromLunch) },
                )
                TimeButton(
                    text = ButtonType.Exit.text,
                    time = formatTime(exitTime),
                    buttonColor = Color(0xFFF44336), // Red-500
                    onClick = { handleButtonPress(ButtonType.Exit, Date()) },
                    onEditClick = { handleEditClick(ButtonType.Exit) },
                )
            }

            Spacer(Modifier.height(16.dp))

            // Info display
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.30f))
                        .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "Orario giornaliero: ${dailyHours}h",
                    color = Color(0xFF9A4616),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                Text(
                    text = "Orario di uscita stimato: ${formatTime(calculatedExitTime)}",
                    color = Color(0xFF9A4616),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                Text(
                    text = "Totale ore di oggi: ${totalWorkedTime ?: "N/A"}",
                    color = Color(0xFF9A4616),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }

        }

        if (timePickerStateDialog.value.isVisible) {
            TimePickerDialog(
                onDismiss = {
                    timePickerStateDialog.value = TimePickerDialogState(
                        state = timePickerState,
                        isVisible = false
                    )
                },
                onConfirm = {
                    when (timePickerStateDialog.value.type) {
                        ButtonType.Enter -> {
                            val calendar = Calendar.getInstance()
                            calendar.set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                            calendar.set(Calendar.MINUTE, timePickerState.minute)
                            enterTime = calendar.time
                            message = "Orario di ingresso aggiornato a ${formatTime(enterTime)}."
                            enterTime?.let {
                                handleButtonPress(ButtonType.Enter, it) // Save the updated time
                            }
                            timePickerStateDialog.value = TimePickerDialogState(
                                state = timePickerState,
                                isVisible = false
                            )
                        }

                        ButtonType.ToLunch -> {
                            val calendar = Calendar.getInstance()
                            calendar.set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                            calendar.set(Calendar.MINUTE, timePickerState.minute)
                            toLunchTime = calendar.time
                            message = "Orario di inizio pausa aggiornato a ${formatTime(toLunchTime)}."
                            enterTime?.let {
                                handleButtonPress(ButtonType.ToLunch, it) // Save the updated time
                            }
                            timePickerStateDialog.value = TimePickerDialogState(
                                state = timePickerState,
                                isVisible = false
                            )
                        }

                        ButtonType.FromLunch -> {
                            val calendar = Calendar.getInstance()
                            calendar.set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                            calendar.set(Calendar.MINUTE, timePickerState.minute)
                            fromLunchTime = calendar.time
                            message = "Orario di fine pausa aggiornato a ${formatTime(fromLunchTime)}."
                            enterTime?.let {
                                handleButtonPress(ButtonType.FromLunch, it) // Save the updated time
                            }
                            timePickerStateDialog.value = TimePickerDialogState(
                                state = timePickerState,
                                isVisible = false
                            )
                        }

                        ButtonType.Exit -> {
                            val calendar = Calendar.getInstance()
                            calendar.set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                            calendar.set(Calendar.MINUTE, timePickerState.minute)
                            exitTime = calendar.time
                            message = "Orario di uscita aggiornato a ${formatTime(enterTime)}."
                            enterTime?.let {
                                handleButtonPress(ButtonType.Exit, it) // Save the updated time
                            }
                            timePickerStateDialog.value = TimePickerDialogState(
                                state = timePickerState,
                                isVisible = false
                            )
                        }

                        else -> {
                            Log.w("TimePicker", "Unknown button type: ${timePickerStateDialog.value.type}")
                        }
                    }
                }
            ) {
                TimePicker(
                    state = timePickerState,
                )
            }
        }

        // If message is not empty, display it as a Toast
        if (message.isNotEmpty()) {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }
}

private class TimePickerDialogState @OptIn(ExperimentalMaterial3Api::class) constructor(
    val state: TimePickerState,
    val type: ButtonType? = null,
    val isVisible: Boolean,
)

sealed class ButtonType(val text: String) {
    object Enter : ButtonType("Ingresso")
    object ToLunch : ButtonType("In pausa")
    object FromLunch : ButtonType("Fine pausa")
    object Exit : ButtonType("Uscita")
}

@Preview
@Composable
fun HomeScreenPreview() {
    HomeScreen(
        userId = "testUser",
        appId = "testApp",
        db = FirebaseFirestore.getInstance()
    )
}