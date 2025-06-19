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
import androidx.compose.ui.graphics.toArgb
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

// For dark theme, you'd define a corresponding DarkPurpleColorScheme
// For simplicity, we'll just use a slightly modified standard dark theme
// or you can create one from the Material Theme Builder.
private val DarkPurpleColorScheme = darkColorScheme(
    primary = Purple80, // Example, customize these
    secondary = PurpleGrey80,
    tertiary = Pink80,
    background = Color(0xFF1C1B1F),
    surface = Color(0xFF2B2930), // Darker purple surface
    onPrimary = Purple40,
    onSecondary = PurpleGrey40,
    onTertiary = Pink40,
    onBackground = LightPurpleOnPrimary, // Light text on dark background
    onSurface = LightPurpleOnPrimary,    // Light text on dark surface
    primaryContainer = Color(0xFF4A4458),
    onPrimaryContainer = LightPurplePrimaryContainer
    // ... other dark theme colors
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
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb() // Or use surface for less emphasis
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            // For navigation bar (optional)
            // window.navigationBarColor = colorScheme.surface.toArgb()
            // WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography, // Assuming you have Typography.kt defined
        shapes = Shapes,         // Assuming you have Shapes.kt defined (M3 often uses slightly larger corner radii)
        content = content
    )
}