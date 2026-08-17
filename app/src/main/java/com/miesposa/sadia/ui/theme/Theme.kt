package com.miesposa.sadia.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Deep-red / purple premium palette on a near-black background
val SadiaBackground = Color(0xFF0A0007)
val SadiaSurface = Color(0xFF160B12)
val SadiaCard = Color(0xFF1E0F1B)
val SadiaDeepRed = Color(0xFFB8123F)
val SadiaPurple = Color(0xFF6C2BD9)
val SadiaGlow = Color(0xFFE83E8C)
val SadiaTextPrimary = Color(0xFFF5EAF2)
val SadiaTextSecondary = Color(0xFFB79CB0)

private val SadiaColorScheme = darkColorScheme(
    primary = SadiaDeepRed,
    secondary = SadiaPurple,
    tertiary = SadiaGlow,
    background = SadiaBackground,
    surface = SadiaSurface,
    onPrimary = SadiaTextPrimary,
    onSecondary = SadiaTextPrimary,
    onBackground = SadiaTextPrimary,
    onSurface = SadiaTextPrimary
)

@Composable
fun MiEsposaTheme(content: @Composable () -> Unit) {
    // Dark theme only — matches the "black + deep red + purple, premium, minimal" brief.
    val colors = SadiaColorScheme
    MaterialTheme(
        colorScheme = colors,
        typography = MaterialTheme.typography,
        content = content
    )
}
