package com.ashasaathi.ui.screens.setup

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ashasaathi.ui.theme.*
import com.ashasaathi.ui.viewmodel.ModelSetupViewModel

@Composable
fun ModelSetupScreen(
    onSetupComplete: () -> Unit,
    vm: ModelSetupViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsState()

    LaunchedEffect(state.allReady) {
        if (state.allReady) onSetupComplete()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Saffron, SaffronDark))),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text("🌿", style = MaterialTheme.typography.displayMedium)
            Text(
                "आशा साथी सेटअप",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Text(
                "AI मॉडल डाउनलोड हो रहे हैं\nFirst-time setup — downloading AI models",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.85f),
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(8.dp))

            // Whisper card
            ModelCard(
                title        = "Voice Recognition (Whisper)",
                subtitle     = "Hindi / Kannada / English STT — ~75 MB",
                isReady      = state.whisperReady,
                isDownloading= state.whisperDownloading,
                progress     = state.whisperProgress,
                error        = state.whisperError,
                onDownload   = vm::downloadWhisper
            )

            // LLaMA card
            ModelCard(
                title        = "AI Assistant (Qwen 2.5 1.5B)",
                subtitle     = "Smart field extraction — ~870 MB",
                isReady      = state.llamaReady,
                isDownloading= state.llamaDownloading,
                progress     = state.llamaProgress,
                error        = state.llamaError,
                onDownload   = vm::downloadLlama,
                skipLabel    = "Skip (use keyword mode)",
                onSkip       = vm::skipLlama
            )

            Spacer(Modifier.height(8.dp))

            Button(
                onClick  = vm::downloadAll,
                enabled  = !state.whisperDownloading && !state.llamaDownloading,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = Color.White),
                shape    = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Default.Download, null, tint = SaffronDark)
                Spacer(Modifier.width(8.dp))
                Text("सब डाउनलोड करें / Download All", color = SaffronDark, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ModelCard(
    title: String,
    subtitle: String,
    isReady: Boolean,
    isDownloading: Boolean,
    progress: Int,
    error: String?,
    onDownload: () -> Unit,
    skipLabel: String? = null,
    onSkip: (() -> Unit)? = null
) {
    Card(
        shape  = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.15f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(title, color = Color.White, fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.bodyLarge)
                    Text(subtitle, color = Color.White.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.bodySmall)
                }
                when {
                    isReady      -> Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF81C784), modifier = Modifier.size(28.dp))
                    error != null -> Icon(Icons.Default.Warning, null, tint = Color(0xFFFFB74D), modifier = Modifier.size(28.dp))
                }
            }

            AnimatedVisibility(isDownloading) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    LinearProgressIndicator(
                        progress    = { progress / 100f },
                        modifier    = Modifier.fillMaxWidth(),
                        color       = Color.White,
                        trackColor  = Color.White.copy(alpha = 0.3f)
                    )
                    Text("$progress%", color = Color.White.copy(alpha = 0.8f),
                        style = MaterialTheme.typography.labelSmall)
                }
            }

            error?.let {
                Text("Error: $it", color = Color(0xFFFFB74D), style = MaterialTheme.typography.bodySmall)
            }

            if (!isReady && !isDownloading) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick  = onDownload,
                        modifier = Modifier.weight(1f),
                        colors   = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        border   = androidx.compose.foundation.BorderStroke(1.dp, Color.White)
                    ) { Text("Download") }

                    if (skipLabel != null && onSkip != null) {
                        TextButton(onClick = onSkip) {
                            Text(skipLabel, color = Color.White.copy(alpha = 0.7f),
                                style = MaterialTheme.typography.labelSmall,
                                textAlign = TextAlign.Center)
                        }
                    }
                }
            }
        }
    }
}
