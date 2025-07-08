package com.ilsecondodasinistra.workitout

// Extension functions for suspend calls
import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.app.ActivityCompat
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.res.stringResource

// Define Firebase configuration and app ID globals (these would typically be managed in
// build.gradle or as string resources in a real Android app, not directly here)
// For this example, we'll simulate them.
// In a real Android app, you would get these from your google-services.json
// and FirebaseApp.initializeApp(this) in your Application class or MainActivity.
// The __app_id, __firebase_config, __initial_auth_token are provided by the Canvas environment
// and are not directly accessible in a standalone Android project.
// You'd initialize Firebase like this in your Application class or MainActivity:
// FirebaseApp.initializeApp(context)
// auth = Firebase.auth
// db = Firebase.firestore

// Simulating global variables for demonstration purposes. In a real app,
// these would be handled by Firebase setup.
const val NOTIFICATION_CHANNEL_ID = "work_tracker_channel"
const val NOTIFICATION_ID = 101

class MainActivity : ComponentActivity() {
    companion object {
        private var entranceCount = 0
        private var lastPermissionRequestCount = -10
        fun incrementEntranceAndMaybeRequestPermission(context: android.content.Context) {
            entranceCount++
            val activity = context as? MainActivity ?: return
            if (shouldRequestNotificationPermission(context)) {
                activity.requestNotificationPermission()
                lastPermissionRequestCount = entranceCount
            }
        }
        fun resetEntranceCounter() {
            entranceCount = 0
            lastPermissionRequestCount = -10
        }
        private fun shouldRequestNotificationPermission(context: android.content.Context): Boolean {
            return Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED &&
                (entranceCount == 1 || entranceCount - lastPermissionRequestCount >= 10)
        }
    }
    // Request notification permission for Android 13+
    private val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission(),
        ) { isGranted: Boolean ->
            if (isGranted) {
                Log.d("Notification", "Notification permission granted")
            } else {
                Log.d("Notification", "Notification permission denied")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Check for custom auth token from Canvas environment (if applicable)
        // In a real Android app, you'd handle user authentication directly,
        // e.g., with email/password, Google Sign-In, or anonymous sign-in.
        // The __initial_auth_token is a Canvas-specific variable.
        setContent {
            var showAlarmDialog by remember { mutableStateOf(false) }
            val context = this
            // Check alarm permission on startup
            LaunchedEffect(Unit) {
                if (!hasExactAlarmPermission()) {
                    showAlarmDialog = true
                }
            }
            WorkItOutAppEntry()
            if (showAlarmDialog) {
                AlarmPermissionDialog(
                    onGoToSettings = {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                        }
                        context.startActivity(intent)
                        showAlarmDialog = false
                    },
                    onCancel = { showAlarmDialog = false }
                )
            }
        }

        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.notification_channel_name)
            val descriptionText = getString(R.string.notification_channel_description)
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel =
                NotificationChannel(NOTIFICATION_CHANNEL_ID, name, importance).apply {
                    description = descriptionText
                }
            val notificationManager: NotificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS,
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun hasExactAlarmPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(ALARM_SERVICE) as android.app.AlarmManager
            alarmManager.canScheduleExactAlarms()
        } else {
            true // Permission not required on older versions
        }
    }
}

@Composable
fun AlarmPermissionDialog(onGoToSettings: () -> Unit, onCancel: () -> Unit) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(text = stringResource(R.string.alarm_permission_title)) },
        text = { Text(text = stringResource(R.string.alarm_permission_message)) },
        confirmButton = {
            Button(onClick = onGoToSettings) {
                Text(text = stringResource(R.string.alarm_permission_settings_button))
            }
        },
        dismissButton = {
            Button(onClick = onCancel) {
                Text(text = stringResource(R.string.alarm_permission_cancel_button))
            }
        }
    )
}


// You would typically have a dedicated theme file for your app
// For simplicity, defining a basic preview here.
@Preview(showBackground = true)
@Composable
fun PreviewWorkItOut() {
    // This preview will not have Firebase initialized,
    // so it will show a loading state or a simplified UI.
    // In a real scenario, you'd mock Firebase or run on a device.
    // For a functional preview, you would need to initialize Firebase
    // within the preview composable, which is generally not recommended
    // as it can slow down previews.
    // A better practice for previews is to create a wrapper that
    // takes mock data/dependencies.
    // For now, it will just show the structure.
    LocalContext.current
    WorkItOutAppEntry()
}
