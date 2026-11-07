package com.ilsecondodasinistra.workitout.ui.theme

import androidx.compose.material3.Shapes
import androidx.compose.ui.graphics.Color
import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Using the custom light purple colors defined in Color.kt
private val LightPurpleColorScheme = lightColorScheme(
    primary = LightPurplePrimary,
    onPrimary = LightPurpleOnPrimary,
    primaryContainer = LightPurplePrimaryContainer,
    onPrimaryContainer = LightPurpleOnPrimaryContainer,
    secondary = LightPurpleSecondary,
    onSecondary = LightPurpleOnSecondary,
    secondaryContainer = LightPurpleSecondaryContainer,
    onSecondaryContainer = LightPurpleOnSecondaryContainer,
    tertiary = LightPurpleTertiary,
    onTertiary = LightPurpleOnTertiary,
    tertiaryContainer = LightPurpleTertiaryContainer,
    onTertiaryContainer = LightPurpleOnTertiaryContainer,
    error = LightPurpleError,
    errorContainer = LightPurpleErrorContainer,
    onError = LightPurpleOnError,
    onErrorContainer = LightPurpleOnErrorContainer,
    background = LightPurpleBackground,
    onBackground = LightPurpleOnBackground,
    surface = LightPurpleSurface,
    onSurface = LightPurpleOnSurface,
    surfaceVariant = LightPurpleSurfaceVariant,
    onSurfaceVariant = LightPurpleOnSurfaceVariant,
    outline = LightPurpleOutline,
    inverseOnSurface = LightPurpleInverseOnSurface,
    inverseSurface = LightPurpleInverseSurface,
    inversePrimary = LightPurpleInversePrimary,
    surfaceTint = LightPurpleSurfaceTint,
    outlineVariant = LightPurpleOutlineVariant,
    scrim = LightPurpleScrim,
)

// Baseline Material 3 dark scheme, used on Android 8-11 where dynamic color is unavailable.
private val DarkPurpleColorScheme = darkColorScheme(
    primary = Color(0xFFD0BCFF),
    onPrimary = Color(0xFF381E72),
    primaryContainer = Color(0xFF4F378B),
    onPrimaryContainer = Color(0xFFEADDFF),
    secondary = Color(0xFFCCC2DC),
    onSecondary = Color(0xFF332D41),
    secondaryContainer = Color(0xFF4A4458),
    onSecondaryContainer = Color(0xFFE8DEF8),
    tertiary = Color(0xFFEFB8C8),
    onTertiary = Color(0xFF492532),
    tertiaryContainer = Color(0xFF633B48),
    onTertiaryContainer = Color(0xFFFFD8E4),
    error = Color(0xFFF2B8B5),
    onError = Color(0xFF601410),
    errorContainer = Color(0xFF8C1D18),
    onErrorContainer = Color(0xFFF9DEDC),
    background = Color(0xFF141218),
    onBackground = Color(0xFFE6E0E9),
    surface = Color(0xFF141218),
    onSurface = Color(0xFFE6E0E9),
    surfaceVariant = Color(0xFF49454F),
    onSurfaceVariant = Color(0xFFCAC4D0),
    outline = Color(0xFF938F99),
    outlineVariant = Color(0xFF49454F),
    inverseSurface = Color(0xFFE6E0E9),
    inverseOnSurface = Color(0xFF322F35),
    inversePrimary = Color(0xFF6750A4),
    scrim = Color(0xFF000000),
)

@Composable
fun WorkItOutM3Theme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkPurpleColorScheme // Your custom dark purple theme
        else -> LightPurpleColorScheme      // Your custom light purple theme
    }
    // The app bar is drawn in the primary color behind a transparent status bar:
    // pick light or dark status bar icons to stay readable on it.
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars =
                colorScheme.primary.luminance() > 0.5f
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography, // Assuming you have Typography.kt defined
        shapes = Shapes,         // Assuming you have Shapes.kt defined (M3 often uses slightly larger corner radii)
        content = content
    )
}