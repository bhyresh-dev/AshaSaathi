package com.ashasaathi.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ── Saffron + Teal palette ────────────────────────────────────────────────────
val Saffron         = Color(0xFFE8650A)   // warm saffron orange — primary
val SaffronDark     = Color(0xFFB54C00)
val SaffronGlow     = Color(0xFFFF7A2F)   // brighter stop for rich gradients
val SaffronLight    = Color(0xFFFFB07A)
val SaffronContainer= Color(0xFFFFE0C4)

val Teal            = Color(0xFF00695C)   // deep teal — secondary
val TealDark        = Color(0xFF004D45)
val TealLight       = Color(0xFF4D9A8C)
val TealContainer   = Color(0xFFB2DFDB)

val WarmWhite       = Color(0xFFFFFDF8)
val WarmSurface     = Color(0xFFFFFFFF)
val WarmBackground  = Color(0xFFF7F3EE)   // slightly deeper warm bg
val SurfaceCard     = Color(0xFFFFFCF9)   // warm card surface
val CardBorder      = Color(0xFFEDE7DF)   // subtle warm border

val RiskRed         = Color(0xFFD32F2F)
val RiskRedSurface  = Color(0xFFFFEBEE)
val RiskAmber       = Color(0xFFF57F17)
val RiskAmberSurface= Color(0xFFFFF8E1)
val RiskGreen       = Color(0xFF2E7D32)
val RiskGreenSurface= Color(0xFFE8F5E9)

val TextPrimary     = Color(0xFF1C1917)
val TextSecondary   = Color(0xFF78716C)
val TextHint        = Color(0xFFBDBDBD)
val Divider         = Color(0xFFEDE8E3)

private val LightColors = lightColorScheme(
    primary          = Saffron,
    onPrimary        = Color.White,
    primaryContainer = SaffronContainer,
    onPrimaryContainer = SaffronDark,
    secondary        = Teal,
    onSecondary      = Color.White,
    secondaryContainer = TealContainer,
    onSecondaryContainer = TealDark,
    background       = WarmBackground,
    onBackground     = TextPrimary,
    surface          = WarmSurface,
    onSurface        = TextPrimary,
    surfaceVariant   = Color(0xFFF5F0EB),
    onSurfaceVariant = TextSecondary,
    error            = RiskRed,
    onError          = Color.White,
    errorContainer   = RiskRedSurface,
    outline          = Color(0xFFD0C8C0),
)

@Composable
fun AshaSaathiTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        typography  = AppTypography,
        content     = content
    )
}
