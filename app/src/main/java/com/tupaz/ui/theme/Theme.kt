package com.tupaz.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = VercelTextPrimary,
    onPrimary = VercelBackground,
    primaryContainer = VercelCardSurface,
    onPrimaryContainer = VercelTextPrimary,
    secondary = VercelTextSecondary,
    background = VercelBackground,
    onBackground = VercelTextPrimary,
    surface = VercelSurface,
    onSurface = VercelTextPrimary,
    surfaceVariant = VercelCardSurface,
    onSurfaceVariant = VercelTextSecondary,
    outline = VercelBorder
)

@Composable
fun TupazTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
