package com.ashasaathi.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ashasaathi.ui.theme.*

// ── Risk badge ────────────────────────────────────────────────────────────────

@Composable
fun RiskBadge(level: String, modifier: Modifier = Modifier) {
    val (color, text) = when (level) {
        "RED"    -> RiskRed to "🔴 उच्च जोखिम"
        "YELLOW" -> RiskAmber to "🟡 मध्यम"
        else     -> RiskGreen to "🟢 सामान्य"
    }
    Surface(
        modifier = modifier,
        color = color.copy(alpha = 0.12f),
        shape = RoundedCornerShape(100)
    ) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium,
            color = color,
            fontWeight = FontWeight.SemiBold
        )
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
        modifier = modifier.shadow(4.dp, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(icon, fontSize = 28.sp)
            Text(value,
                style = MaterialTheme.typography.headlineMedium,
                color = color,
                fontWeight = FontWeight.Bold
            )
            Text(labelHi,
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
    Text(
        title,
        modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        color = TextPrimary
    )
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
                    colors = listOf(
                        Color(0xFFE0E0E0),
                        Color(0xFFF5F5F5),
                        Color(0xFFE0E0E0)
                    ),
                    start = Offset(shimmerX, 0f),
                    end   = Offset(shimmerX + 500f, 0f)
                )
            )
    )
}

@Composable
fun CardSkeleton(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SkeletonBox(Modifier.fillMaxWidth(0.6f), height = 18.dp)
            SkeletonBox(Modifier.fillMaxWidth(0.9f), height = 14.dp)
            SkeletonBox(Modifier.fillMaxWidth(0.5f), height = 14.dp)
        }
    }
}

// ── Empty state ───────────────────────────────────────────────────────────────

@Composable
fun EmptyState(emoji: String, titleHi: String, subtitleEn: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(emoji, fontSize = 56.sp, textAlign = TextAlign.Center)
        Text(titleHi,
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary,
            textAlign = TextAlign.Center
        )
        Text(subtitleEn,
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )
    }
}

// ── Info row ──────────────────────────────────────────────────────────────────

@Composable
fun InfoRow(label: String, value: String) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label,
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
            modifier = Modifier.weight(1f)
        )
        Text(value,
            style = MaterialTheme.typography.bodyMedium,
            color = TextPrimary,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1.2f),
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
        color = color.copy(alpha = 0.12f)
    ) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
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
            Brush.verticalGradient(listOf(Saffron, SaffronDark))
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
        targetValue  = if (pulsing) 1.12f else 1f,
        animationSpec = infiniteRepeatable(tween(if (pulsing) 600 else 999999), RepeatMode.Reverse),
        label = "scale"
    )
    FloatingActionButton(
        onClick = onClick,
        containerColor = containerColor,
        modifier = Modifier.size((56 * scale).dp)
    ) {
        Icon(icon, contentDescription, tint = Color.White)
    }
}
