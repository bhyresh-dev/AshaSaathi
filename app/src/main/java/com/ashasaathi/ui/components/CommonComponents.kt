package com.ashasaathi.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ashasaathi.ui.LocalAppLanguage
import com.ashasaathi.ui.theme.*

// ── Risk badge ────────────────────────────────────────────────────────────────

@Composable
fun RiskBadge(level: String, modifier: Modifier = Modifier) {
    val lang = LocalAppLanguage.current
    val (color, text) = when (level) {
        "RED"    -> RiskRed to when (lang) {
            "en" -> "High Risk"; "kn" -> "ಅಧಿಕ ಅಪಾಯ"; else -> "उच्च जोखिम"
        }
        "YELLOW" -> RiskAmber to when (lang) {
            "en" -> "Moderate"; "kn" -> "ಮಧ್ಯಮ"; else -> "मध्यम"
        }
        else     -> RiskGreen to when (lang) {
            "en" -> "Normal"; "kn" -> "ಸಾಮಾನ್ಯ"; else -> "सामान्य"
        }
    }
    Surface(
        modifier = modifier,
        color = color.copy(alpha = 0.10f),
        shape = RoundedCornerShape(100),
        border = BorderStroke(1.dp, color.copy(alpha = 0.25f))
    ) {
        Row(
            Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Text(
                text,
                style = MaterialTheme.typography.labelSmall,
                color = color,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

// ── Metric card ───────────────────────────────────────────────────────────────

@Composable
fun MetricCard(
    icon: String,
    labelHi: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(0.dp),
        border = BorderStroke(1.dp, CardBorder)
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(color.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Text(icon, fontSize = 20.sp)
            }
            Text(
                value,
                style = MaterialTheme.typography.headlineMedium,
                color = color,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                labelHi,
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                lineHeight = 14.sp
            )
        }
    }
}

// ── Section header ────────────────────────────────────────────────────────────

@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            Modifier
                .width(4.dp)
                .height(18.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(
                    Brush.verticalGradient(listOf(SaffronGlow, SaffronDark))
                )
        )
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
    }
}

// ── Skeleton loader ───────────────────────────────────────────────────────────

@Composable
fun SkeletonBox(
    modifier: Modifier = Modifier,
    height: Dp = 16.dp,
    cornerRadius: Dp = 8.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val shimmerX by infiniteTransition.animateFloat(
        initialValue = -500f,
        targetValue  = 1500f,
        animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing)),
        label = "shimmerX"
    )
    Box(
        modifier = modifier
            .height(height)
            .clip(RoundedCornerShape(cornerRadius))
            .background(
                Brush.linearGradient(
                    colors = listOf(Color(0xFFEDE8E3), Color(0xFFF5F2EE), Color(0xFFEDE8E3)),
                    start  = Offset(shimmerX, 0f),
                    end    = Offset(shimmerX + 500f, 0f)
                )
            )
    )
}

@Composable
fun CardSkeleton(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(0.dp),
        border = BorderStroke(1.dp, CardBorder)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SkeletonBox(Modifier.fillMaxWidth(0.6f), height = 18.dp)
            SkeletonBox(Modifier.fillMaxWidth(0.9f), height = 14.dp)
            SkeletonBox(Modifier.fillMaxWidth(0.45f), height = 14.dp)
        }
    }
}

// ── Empty state ───────────────────────────────────────────────────────────────

@Composable
fun EmptyState(emoji: String, titleHi: String, subtitleEn: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(SurfaceCard)
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(emoji, fontSize = 52.sp, textAlign = TextAlign.Center)
            Text(
                titleHi,
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
            Text(
                subtitleEn,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ── Info row ──────────────────────────────────────────────────────────────────

@Composable
fun InfoRow(label: String, value: String) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
            modifier = Modifier.weight(1f)
        )
        Box(
            Modifier
                .height(12.dp)
                .width(1.dp)
                .background(Divider)
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            color = TextPrimary,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1.2f).padding(start = 12.dp),
            textAlign = TextAlign.End
        )
    }
}

// ── Chip ──────────────────────────────────────────────────────────────────────

@Composable
fun ColorChip(text: String, color: Color, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(100),
        color = color.copy(alpha = 0.10f),
        border = BorderStroke(0.5.dp, color.copy(alpha = 0.30f))
    ) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.SemiBold
        )
    }
}

// ── Primary top bar gradient ──────────────────────────────────────────────────

@Composable
fun SaffronGradient(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier = modifier.background(
            Brush.linearGradient(
                listOf(SaffronGlow, Saffron, SaffronDark),
                start = Offset(0f, 0f),
                end   = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
            )
        ),
        content = content
    )
}

// ── FAB with pulse animation ──────────────────────────────────────────────────

@Composable
fun PulseFAB(
    onClick: () -> Unit,
    containerColor: Color,
    icon: ImageVector,
    pulsing: Boolean = false,
    contentDescription: String = ""
) {
    val scale by rememberInfiniteTransition(label = "pulse").animateFloat(
        initialValue = 1f,
        targetValue  = if (pulsing) 1.14f else 1f,
        animationSpec = infiniteRepeatable(tween(if (pulsing) 700 else 999999), RepeatMode.Reverse),
        label = "scale"
    )
    Box(contentAlignment = Alignment.Center) {
        if (pulsing) {
            Box(
                Modifier
                    .size((68 * scale).dp)
                    .clip(CircleShape)
                    .background(containerColor.copy(alpha = 0.18f))
            )
        }
        FloatingActionButton(
            onClick        = onClick,
            containerColor = containerColor,
            shape          = CircleShape,
            modifier       = Modifier.size(56.dp)
        ) {
            Icon(icon, contentDescription, tint = Color.White)
        }
    }
}
