package com.ilsecondodasinistra.workitout

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel as composeViewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.ilsecondodasinistra.workitout.ui.HomeRoute
import com.ilsecondodasinistra.workitout.ui.HomeViewModel
import com.ilsecondodasinistra.workitout.ui.SettingsScreen
import com.ilsecondodasinistra.workitout.ui.SettingsViewModel
import com.ilsecondodasinistra.workitout.ui.stringProvider
import com.ilsecondodasinistra.workitout.ui.theme.WorkItOutM3Theme
import kotlinx.serialization.Serializable

@Serializable
data object HomeKey : NavKey

@Serializable
data object SettingsKey : NavKey

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkItOutAppEntry() {
    WorkItOutM3Theme {
        val context = LocalContext.current
        val container = (context.applicationContext as WorkItOutApp).container
        val backStack = rememberNavBackStack(HomeKey)
        val onHome = backStack.lastOrNull() != SettingsKey

        // The reset button lives in the top bar, so Home's ViewModel is owned by the activity.
        val homeViewModel: HomeViewModel = composeViewModel(
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
                            text = stringResource(if (onHome) R.string.app_name else R.string.settings),
                            style = MaterialTheme.typography.headlineSmall,
                        )
                    },
                    actions = {
                        // Reset button (only on home page)
                        if (onHome) {
                            IconButton(onClick = homeViewModel::resetAll) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = stringResource(R.string.reset_all)
                                )
                            }
                        }
                        // Settings/Home navigation button
                        IconButton(onClick = {
                            if (onHome) backStack.add(SettingsKey) else backStack.removeLastOrNull()
                        }) {
                            Icon(
                                imageVector = if (onHome) Icons.Filled.Settings else Icons.Filled.Home,
                                contentDescription = if (onHome) stringResource(R.string.settings) else stringResource(R.string.home)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        titleContentColor = MaterialTheme.colorScheme.onPrimary,
                        actionIconContentColor = MaterialTheme.colorScheme.onPrimary,
                        navigationIconContentColor = MaterialTheme.colorScheme.onPrimary // Color for the nav icon
                    ),
                    // Navigation Icon (App Logo)
                    navigationIcon = {
                        // Add some padding to the logo if needed
                        Image(
                            painter = painterResource(id = R.drawable.ic_launcher_for_topbar), // Replace with your actual drawable resource ID
                            contentDescription = stringResource(R.string.app_name), // App name as content description
                            modifier = Modifier
                                .padding(start = 12.dp, end = 8.dp) // Adjust padding as needed
                                .size(32.dp), // Adjust size as needed
                            contentScale = ContentScale.Fit,
                            // If your logo is a single color and needs to be tinted by the theme:
                            // colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onPrimary)
                        )
                        // If you want it to be an IconButton (e.g., if it were a back arrow)
                        // IconButton(onClick = { /* Potentially go to a main dashboard or do nothing */ }) {
                        //     Icon(
                        //         painter = painterResource(id = R.drawable.ic_workitout_logo),
                        //         contentDescription = stringResource(R.string.app_name)
                        //     )
                        // }
                    },
                )
            },
        ) { paddingValues ->
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                color = MaterialTheme.colorScheme.background
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    NavDisplay(
                        backStack = backStack,
                        onBack = { backStack.removeLastOrNull() },
                        entryDecorators = listOf(
                            rememberSaveableStateHolderNavEntryDecorator(),
                            rememberViewModelStoreNavEntryDecorator(),
                        ),
                        entryProvider = entryProvider {
                            entry<HomeKey> { HomeRoute(homeViewModel) }
                            entry<SettingsKey> {
                                val settingsViewModel: SettingsViewModel = composeViewModel(
                                    factory = viewModelFactory {
                                        initializer {
                                            SettingsViewModel(container.historyRepository, context.applicationContext.stringProvider())
                                        }
                                    },
                                )
                                SettingsScreen(settingsViewModel)
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
