package com.ilsecondodasinistra.workitout

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.firestore.FirebaseFirestore
import com.ilsecondodasinistra.workitout.ui.HomeScreen
import com.ilsecondodasinistra.workitout.ui.SettingsScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkTrackerApp(
    db: FirebaseFirestore,
    userId: String,
    appId: String,
    requestNotificationPermission: () -> Unit,
) {
    var currentPage by remember { mutableStateOf("home") } // 'home' or 'settings'

    // Request notification permission when the app starts
    LocalContext.current
    LaunchedEffect(Unit) {
        requestNotificationPermission()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Workitout",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFFF7720)), // Purple-700
                actions = {
                    Button(
                        onClick = {
                            currentPage =
                                if (currentPage ==
                                    "home"
                                ) {
                                    "settings"
                                } else {
                                    "home"
                                }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9A4616)), // Purple-800
                        modifier = Modifier.padding(end = 8.dp),
                    ) {
                        Text(text = if (currentPage == "home") stringResource(R.string.settings) else "Home", color = Color.White)
                    }
                },
            )
        },
        bottomBar = {
            BottomAppBar(
                containerColor = Color(0xFFFA914D), // Darker Purple
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = "User ID: $userId",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.7f),
                    modifier =
                        Modifier
                            .weight(1f)
                            .padding(start = 16.dp),
                )
                Text(
                    text = "App ID: $appId",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.padding(end = 16.dp),
                )
            }
        },
        containerColor = Color(0xFFFF7720), // Purple-600
    ) { paddingValues ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(
                        brush =
                            androidx.compose.ui.graphics.Brush.verticalGradient(
                                colors = listOf(Color(0xFFFF924F), Color(0xFFFF6300)), // Purple-600 to Indigo-800
                            ),
                    )
                    .padding(paddingValues)
                    .padding(16.dp),
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color.White.copy(alpha = 0.1f))
                        .padding(16.dp),
            ) {
                if (currentPage == "home") {
                    HomeScreen(db, userId, appId)
                } else {
                    SettingsScreen(db, userId, appId)
                }
            }
        }
    }
}