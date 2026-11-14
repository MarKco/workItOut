package com.ilsecondodasinistra.workitout.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ilsecondodasinistra.workitout.R
import kotlinx.coroutines.launch

@Composable
fun EditEntryScreen(viewModel: EditEntryViewModel, onClose: () -> Unit) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(uiState.finished) { if (uiState.finished) onClose() }
    LaunchedEffect(uiState.message) {
        uiState.message?.let { message ->
            val text = message.resolve(context)
            scope.launch { snackbarHostState.showSnackbar(message = text) }
            viewModel.messageShown()
        }
    }

    val draft = uiState.draft
    Scaffold(
        contentWindowInsets = WindowInsets(0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        if (draft == null) return@Scaffold
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            FormCard {
                DateField(
                    label = stringResource(R.string.field_date),
                    date = draft.date,
                    onDateChange = viewModel::setDate,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TimeField(stringResource(R.string.field_enter), draft.enter, viewModel::setEnter, Modifier.weight(1f))
                    TimeField(stringResource(R.string.field_exit), draft.exit, viewModel::setExit, Modifier.weight(1f))
                }
            }

            draft.pauses.forEachIndexed { index, pause ->
                FormCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = stringResource(R.string.pause_number, index + 1),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 4.dp),
                        )
                        IconButton(onClick = { viewModel.removePause(index) }) {
                            Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.remove_pause))
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TimeField(
                            stringResource(R.string.field_pause_start), pause.start,
                            { viewModel.setPauseStart(index, it) }, Modifier.weight(1f),
                        )
                        TimeField(
                            stringResource(R.string.field_pause_end), pause.end,
                            { viewModel.setPauseEnd(index, it) }, Modifier.weight(1f),
                        )
                    }
                }
            }
            FilledTonalButton(onClick = viewModel::addPause, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                Text(stringResource(R.string.add_pause))
            }

            FormCard {
                OutlinedTextField(
                    value = uiState.targetInput,
                    onValueChange = viewModel::onTargetInputChange,
                    label = { Text(stringResource(R.string.field_target_hours)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Button(
                onClick = viewModel::save,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = MaterialTheme.shapes.large,
            ) { Text(stringResource(R.string.save), style = MaterialTheme.typography.titleMedium) }
            if (!uiState.isNew) {
                TextButton(
                    onClick = viewModel::requestDelete,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) { Text(stringResource(R.string.delete)) }
            }
            Spacer(Modifier.height(8.dp))
        }

        if (uiState.showDeleteConfirm) {
            AlertDialog(
                onDismissRequest = viewModel::cancelDelete,
                title = { Text(stringResource(R.string.delete)) },
                text = { Text(stringResource(R.string.delete_day_confirmation)) },
                confirmButton = {
                    TextButton(
                        onClick = viewModel::confirmDelete,
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    ) { Text(stringResource(R.string.confirm)) }
                },
                dismissButton = { TextButton(onClick = viewModel::cancelDelete) { Text(stringResource(R.string.cancel)) } },
            )
        }
    }
}

@Composable
private fun FormCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            content = content,
        )
    }
}
