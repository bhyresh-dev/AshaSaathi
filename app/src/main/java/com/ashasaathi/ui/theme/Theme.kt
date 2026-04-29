package com.ashasaathi.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Primary = Color(0xFF1B5E20)
val PrimaryDark = Color(0xFF003300)
val PrimaryLight = Color(0xFF4C8C4A)
val Secondary = Color(0xFF2E7D32)
val Background = Color(0xFFF5F5F5)
val Surface = Color(0xFFFFFFFF)
val RiskRed = Color(0xFFD32F2F)
val RiskAmber = Color(0xFFF57F17)
val RiskGreen = Color(0xFF388E3C)
val TextPrimary = Color(0xFF212121)
val TextSecondary = Color(0xFF757575)

private val LightColors = lightColorScheme(
    primary = Primary,
    onPrimary = Color.White,
    primaryContainer = PrimaryLight,
    secondary = Secondary,
    onSecondary = Color.White,
    background = Background,
    surface = Surface,
    error = RiskRed,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
)

@Composable
fun AshaSaathiTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        typography = AppTypography,
        content = content
    )
}
