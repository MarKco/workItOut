package com.ilsecondodasinistra.workitout

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.ilsecondodasinistra.workitout.ui.EditEntryScreen
import com.ilsecondodasinistra.workitout.ui.EditEntryViewModel
import com.ilsecondodasinistra.workitout.ui.HistoryScreen
import com.ilsecondodasinistra.workitout.ui.HistoryViewModel
import com.ilsecondodasinistra.workitout.ui.HomeRoute
import com.ilsecondodasinistra.workitout.ui.HomeViewModel
import com.ilsecondodasinistra.workitout.ui.SettingsScreen
import com.ilsecondodasinistra.workitout.ui.SettingsViewModel
import com.ilsecondodasinistra.workitout.ui.StatsRoute
import com.ilsecondodasinistra.workitout.ui.StatsViewModel
import com.ilsecondodasinistra.workitout.ui.stringProvider
import com.ilsecondodasinistra.workitout.ui.theme.WorkItOutM3Theme
import kotlinx.serialization.Serializable

@Serializable
data object HomeKey : NavKey

@Serializable
data object SettingsKey : NavKey

@Serializable
data object HistoryKey : NavKey

@Serializable
data object StatsKey : NavKey

/** [id] null means "add a new day". */
@Serializable
data class EditEntryKey(val id: String? = null) : NavKey

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkItOutAppEntry() {
    WorkItOutM3Theme {
        val context = LocalContext.current
        val container = (context.applicationContext as WorkItOutApp).container
        val backStack = rememberNavBackStack(HomeKey)
        val current = backStack.lastOrNull() ?: HomeKey

        // The reset button lives in the top bar, so Home's ViewModel is owned by the activity.
        val homeViewModel: HomeViewModel = viewModel(
            factory = viewModelFactory {
                initializer { HomeViewModel(container.historyRepository, container.exitReminderScheduler) }
            },
        )

        RequestNotificationPermission()

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = stringResource(
                                when (current) {
                                    SettingsKey -> R.string.settings
                                    HistoryKey -> R.string.history
                                    StatsKey -> R.string.statistics
                                    is EditEntryKey -> if (current.id == null) R.string.add_day else R.string.edit_day
                                    else -> R.string.app_name
                                },
                            ),
                            style = MaterialTheme.typography.headlineSmall,
                        )
                    },
                    actions = {
                        if (current == HomeKey) {
                            IconButton(onClick = homeViewModel::resetAll) {
                                Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.reset_all))
                            }
                            IconButton(onClick = { backStack.add(HistoryKey) }) {
                                Icon(painterResource(R.drawable.ic_history), contentDescription = stringResource(R.string.history))
                            }
                            IconButton(onClick = { backStack.add(StatsKey) }) {
                                Icon(painterResource(R.drawable.ic_bar_chart), contentDescription = stringResource(R.string.statistics))
                            }
                            IconButton(onClick = { backStack.add(SettingsKey) }) {
                                Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.settings))
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        titleContentColor = MaterialTheme.colorScheme.onPrimary,
                        actionIconContentColor = MaterialTheme.colorScheme.onPrimary,
                        navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                    navigationIcon = {
                        if (current == HomeKey) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_launcher_for_topbar),
                                contentDescription = stringResource(R.string.app_name),
                                modifier = Modifier
                                    .padding(start = 12.dp, end = 8.dp)
                                    .size(32.dp),
                                contentScale = ContentScale.Fit,
                            )
                        } else {
                            IconButton(onClick = { backStack.removeLastOrNull() }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                            }
                        }
                    },
                )
            },
        ) { paddingValues ->
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                color = MaterialTheme.colorScheme.background,
            ) {
                // On tablets and foldables keep the content a readable width instead of stretching it.
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                    NavDisplay(
                        modifier = Modifier.widthIn(max = 640.dp).fillMaxHeight(),
                        backStack = backStack,
                        onBack = { backStack.removeLastOrNull() },
                        entryDecorators = listOf(
                            rememberSaveableStateHolderNavEntryDecorator(),
                            rememberViewModelStoreNavEntryDecorator(),
                        ),
                        entryProvider = entryProvider {
                            entry<HomeKey> {
                                Column(Modifier.padding(16.dp)) { HomeRoute(homeViewModel) }
                            }
                            entry<SettingsKey> {
                                val vm: SettingsViewModel = viewModel(
                                    factory = viewModelFactory { initializer { SettingsViewModel(container.historyRepository) } },
                                )
                                SettingsScreen(vm)
                            }
                            entry<HistoryKey> {
                                val vm: HistoryViewModel = viewModel(
                                    factory = viewModelFactory {
                                        initializer {
                                            HistoryViewModel(container.historyRepository, context.applicationContext.stringProvider())
                                        }
                                    },
                                )
                                HistoryScreen(
                                    viewModel = vm,
                                    onEditEntry = { id -> backStack.add(EditEntryKey(id)) },
                                    onAddEntry = { backStack.add(EditEntryKey(null)) },
                                )
                            }
                            entry<StatsKey> {
                                val vm: StatsViewModel = viewModel(
                                    factory = viewModelFactory { initializer { StatsViewModel(container.historyRepository) } },
                                )
                                StatsRoute(vm)
                            }
                            entry<EditEntryKey> { key ->
                                val vm: EditEntryViewModel = viewModel(
                                    factory = viewModelFactory {
                                        initializer { EditEntryViewModel(container.historyRepository, key.id) }
                                    },
                                )
                                EditEntryScreen(vm, onClose = { backStack.removeLastOrNull() })
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun RequestNotificationPermission() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    LaunchedEffect(Unit) {
        val granted = context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        if (!granted) launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}
