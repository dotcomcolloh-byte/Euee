package com.telefam.company.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = TelefamCrimson,
    onPrimary = Color.White,
    primaryContainer = TelefamCrimsonDark,
    onPrimaryContainer = Color.White,
    secondary = TelefamCrimson,
    onSecondary = Color.White,
    background = TelefamDarkBg,
    onBackground = TelefamTextPrimaryDark,
    surface = TelefamDarkSurface,
    onSurface = TelefamTextPrimaryDark,
    surfaceVariant = TelefamDarkSurfaceVariant,
    onSurfaceVariant = TelefamTextSecondaryDark,
    outline = TelefamDarkOutline,
    error = Color(0xFFF87171),
    onError = Color.Black
)

private val LightColorScheme = lightColorScheme(
    primary = TelefamCrimson,
    onPrimary = Color.White,
    primaryContainer = TelefamBlush,
    onPrimaryContainer = TelefamCrimson,
    secondary = TelefamCrimson,
    onSecondary = Color.White,
    background = TelefamLightBg,
    onBackground = TelefamTextPrimaryLight,
    surface = TelefamLightSurface,
    onSurface = TelefamTextPrimaryLight,
    surfaceVariant = TelefamLightSurfaceVariant,
    onSurfaceVariant = TelefamTextSecondaryLight,
    outline = TelefamLightOutline,
    error = TelefamCrimson,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
