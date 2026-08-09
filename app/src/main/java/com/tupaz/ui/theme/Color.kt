package com.tupaz.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Vercel (Geist) Monochrome Design System Tokens
val VercelBackground = Color(0xFF000000)
val VercelSurface = Color(0xFF0A0A0A)
val VercelCardSurface = Color(0xFF111111)
val VercelBorder = Color(0xFF222222)
val VercelBorderHighlight = Color(0xFF333333)

val VercelTextPrimary = Color(0xFFFFFFFF)
val VercelTextSecondary = Color(0xFFA1A1A1)
val VercelTextMuted = Color(0xFF666666)

val VercelButtonBackground = Color(0xFFFFFFFF)
val VercelButtonText = Color(0xFF000000)

// Pre-instantiated Brushes for 60fps/120fps smooth scrolling performance (Zero per-frame allocation)
val MetallicBorderBrush = Brush.horizontalGradient(listOf(Color(0xFF3B3B3B), Color(0xFF222222), Color(0xFF3B3B3B)))
val HeroBackgroundBrush = Brush.verticalGradient(listOf(Color(0xFF141414), Color(0xFF0A0A0A)))

// Aliases for compatibility
val DarkBackground = VercelBackground
val DarkSurface = VercelSurface
val DarkCardSurface = VercelCardSurface
val DarkBorder = VercelBorder

val CyanPrimary = Color(0xFFFFFFFF)
val CyanPrimaryVariant = Color(0xFFCCCCCC)
val VioletPrimary = Color(0xFFFFFFFF)
val VioletGradientStart = Color(0xFF111111)
val VioletGradientEnd = Color(0xFF222222)

val TextPrimary = VercelTextPrimary
val TextSecondary = VercelTextSecondary
val TextTertiary = VercelTextMuted

val AccentGreen = Color(0xFF10B981)
val AccentBlue = Color(0xFF3B82F6)
val AccentYellow = Color(0xFFF59E0B)
val AccentCyan = Color(0xFFFFFFFF)
