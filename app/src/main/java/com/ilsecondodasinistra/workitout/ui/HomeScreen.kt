package com.ilsecondodasinistra.workitout.ui

import android.Manifest
import android.content.Context
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ilsecondodasinistra.workitout.NOTIFICATION_CHANNEL_ID
import com.ilsecondodasinistra.workitout.NOTIFICATION_ID

import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalLifecycleOwner // Import this
import androidx.lifecycle.LifecycleEventObserver        // Import this

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    homeViewModel: HomeViewModel = viewModel()
) {
    val uiState by homeViewModel.uiState.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current // Get the lifecycle owner

    // Observe lifecycle events to call ViewModel's onResume
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            // We only need to tell the ViewModel about the resume event,
            // as it implements DefaultLifecycleObserver and will call its own onResume.
            // However, to be explicit or if not using DefaultLifecycleObserver directly on VM:
            // if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
            //     homeViewModel.someSpecificOnResumeFunction() // if you had one
            // }
        }
        lifecycleOwner.lifecycle.addObserver(homeViewModel) // Add ViewModel as an observer

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(homeViewModel) // Clean up
        }
    }

    val timePickerState = rememberTimePickerState(is24Hour = true)

    // Handle showing TimePickerDialog based on ViewModel event
    LaunchedEffect(uiState.timePickerEvent) {
        uiState.timePickerEvent?.let { event ->
            timePickerState.hour = event.initialHour
            timePickerState.minute = event.initialMinute
            // The dialog visibility will be controlled by a separate state below
        }
    }

    var showTimePickerDialog by remember { mutableStateOf(false) }
    LaunchedEffect(uiState.timePickerEvent) {
        showTimePickerDialog = uiState.timePickerEvent != null
    }


    // Handle messages (Toast and Notifications)
    LaunchedEffect(uiState.message) {
        if (uiState.message.isNotEmpty()) {
            if (uiState.message.startsWith("NOTIFY_EXIT_TIME:")) {
                val notificationMessage = uiState.message.substringAfter("NOTIFY_EXIT_TIME:")
                showLocalNotification(context, "Ora di uscire!", "Il tuo orario di uscita previsto è $notificationMessage")
                homeViewModel.clearMessage() // Clear notification trigger message
            } else {
                Toast.makeText(context, uiState.message, Toast.LENGTH_LONG).show()
                homeViewModel.clearMessage() // Clear regular message after showing
            }
        }
    }


    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                modifier = Modifier
                    .wrapContentWidth()
                    .weight(1f)
                    .padding(top = 32.dp, bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceAround,
            ) {
                TimeButton(
                    text = ButtonType.Enter.text,
                    time = homeViewModel.formatTimeToDisplay(uiState.enterTime),
                    buttonColor = Color(0xFFFF6A3A),
                    onClick = { homeViewModel.handleTimeButtonPress(ButtonType.Enter) },
                    onEditClick = { homeViewModel.handleTimeEditRequest(ButtonType.Enter) },
                    enabled = true
                )
                TimeButton(
                    text = ButtonType.ToLunch.text,
                    time = homeViewModel.formatTimeToDisplay(uiState.toLunchTime),
                    buttonColor = Color(0xFFB8552A),
                    onClick = { homeViewModel.handleTimeButtonPress(ButtonType.ToLunch) },
                    onEditClick = { homeViewModel.handleTimeEditRequest(ButtonType.ToLunch) },
                    enabled = uiState.enterTime != null
                )
                TimeButton(
                    text = ButtonType.FromLunch.text,
                    time = homeViewModel.formatTimeToDisplay(uiState.fromLunchTime),
                    buttonColor = Color(0xFF9A4616),
                    onClick = { homeViewModel.handleTimeButtonPress(ButtonType.FromLunch) },
                    onEditClick = { homeViewModel.handleTimeEditRequest(ButtonType.FromLunch) },
                    enabled = uiState.toLunchTime != null
                )
                TimeButton(
                    text = ButtonType.Exit.text,
                    time = homeViewModel.formatTimeToDisplay(uiState.exitTime), // ViewModel's exitTime is cleared after save
                    buttonColor = Color(0xFF7A3610),
                    onClick = { homeViewModel.handleTimeButtonPress(ButtonType.Exit) },
                    onEditClick = { homeViewModel.handleTimeEditRequest(ButtonType.Exit) },
                    enabled = uiState.enterTime != null && ((uiState.toLunchTime == null && uiState.fromLunchTime == null) || uiState.fromLunchTime != null)
                )
            }

            Spacer(Modifier.height(16.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(alpha = 0.90f))
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "Ore giornaliere: ${"%.1f".format(uiState.dailyHours)}h",
                    color = Color(0xFF9A4616),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                Text(
                    text = "Uscita stimata: ${homeViewModel.formatTimeToDisplay(uiState.calculatedExitTime)}",
                    color = Color(0xFF9A4616),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                Text(
                    text = "Totale oggi: ${uiState.totalWorkedTime ?: "N/A"}",
                    color = Color(0xFF9A4616),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(Modifier.height(16.dp))
        }

        if (showTimePickerDialog && uiState.timePickerEvent != null) {
            val currentDialogEvent = uiState.timePickerEvent // Capture for stable use in dialog
            TimePickerDialog(
                onDismiss = { homeViewModel.onDialogDismissed() },
                onConfirm = {
                    homeViewModel.onTimeEdited(
                        buttonType = currentDialogEvent!!.type, // !! safe due to outer check
                        hour = timePickerState.hour,
                        minute = timePickerState.minute
                    )
                }
            ) {
                TimePicker(state = timePickerState)
            }
        }
    }
}

// Helper function for showing local notifications (can be moved to a utility file)
private fun showLocalNotification(context: Context, title: String, body: String) {
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
        Log.w("Notification", "POST_NOTIFICATIONS permission not granted. Requesting...")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            (context as? androidx.activity.ComponentActivity)?.let { activity ->
                ActivityCompat.requestPermissions(
                    activity,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    0, // Define a request code if you need to handle the result
                )
            }
        }
        // Fallback: Show toast if permission cannot be requested or is denied.
        Toast.makeText(context, "$title: $body (Notification permission needed)", Toast.LENGTH_LONG).show()

    }
}

@Preview(showBackground = true, backgroundColor = 0xFFE0F2F1)
@Composable
fun HomeScreenPreview() {
    MaterialTheme {
        HomeScreen() // ViewModel will be default-instantiated
    }
}