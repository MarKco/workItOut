package com.ilsecondodasinistra.workitout

import android.Manifest
import android.os.Build
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.ilsecondodasinistra.workitout.ui.HomeScreen
import com.ilsecondodasinistra.workitout.ui.HomeViewModel
import com.ilsecondodasinistra.workitout.ui.SettingsScreen
import com.ilsecondodasinistra.workitout.ui.theme.WorkItOutM3Theme

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun WorkItOutAppEntry() {
    WorkItOutM3Theme {
        var currentPage by remember { mutableStateOf("home") }
        var resetRequested by remember { mutableStateOf(false) }

        val context = LocalContext.current
        val notificationPermissionState = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            rememberPermissionState(permission = Manifest.permission.POST_NOTIFICATIONS)
        } else {
            null
        }

        LaunchedEffect(key1 = notificationPermissionState) {
            if (notificationPermissionState != null && !notificationPermissionState.status.isGranted) {
                notificationPermissionState.launchPermissionRequest()
            }
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = if (currentPage == "home") stringResource(R.string.app_name) else stringResource(R.string.settings),
                            style = MaterialTheme.typography.headlineSmall,
                        )
                    },
                    actions = {
                        // Reset button (only on home page)
                        if (currentPage == "home") {
                            IconButton(onClick = { resetRequested = true }) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = stringResource(R.string.reset_all)
                                )
                            }
                        }
                        // Settings/Home navigation button
                        IconButton(onClick = {
                            currentPage = if (currentPage == "home") "settings" else "home"
                        }) {
                            Icon(
                                imageVector = if (currentPage == "home") Icons.Filled.Settings else Icons.Filled.Home,
                                contentDescription = if (currentPage == "home") stringResource(R.string.settings) else stringResource(R.string.home)
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
                    if (currentPage == "home") {
                        HomeScreen(
                            resetRequested = resetRequested,
                            onResetHandled = { resetRequested = false }
                        )
                    } else {
                        SettingsScreen()
                    }
                }
            }
        }
    }
}

// Make sure you have this in your strings.xml
// <string name="app_name">WorkItOut</string>
// <string name="settings">Impostazioni</string>


// Create a dummy drawable for preview if you don't have the actual logo yet
// For example, in res/drawable/ic_workitout_logo_preview.xml:
/*
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
  <path
      android:fillColor="@android:color/white"
      android:pathData="M12,2C6.48,2 2,6.48 2,12s4.48,10 10,10 10,-4.48 10,-10S17.52,2 12,2zM12,20c-4.41,0 -8,-3.59 -8,-8s3.59,-8 8,-8 8,3.59 8,8S16.41,20 12,20zM12.5,7H11v6l5.25,3.15 0.75,-1.23 -4.5,-2.67z"/>
</vector>
*/

@Preview(showBackground = true)
@Composable
fun WorkItOutAppEntryPreview() {
    // In preview, provide a fallback drawable or ensure your real one exists
    // For the R.drawable.ic_workitout_logo to resolve in preview, ensure it's in a common source set or use a specific preview drawable.
    WorkItOutAppEntry()
}