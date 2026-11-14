package com.ilsecondodasinistra.workitout.ui.theme

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

private val LightOrangeColorScheme = lightColorScheme(
    primary = OrangeLightPrimary,
    onPrimary = OrangeLightOnPrimary,
    primaryContainer = OrangeLightPrimaryContainer,
    onPrimaryContainer = OrangeLightOnPrimaryContainer,
    secondary = OrangeLightSecondary,
    onSecondary = OrangeLightOnSecondary,
    secondaryContainer = OrangeLightSecondaryContainer,
    onSecondaryContainer = OrangeLightOnSecondaryContainer,
    tertiary = OrangeLightTertiary,
    onTertiary = OrangeLightOnTertiary,
    tertiaryContainer = OrangeLightTertiaryContainer,
    onTertiaryContainer = OrangeLightOnTertiaryContainer,
    background = OrangeLightBackground,
    onBackground = OrangeLightOnBackground,
    surface = OrangeLightBackground,
    onSurface = OrangeLightOnBackground,
    surfaceVariant = OrangeLightSurfaceVariant,
    onSurfaceVariant = OrangeLightOnSurfaceVariant,
    outline = OrangeLightOutline,
    outlineVariant = OrangeLightOutlineVariant,
    inverseSurface = OrangeLightInverseSurface,
    inverseOnSurface = OrangeLightInverseOnSurface,
    inversePrimary = OrangeLightInversePrimary,
    surfaceTint = OrangeLightPrimary,
    surfaceBright = OrangeLightBackground,
    surfaceDim = OrangeLightSurfaceDim,
    surfaceContainerLowest = OrangeLightSurfaceContainerLowest,
    surfaceContainerLow = OrangeLightSurfaceContainerLow,
    surfaceContainer = OrangeLightSurfaceContainer,
    surfaceContainerHigh = OrangeLightSurfaceContainerHigh,
    surfaceContainerHighest = OrangeLightSurfaceContainerHighest,
)

private val DarkOrangeColorScheme = darkColorScheme(
    primary = OrangeDarkPrimary,
    onPrimary = OrangeDarkOnPrimary,
    primaryContainer = OrangeDarkPrimaryContainer,
    onPrimaryContainer = OrangeDarkOnPrimaryContainer,
    secondary = OrangeDarkSecondary,
    onSecondary = OrangeDarkOnSecondary,
    secondaryContainer = OrangeDarkSecondaryContainer,
    onSecondaryContainer = OrangeDarkOnSecondaryContainer,
    tertiary = OrangeDarkTertiary,
    onTertiary = OrangeDarkOnTertiary,
    tertiaryContainer = OrangeDarkTertiaryContainer,
    onTertiaryContainer = OrangeDarkOnTertiaryContainer,
    background = OrangeDarkBackground,
    onBackground = OrangeDarkOnBackground,
    surface = OrangeDarkBackground,
    onSurface = OrangeDarkOnBackground,
    surfaceVariant = OrangeDarkSurfaceVariant,
    onSurfaceVariant = OrangeDarkOnSurfaceVariant,
    outline = OrangeDarkOutline,
    outlineVariant = OrangeDarkOutlineVariant,
    inverseSurface = OrangeDarkInverseSurface,
    inverseOnSurface = OrangeDarkInverseOnSurface,
    inversePrimary = OrangeDarkInversePrimary,
    surfaceTint = OrangeDarkPrimary,
    surfaceBright = OrangeDarkSurfaceBright,
    surfaceDim = OrangeDarkBackground,
    surfaceContainerLowest = OrangeDarkSurfaceContainerLowest,
    surfaceContainerLow = OrangeDarkSurfaceContainerLow,
    surfaceContainer = OrangeDarkSurfaceContainer,
    surfaceContainerHigh = OrangeDarkSurfaceContainerHigh,
    surfaceContainerHighest = OrangeDarkSurfaceContainerHighest,
)

@Composable
fun WorkItOutM3Theme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Wallpaper-based dynamic color (Android 12+) is opt-in: by default the app keeps its orange identity.
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkOrangeColorScheme
        else -> LightOrangeColorScheme
    }
    // The app bar blends into the background behind a transparent status bar:
    // pick light or dark status bar icons to stay readable on it.
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars =
                colorScheme.background.luminance() > 0.5f
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography, // Assuming you have Typography.kt defined
        shapes = Shapes,         // Assuming you have Shapes.kt defined (M3 often uses slightly larger corner radii)
        content = content
    )
}