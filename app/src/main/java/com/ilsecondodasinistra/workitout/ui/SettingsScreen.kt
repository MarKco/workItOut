package com.ilsecondodasinistra.workitout.ui

import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ilsecondodasinistra.workitout.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(settingsViewModel: SettingsViewModel) {
    val uiState by settingsViewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current
    val locale = LocalConfiguration.current.locales[0]

    LaunchedEffect(uiState.message) {
        uiState.message?.let { message ->
            val text = message.resolve(context)
            scope.launch { snackbarHostState.showSnackbar(message = text) }
            settingsViewModel.messageShown()
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val saved = runCatching {
                val text = settingsViewModel.createBackup()
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri, "wt")!!.use { it.write(text.toByteArray()) }
                }
            }
            if (saved.isSuccess) settingsViewModel.onBackupSaved() else settingsViewModel.onBackupIoError()
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val text = runCatching {
                withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)!!.use { it.readBytes().decodeToString() }
                }
            }
            text.onSuccess(settingsViewModel::onBackupRead).onFailure { settingsViewModel.onBackupIoError() }
        }
    }

    var textFieldValue by remember { mutableStateOf(TextFieldValue(uiState.dailyHoursInput)) }
    LaunchedEffect(uiState.dailyHoursInput) {
        if (textFieldValue.text != uiState.dailyHoursInput) {
            textFieldValue = textFieldValue.copy(text = uiState.dailyHoursInput)
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Section(stringResource(R.string.total_working_hours)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedTextField(
                        value = textFieldValue,
                        onValueChange = {
                            textFieldValue = it
                            settingsViewModel.onDailyHoursInputChange(it.text)
                        },
                        label = { Text(stringResource(R.string.hours_example)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = MaterialTheme.shapes.large,
                    )
                    Button(
                        onClick = {
                            keyboardController?.hide()
                            settingsViewModel.saveDailyHours()
                        },
                        modifier = Modifier.height(56.dp),
                        shape = MaterialTheme.shapes.large,
                    ) {
                        Text(text = stringResource(R.string.save), style = MaterialTheme.typography.labelLarge)
                    }
                }
            }

            Section(stringResource(R.string.work_days)) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    orderedWeekDays(locale).forEach { day ->
                        FilterChip(
                            selected = day in uiState.workDays,
                            onClick = { settingsViewModel.toggleWorkDay(day) },
                            label = { Text(day.getDisplayName(TextStyle.SHORT, locale)) },
                        )
                    }
                }
                Text(
                    text = stringResource(R.string.work_days_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                Section(stringResource(R.string.settings_appearance)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.dynamic_color), style = MaterialTheme.typography.bodyLarge)
                            Text(
                                text = stringResource(R.string.dynamic_color_hint),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(
                            checked = uiState.dynamicColor,
                            onCheckedChange = settingsViewModel::setDynamicColor,
                        )
                    }
                }
            }

            Section(stringResource(R.string.backup)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    FilledTonalButton(
                        onClick = { exportLauncher.launch(settingsViewModel.backupFileName()) },
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        shape = MaterialTheme.shapes.large,
                    ) { Text(stringResource(R.string.backup_export)) }
                    FilledTonalButton(
                        onClick = { importLauncher.launch(arrayOf("*/*")) },
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        shape = MaterialTheme.shapes.large,
                    ) { Text(stringResource(R.string.backup_restore)) }
                }
            }
            Spacer(Modifier.height(8.dp))
        }

        uiState.pendingRestore?.let { file ->
            AlertDialog(
                onDismissRequest = settingsViewModel::cancelRestore,
                title = { Text(stringResource(R.string.restore_confirm_title)) },
                text = { Text(stringResource(R.string.restore_confirm_message, file.history.size)) },
                confirmButton = {
                    TextButton(onClick = settingsViewModel::confirmRestore) { Text(stringResource(R.string.confirm)) }
                },
                dismissButton = {
                    TextButton(onClick = settingsViewModel::cancelRestore) { Text(stringResource(R.string.cancel)) }
                },
            )
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            content()
        }
    }
}

/** Week days starting from the first day of the week of the current locale. */
private fun orderedWeekDays(locale: Locale): List<DayOfWeek> {
    val first = WeekFields.of(locale).firstDayOfWeek
    return (0..6).map { first.plus(it.toLong()) }
}
