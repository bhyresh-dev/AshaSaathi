package com.ashasaathi.ui.screens.auth

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ashasaathi.ui.navigation.Route
import com.ashasaathi.ui.theme.Saffron
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    isLoggedIn: Boolean?,
    onNavigate: (String) -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(800),
        label = "splash_alpha"
    )

    LaunchedEffect(Unit) { visible = true }

    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn == null) return@LaunchedEffect
        delay(1200)
        onNavigate(if (isLoggedIn) Route.HOME else Route.LOGIN)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Saffron),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.alpha(alpha)
        ) {
            Text(
                text = "🌿",
                fontSize = 72.sp,
                modifier = Modifier.padding(bottom = 24.dp)
            )
            Text(
                text = "आशा साथी",
                style = MaterialTheme.typography.headlineLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "ASHA Saathi",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White.copy(alpha = 0.8f),
                modifier = Modifier.padding(top = 8.dp)
            )
            Text(
                text = "स्वास्थ्य सेविकाओं का डिजिटल साथी",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.6f),
                modifier = Modifier.padding(top = 16.dp)
            )
        }
    }
}
