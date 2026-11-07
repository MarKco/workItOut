package com.ilsecondodasinistra.workitout

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.ilsecondodasinistra.workitout.alarm.NOTIFICATION_CHANNEL_ID

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        createNotificationChannel()

        setContent {
            var showAlarmDialog by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) { showAlarmDialog = !canScheduleExactAlarms() }

            WorkItOutAppEntry()

            if (showAlarmDialog) {
                AlarmPermissionDialog(
                    onGoToSettings = {
                        startActivity(
                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                                .setData(Uri.fromParts("package", packageName, null)),
                        )
                        showAlarmDialog = false
                    },
                    onCancel = { showAlarmDialog = false },
                )
            }
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply { description = getString(R.string.notification_channel_description) }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun canScheduleExactAlarms(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            getSystemService(AlarmManager::class.java).canScheduleExactAlarms()
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
        },
    )
}
