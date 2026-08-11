package com.tupaz.ui.theme

import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

val LocalButtonSize = compositionLocalOf { ButtonSize.NORMAL }

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
    buttonSize: ButtonSize = ButtonSize.NORMAL,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalButtonSize provides buttonSize) {
        MaterialTheme(
            colorScheme = DarkColorScheme,
            typography = Typography,
            content = content
        )
    }
}

@Composable
fun Modifier.accessibleButtonSize(defaultHeight: Dp = 48.dp): Modifier {
    val buttonSize = LocalButtonSize.current
    val scale = buttonSize.scale
    val scaledHeight = (defaultHeight * scale).coerceAtLeast(36.dp)
    return this.then(Modifier.height(scaledHeight))
}

