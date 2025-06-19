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
        versionCode = 1002
        versionName = "3.1.0"
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
    implementation(libs.ui.accompanist.user.permissions) // Or latest version
    implementation(libs.material3)
    implementation(libs.androidx.material.icons.core)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx) // For lifecycleScope, collectAsStateWithLifecycle
    implementation(libs.androidx.lifecycle.viewmodel.ktx) // For viewModelScope

    // Tooling for previews
    debugImplementation(libs.ui.tooling)
    debugImplementation(libs.ui.test.manifest)

    implementation(libs.androidx.fragment.ktx) // Or just "androidx.fragment:fragment:YOUR_CURRENT_VERSION"
}
