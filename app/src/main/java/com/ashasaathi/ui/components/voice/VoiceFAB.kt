package com.ashasaathi.ui.components.voice

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ashasaathi.ui.theme.Primary
import com.ashasaathi.ui.theme.RiskRed
import com.ashasaathi.ui.viewmodel.VoiceViewModel

@Composable
fun VoiceFAB(
    onTranscript: ((String) -> Unit)? = null,
    vm: VoiceViewModel = hiltViewModel()
) {
    val isRecording by vm.isRecording.collectAsState()
    val transcript by vm.transcript.collectAsState()

    LaunchedEffect(transcript) {
        if (transcript.isNotBlank()) onTranscript?.invoke(transcript)
    }

    val pulse by rememberInfiniteTransition(label = "pulse").animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(tween(600), RepeatMode.Reverse),
        label = "scale"
    )

    Column(horizontalAlignment = Alignment.End) {
        AnimatedVisibility(visible = transcript.isNotBlank() && !isRecording) {
            Card(
                modifier = Modifier.widthIn(max = 260.dp).padding(bottom = 8.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Text(
                    transcript,
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        FloatingActionButton(
            onClick = { if (isRecording) vm.stopRecording() else vm.startRecording() },
            containerColor = if (isRecording) RiskRed else Primary,
            modifier = if (isRecording) Modifier.scale(pulse) else Modifier
        ) {
            Icon(
                imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                contentDescription = if (isRecording) "Stop Recording" else "Voice Input",
                tint = Color.White
            )
        }
    }
}
