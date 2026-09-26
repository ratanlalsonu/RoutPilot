package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = RpPrimaryBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCEBFF),
    onPrimaryContainer = RpTextNavy,
    secondary = RpSafeGreen,
    onSecondary = Color.White,
    secondaryContainer = RpSafeGreenBg,
    onSecondaryContainer = Color(0xFF065F46),
    tertiary = RpCriticalOrange,
    background = RpLightBackground,
    onBackground = RpTextNavy,
    surface = RpLightSurface,
    onSurface = RpTextNavy,
    surfaceVariant = RpLightSurfaceVariant,
    onSurfaceVariant = RpTextSecondary,
    outline = RpLightBorder,
    error = RpBlockedRed,
    onError = Color.White
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF1D8CF8),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF16335B),
    onPrimaryContainer = Color.White,
    secondary = Color(0xFF22C55E),
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF14532D),
    onSecondaryContainer = Color(0xFFDCFCE7),
    tertiary = RpCriticalOrange,
    background = RpDarkBackground,
    onBackground = RpDarkTextPrimary,
    surface = RpDarkSurface,
    onSurface = RpDarkTextPrimary,
    surfaceVariant = RpDarkSurfaceVariant,
    onSurfaceVariant = RpDarkTextSecondary,
    outline = RpDarkBorder,
    error = RpBlockedRed,
    onError = Color.White
)

@Composable
fun RoutPilotTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
