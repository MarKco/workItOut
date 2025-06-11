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
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

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
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var userId: String
    private var appId: String = "default-app-id" // Replace with your actual app ID

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

        // Initialize Firebase
        FirebaseApp.initializeApp(this)
        auth = Firebase.auth
        db = Firebase.firestore

        // Check for custom auth token from Canvas environment (if applicable)
        // In a real Android app, you'd handle user authentication directly,
        // e.g., with email/password, Google Sign-In, or anonymous sign-in.
        // The __initial_auth_token is a Canvas-specific variable.
        val initialAuthToken = intent.getStringExtra("initial_auth_token") // Simulate passing token

        auth.addAuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            if (user != null) {
                userId = user.uid
                Log.d("Auth", "User ID: $userId")
                setContent {
                    WorkTrackerApp(db, userId, appId) { requestNotificationPermission() }
                }
            } else {
                // Sign in anonymously if no user is logged in
                if (!initialAuthToken.isNullOrEmpty()) {
                    auth.signInWithCustomToken(initialAuthToken).addOnCompleteListener(this) { task ->
                        if (task.isSuccessful) {
                            userId = auth.currentUser!!.uid
                            Log.d("Auth", "Signed in with custom token. User ID: $userId")
                            setContent {
                                WorkTrackerApp(db, userId, appId) { requestNotificationPermission() }
                            }
                        } else {
                            Log.e("Auth", "Custom token sign-in failed", task.exception)
                            auth.signInAnonymously().addOnCompleteListener(this) { anonTask ->
                                if (anonTask.isSuccessful) {
                                    userId = auth.currentUser!!.uid
                                    Log.d("Auth", "Signed in anonymously. User ID: $userId")
                                    setContent {
                                        WorkTrackerApp(db, userId, appId) { requestNotificationPermission() }
                                    }
                                } else {
                                    Log.e("Auth", "Anonymous sign-in failed", anonTask.exception)
                                    // Handle sign-in failure, e.g., show an error message
                                }
                            }
                        }
                    }
                } else {
                    auth.signInAnonymously().addOnCompleteListener(this) { task ->
                        if (task.isSuccessful) {
                            userId = auth.currentUser!!.uid
                            Log.d("Auth", "Signed in anonymously. User ID: $userId")
                            setContent {
                                WorkTrackerApp(db, userId, appId) { requestNotificationPermission() }
                            }
                        } else {
                            Log.e("Auth", "Anonymous sign-in failed", task.exception)
                            // Handle sign-in failure, e.g., show an error message
                        }
                    }
                }
            }
        }

        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Workitout"
            val descriptionText = "Notifications for work hours tracking"
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
}

suspend fun <T> Task<T>.await(): T =
    suspendCoroutine { continuation ->
        addOnSuccessListener { result ->
            continuation.resume(result)
        }
        addOnFailureListener { exception ->
            continuation.resumeWithException(exception)
        }
    }

// You would typically have a dedicated theme file for your app
// For simplicity, defining a basic preview here.
@Preview(showBackground = true)
@Composable
fun PreviewWorkTrackerApp() {
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
    Firebase.auth
    val db = Firebase.firestore
    WorkTrackerApp(db, "preview_user_id", "preview_app_id") { /* No-op for preview */ }
}
