package com.telefam.company.desktop.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val TelefamCrimson = Color(0xFFC80E18)
val TelefamCrimsonDark = Color(0xFF9F0A12)
val TelefamBlush = Color(0xFFFDF2F2)
val TelefamBlushTint = Color(0x22C80E18)

val TelefamDarkBg = Color(0xFF121214)
val TelefamDarkSurface = Color(0xFF1C1D22)
val TelefamDarkSurfaceVariant = Color(0xFF25262C)
val TelefamDarkOutline = Color(0xFF33353D)

val TelefamLightBg = Color(0xFFFFFFFF)
val TelefamLightSurface = Color(0xFFFFFFFF)
val TelefamLightSurfaceVariant = Color(0xFFF5F6F8)
val TelefamLightOutline = Color(0xFFE0E2E7)

val TelefamTextPrimaryLight = Color(0xFF1A1C1E)
val TelefamTextSecondaryLight = Color(0xFF6B7280)
val TelefamTextPrimaryDark = Color(0xFFF3F4F6)
val TelefamTextSecondaryDark = Color(0xFF9CA3AF)
val TelefamSuccess = Color(0xFF16A34A)

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

private val DesktopTypography = Typography(
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.2.sp
    )
)

@Composable
fun TelefamDesktopTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = DesktopTypography,
        content = content
    )
}
