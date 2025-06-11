plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.gms.google-services") // Google Services plugin
    id("org.jetbrains.kotlin.plugin.compose") // No version here, it's inherited
}

android {
    namespace = "com.ilsecondodasinistra.workitout" // Replace with your actual package
    compileSdk = 36

    defaultConfig {
        applicationId = "com.ilsecondodasinistra.workitout" // Replace with your actual package
        minSdk = 24
        targetSdk = 36
        versionCode = 1001
        versionName = "3.0.0"
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1"
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }

    // Optional: Enable if using Java 11+ features
    // compileOptions {
    //     sourceCompatibility = JavaVersion.VERSION_11
    //     targetCompatibility = JavaVersion.VERSION_11
    // }
}

dependencies {
    // Core Android
    implementation(libs.androidx.core.ktx.v1131)
    implementation(libs.androidx.lifecycle.runtime.ktx.v281)
    implementation(libs.androidx.activity.compose.v190)

    // Jetpack Compose
    implementation(platform(libs.androidx.compose.bom.v20230800))
    implementation(libs.ui)
    implementation(libs.ui.graphics)
    implementation(libs.ui.tooling.preview)
    implementation(libs.material3)
    implementation(libs.androidx.material.icons.core)

    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth.ktx)
    implementation(libs.firebase.firestore.ktx)

    // Coroutines for Firebase
    implementation(libs.kotlinx.coroutines.play.services)

    // Tooling for previews
    debugImplementation(libs.ui.tooling)
    debugImplementation(libs.ui.test.manifest)

    implementation(libs.androidx.fragment.ktx) // Or just "androidx.fragment:fragment:YOUR_CURRENT_VERSION"
}
