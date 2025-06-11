// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id("com.android.application") version "8.10.1" apply false // Your actual version
    id("org.jetbrains.kotlin.android") version "2.1.21" apply false // Your actual version
    id("com.google.gms.google-services") version "4.4.2" apply false // Google Services plugin version
    id("org.jetbrains.kotlin.plugin.compose") version "2.1.21" apply false // Or your target Kotlin version, e.g., "2.1.0-Beta" if you're on a beta
}
