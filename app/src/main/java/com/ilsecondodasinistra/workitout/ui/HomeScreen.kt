package com.ilsecondodasinistra.workitout.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.ilsecondodasinistra.workitout.NOTIFICATION_CHANNEL_ID
import com.ilsecondodasinistra.workitout.NOTIFICATION_ID
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

// Constants for SharedPreferences (ensure these are consistent with SettingsScreen)
private const val PREFS_NAME = "workitout_settings_prefs"
private const val KEY_DAILY_HOURS = "daily_hours"
private const val KEY_CURRENT_DAY_PREFIX = "current_day_"
private const val KEY_ENTER_TIME = "enter_time"
private const val KEY_TO_LUNCH_TIME = "to_lunch_time"
private const val KEY_FROM_LUNCH_TIME = "from_lunch_time"
// No need to save exit_time for current day, as exiting completes it.

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen() {
    Log.d("HomeScreen", "Initializing HomeScreen (Local Storage Version)")
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val sharedPreferences = remember { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }

    // State for the current day's activities
    var enterTime by remember { mutableStateOf<Date?>(null) }
    var toLunchTime by remember { mutableStateOf<Date?>(null) }
    var fromLunchTime by remember { mutableStateOf<Date?>(null) }
    var exitTime by remember { mutableStateOf<Date?>(null) } // This will be set on exit, then record is saved
    var calculatedExitTime by remember { mutableStateOf<Date?>(null) }
    var totalWorkedTime by remember { mutableStateOf<String?>(null) } // For the current day
    var dailyHours by remember { mutableDoubleStateOf(8.0) }
    var message by remember { mutableStateOf("") }


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

    // --- Helper Functions (mostly unchanged) ---
    val formatTime: (Date?) -> String = { date ->
        date?.let { SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(it) } ?: "N/A"
    }

    val showLocalNotification: (String, String) -> Unit = { title, body ->
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            val builder = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle(title)
                .setContentText(body)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            with(NotificationManagerCompat.from(context)) {
                notify(NOTIFICATION_ID, builder.build())
            }
        } else {
            message = "🔔 Notifica: $body"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // Consider a more user-friendly way to request this, perhaps a dedicated button
                (context as? androidx.activity.ComponentActivity)?.let { activity ->
                    ActivityCompat.requestPermissions(
                        activity,
                        arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                        0,
                    )
                }

            } else {
                Log.w("Notification", "POST_NOTIFICATIONS permission not granted.")
            }
        }
    }

    val calculateExpectedExitTime: () -> Unit = {
        if (enterTime != null && dailyHours > 0) {
            val enterMs = enterTime!!.time
            val totalWorkMilliseconds = (dailyHours * 60 * 60 * 1000).toLong()
            var lunchBreakMilliseconds = 0L

            if (toLunchTime != null && fromLunchTime != null && fromLunchTime!!.after(toLunchTime!!)) {
                lunchBreakMilliseconds = fromLunchTime!!.time - toLunchTime!!.time
            } else if (toLunchTime != null && fromLunchTime == null) {
                // Assume 1 hour default if "To Lunch" is pressed but not "From Lunch" when calculating
                // This could be configurable or removed if strict "From Lunch" is required.
                // lunchBreakMilliseconds = 60 * 60 * 1000L // Let's remove this assumption for now
            }

            val newCalculatedExitMs = enterMs + totalWorkMilliseconds + lunchBreakMilliseconds
            calculatedExitTime = Date(newCalculatedExitMs)

            // Notification for calculated exit time is handled by the polling LaunchedEffect
        } else {
            calculatedExitTime = null
        }
    }

    val calculateTotalWorkedTime: () -> String? = { // Returns String? to be assigned
        if (enterTime != null && exitTime != null && exitTime!!.after(enterTime!!)) {
            var totalMs = exitTime!!.time - enterTime!!.time
            var lunchMs = 0L

            if (toLunchTime != null && fromLunchTime != null && fromLunchTime!!.after(toLunchTime!!)) {
                lunchMs = fromLunchTime!!.time - toLunchTime!!.time
            }
            totalMs -= lunchMs
            if (totalMs < 0) totalMs = 0 // Ensure non-negative

            val totalHours = TimeUnit.MILLISECONDS.toHours(totalMs)
            val totalMinutes = TimeUnit.MILLISECONDS.toMinutes(totalMs) % 60
            // val totalSeconds = TimeUnit.MILLISECONDS.toSeconds(totalMs) % 60 // Usually not needed for total

            "${totalHours}h ${totalMinutes}m" // Return the formatted string
        } else {
            null // Return null if conditions not met
        }
    }

    // --- Data Loading and Saving Logic (Local) ---

    // Function to save current day's progress (except exit)
    fun saveCurrentDayProgress() {
        with(sharedPreferences.edit()) {
            enterTime?.let { putLong("${KEY_CURRENT_DAY_PREFIX}$KEY_ENTER_TIME", it.time) } ?: remove("${KEY_CURRENT_DAY_PREFIX}$KEY_ENTER_TIME")
            toLunchTime?.let { putLong("${KEY_CURRENT_DAY_PREFIX}$KEY_TO_LUNCH_TIME", it.time) } ?: remove("${KEY_CURRENT_DAY_PREFIX}$KEY_TO_LUNCH_TIME")
            fromLunchTime?.let { putLong("${KEY_CURRENT_DAY_PREFIX}$KEY_FROM_LUNCH_TIME", it.time) } ?: remove("${KEY_CURRENT_DAY_PREFIX}$KEY_FROM_LUNCH_TIME")
            apply()
        }
        Log.d("HomeScreen", "Current day progress saved to SharedPreferences.")
    }

    // Function to clear current day's progress (e.g., after saving to history)
    fun clearCurrentDayProgressFromPrefs() {
        with(sharedPreferences.edit()) {
            remove("${KEY_CURRENT_DAY_PREFIX}$KEY_ENTER_TIME")
            remove("${KEY_CURRENT_DAY_PREFIX}$KEY_TO_LUNCH_TIME")
            remove("${KEY_CURRENT_DAY_PREFIX}$KEY_FROM_LUNCH_TIME")
            apply()
        }
        Log.d("HomeScreen", "Current day progress cleared from SharedPreferences.")
    }


    // Load daily hours from SharedPreferences and potentially resume today's state
    LaunchedEffect(Unit) { // Run once on init
        dailyHours = sharedPreferences.getFloat(KEY_DAILY_HOURS, 8.0f).toDouble()
        Log.d("HomeScreen", "Loaded dailyHours: $dailyHours")

        // Attempt to load current day's progress
        val savedEnterTime = sharedPreferences.getLong("${KEY_CURRENT_DAY_PREFIX}$KEY_ENTER_TIME", -1L)
        val savedToLunchTime = sharedPreferences.getLong("${KEY_CURRENT_DAY_PREFIX}$KEY_TO_LUNCH_TIME", -1L)
        val savedFromLunchTime = sharedPreferences.getLong("${KEY_CURRENT_DAY_PREFIX}$KEY_FROM_LUNCH_TIME", -1L)

        if (savedEnterTime != -1L) {
            enterTime = Date(savedEnterTime)
            if (savedToLunchTime != -1L) toLunchTime = Date(savedToLunchTime)
            if (savedFromLunchTime != -1L) fromLunchTime = Date(savedFromLunchTime)
//            message = "Giorno lavorativo ripreso."
            Log.d("HomeScreen", "Resumed day: Enter=$enterTime, ToLunch=$toLunchTime, FromLunch=$fromLunchTime")
        }


        calculateExpectedExitTime()
        // totalWorkedTime is only calculated when exitTime is set
    }


    // Polling for exit time notification
    LaunchedEffect(calculatedExitTime) { // Re-run if calculatedExitTime changes
        if (calculatedExitTime == null) return@LaunchedEffect // Stop if no exit time

        var notified = false // Prevent multiple notifications for the same exit time
        while (isActive && calculatedExitTime != null && !notified) { // Use isActive from CoroutineScope
            if (System.currentTimeMillis() >= calculatedExitTime!!.time) {
                showLocalNotification(
                    "Ora di uscire!",
                    "Il tuo orario di uscita previsto è ${formatTime(calculatedExitTime)}."
                )
                notified = true // Mark as notified
                // Optionally, clear calculatedExitTime here if you don't want it to persist visually after notification
                // calculatedExitTime = null
            }
            delay(10000L) // Check every 10 seconds to reduce frequency
        }
    }


    // Effect to recalculate when dependencies change
    LaunchedEffect(enterTime, toLunchTime, fromLunchTime, exitTime, dailyHours) {
        calculateExpectedExitTime()
        if (exitTime != null) { // Only calculate total if exitTime is set
            totalWorkedTime = calculateTotalWorkedTime()
        } else {
            totalWorkedTime = null // Clear if exitTime is not set
        }
        // Save progress whenever these key times change (except exit, which completes the day)
        if (exitTime == null) { // Don't save if exiting, as that's a separate "complete day" logic
            saveCurrentDayProgress()
        }
    }

    // Function to save timings to SharedPreferences (for history)
    fun saveTimings(finalTotalWorked: String) {
        // Save to LocalHistoryRepository
        val record = mapOf(
            "enterTime" to enterTime?.time, // Store as Long
            "toLunchTime" to toLunchTime?.time,
            "fromLunchTime" to fromLunchTime?.time,
            "exitTime" to exitTime?.time,
            "totalWorkedTime" to finalTotalWorked,
            "dailyHours" to dailyHours
            // 'id' will be generated by LocalHistoryRepository
        )
        LocalHistoryRepository.addRecord(record)
        Log.d("HomeScreen", "Day record saved to LocalHistoryRepository: $record")

//        // Clear current day's state after successful exit and save
//        enterTime = null
//        toLunchTime = null
//        fromLunchTime = null
//        exitTime = null
//        // totalWorkedTime is already set for display, will clear on next entry
//        calculatedExitTime = null // Clear calculated as well
    }

    val handleButtonPress: (ButtonType, Date) -> Unit = { buttonType, dateTime ->
        message = "" // Clear previous messages

        when (buttonType) {
            ButtonType.Enter -> {
                // If already entered and not exited, perhaps ask for confirmation to reset?
                // For now, allow reset.
                clearCurrentDayProgressFromPrefs() // Clear from SharedPreferences
                enterTime = dateTime
                toLunchTime = null
                fromLunchTime = null
                exitTime = null
                totalWorkedTime = null
                message = "Ingresso registrato: ${formatTime(enterTime)}"
            }

            ButtonType.ToLunch -> {
                if (enterTime == null) {
                    message = "Devi prima registrare l'ingresso!"

                } else if (toLunchTime != null && fromLunchTime == null) {
                    message = "Sei già in pausa. Registra il rientro."
                }
                toLunchTime = dateTime
                message = "Inizio pausa: ${formatTime(toLunchTime)}"
            }

            ButtonType.FromLunch -> {
                if (toLunchTime == null) {
                    message = "Devi prima registrare l'inizio della pausa!"
                } else if (dateTime.before(toLunchTime)) {
                    message = "Il rientro non può essere prima dell'inizio pausa!"
                }
                fromLunchTime = dateTime
                message = "Fine pausa: ${formatTime(fromLunchTime)}"
            }

            ButtonType.Exit -> {
                if (enterTime == null) {
                    message = "Devi prima registrare l'ingresso!"
                } else {
                    exitTime = dateTime
                    val finalTotalWorked = calculateTotalWorkedTime() // Calculate one last time
                    totalWorkedTime = finalTotalWorked
                    message = "Uscita registrata: ${formatTime(exitTime)}. Totale: $finalTotalWorked"

                    finalTotalWorked?.let {
                        saveTimings(it)
                    }
                }
            }
        }
        // Calculations are handled by LaunchedEffect
    }

    val handleEditClick: (ButtonType) -> Unit = { buttonType ->
        val calendar = Calendar.getInstance()
        val timeToEdit: Date? = when (buttonType) {
            ButtonType.Enter -> enterTime
            ButtonType.ToLunch -> toLunchTime
            ButtonType.FromLunch -> fromLunchTime
            ButtonType.Exit -> exitTime
        }

        timeToEdit?.let { calendar.time = it }
        // If null, use current time as default for picker
        timePickerState.hour = calendar.get(Calendar.HOUR_OF_DAY)
        timePickerState.minute = calendar.get(Calendar.MINUTE)

        timePickerStateDialog.value = TimePickerDialogState(
            state = timePickerState,
            type = buttonType,
            isVisible = true
        )
    }


    // --- UI (Largely Unchanged) ---
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                modifier = Modifier
                    .wrapContentWidth()
                    .weight(1f)
                    .padding(top = 8.dp, bottom = 8.dp), // Added padding
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween,
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
                    enabled = enterTime != null // Enable only if entered
                )
                TimeButton(
                    text = ButtonType.FromLunch.text,
                    time = formatTime(fromLunchTime),
                    buttonColor = Color(0xFF2196F3), // Blue-500
                    onClick = { handleButtonPress(ButtonType.FromLunch, Date()) },
                    onEditClick = { handleEditClick(ButtonType.FromLunch) },
                    enabled = toLunchTime != null // Enable only if on lunch
                )
                TimeButton(
                    text = ButtonType.Exit.text,
                    time = formatTime(exitTime),
                    buttonColor = Color(0xFFF44336), // Red-500
                    onClick = { handleButtonPress(ButtonType.Exit, Date()) },
                    onEditClick = { handleEditClick(ButtonType.Exit) },
                    enabled = enterTime != null // Enable only if entered
                )
            }

            Spacer(Modifier.height(16.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp) // Added horizontal padding
                    .clip(RoundedCornerShape(16.dp)) // Increased rounding
                    .background(Color.White.copy(alpha = 0.30f))
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "Ore giornaliere: ${"%.1f".format(dailyHours)}h", // Format to 1 decimal
                    color = Color(0xFF9A4616),
                    fontSize = 18.sp, // Adjusted size
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                Text(
                    text = "Uscita stimata: ${formatTime(calculatedExitTime)}",
                    color = Color(0xFF9A4616),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                Text(
                    text = "Totale oggi: ${totalWorkedTime ?: "N/A"}",
                    color = Color(0xFF9A4616),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold, // Made bold for emphasis
                )
            }
            Spacer(Modifier.height(16.dp)) // Spacer at the bottom
        }

        if (timePickerStateDialog.value.isVisible) {
            val currentDialogType = timePickerStateDialog.value.type
            TimePickerDialog(
                onDismiss = { timePickerStateDialog.value = timePickerStateDialog.value.copy(isVisible = false) },
                onConfirm = {
                    val calendar = Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                        set(Calendar.MINUTE, timePickerState.minute)
                        set(Calendar.SECOND, 0) // Reset seconds for consistency
                        set(Calendar.MILLISECOND, 0)
                    }
                    val selectedDate = calendar.time

                    when (currentDialogType) {
                        ButtonType.Enter -> enterTime = selectedDate
                        ButtonType.ToLunch -> {
                            if (enterTime != null && selectedDate.before(enterTime)) {
                                message = "L'inizio pausa non può essere prima dell'ingresso."
                            } else {
                                toLunchTime = selectedDate
                            }
                        }

                        ButtonType.FromLunch -> {
                            if (toLunchTime != null && selectedDate.before(toLunchTime)) {
                                message = "Il rientro dalla pausa non può essere prima dell'inizio."
                            } else {
                                fromLunchTime = selectedDate
                            }
                        }

                        ButtonType.Exit -> {
                            if (enterTime != null && selectedDate.before(enterTime)) {
                                message = "L'uscita non può essere prima dell'ingresso."
                            } else {
                                // For exit, we only update the state. The main "Exit" button logic handles saving.
                                exitTime = selectedDate // Update the state for immediate UI feedback
                                totalWorkedTime = calculateTotalWorkedTime() // Recalculate for display
                                totalWorkedTime?.let {
                                    saveTimings(it)
                                }
                            }
                        }

                        null -> {}
                    }
                    // The LaunchedEffect will pick up these changes and save progress if needed
                    timePickerStateDialog.value = timePickerStateDialog.value.copy(isVisible = false)
                }
            ) {
                TimePicker(state = timePickerState)
            }
        }

        // Display messages using Toast
        // LaunchedEffect to show Toast only when message changes and is not empty
        LaunchedEffect(message) {
            if (message.isNotEmpty()) {

                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                // Optionally clear message after showing, or let it be overwritten
                // delay(3500) // Keep message on screen for a bit if needed for other UI updates
                // message = ""
            }
        }
    }
}

// Sealed class ButtonType and TimePickerDialogState (ensure these are accessible or defined in the same file)
// If they are in another file, import them. For this example, I'll assume they are defined here or copied.
private data class TimePickerDialogState @OptIn(ExperimentalMaterial3Api::class) constructor(
    val state: TimePickerState,
    val type: ButtonType? = null,
    val isVisible: Boolean,
)

sealed class ButtonType(val text: String) {
    object Enter : ButtonType("Ingresso")
    object ToLunch : ButtonType("In Pausa")
    object FromLunch : ButtonType("Fine Pausa")
    object Exit : ButtonType("Uscita")
}

@Preview(showBackground = true, backgroundColor = 0xFFE0F2F1)
@Composable
fun HomeScreenPreview() {
    MaterialTheme { // Wrap preview in MaterialTheme
        HomeScreen()
    }
}
