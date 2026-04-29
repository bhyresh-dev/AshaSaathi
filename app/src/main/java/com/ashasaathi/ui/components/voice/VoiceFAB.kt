package com.ashasaathi.ui.components.voice

import android.Manifest
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ashasaathi.service.ai.ExtractedVisitData
import com.ashasaathi.service.ai.VoiceStage
import com.ashasaathi.ui.theme.RiskRed
import com.ashasaathi.ui.theme.Saffron
import com.ashasaathi.ui.theme.TextSecondary
import com.ashasaathi.ui.viewmodel.VoiceViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun VoiceFAB(
    onExtracted: ((ExtractedVisitData) -> Unit)? = null,
    vm: VoiceViewModel = hiltViewModel()
) {
    val micPermission = rememberPermissionState(Manifest.permission.RECORD_AUDIO)
    val state  by vm.pipelineState.collectAsState()
    val isRec  = state.stage == VoiceStage.RECORDING
    val isBusy = state.stage == VoiceStage.TRANSCRIBING || state.stage == VoiceStage.EXTRACTING

    LaunchedEffect(state.extracted) {
        state.extracted?.let { onExtracted?.invoke(it) }
    }

    val pulse by rememberInfiniteTransition(label = "pulse").animateFloat(
        initialValue = 1f,
        targetValue  = if (isRec) 1.15f else 1f,
        animationSpec = infiniteRepeatable(tween(if (isRec) 600 else 999_999), RepeatMode.Reverse),
        label = "scale"
    )

    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Permission rationale snackbar
        AnimatedVisibility(
            visible = !micPermission.status.isGranted && micPermission.status.shouldShowRationale
        ) {
            Card(
                modifier = Modifier.widthIn(max = 260.dp),
                shape    = RoundedCornerShape(16.dp),
                colors   = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(6.dp)
            ) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("माइक की अनुमति चाहिए / Microphone needed",
                        style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    TextButton(
                        onClick  = { micPermission.launchPermissionRequest() },
                        modifier = Modifier.align(Alignment.End)
                    ) { Text("Allow", color = Saffron) }
                }
            }
        }

        // Transcript / status bubble
        AnimatedVisibility(
            visible = state.transcript.isNotBlank() || isBusy,
            enter   = slideInVertically { it } + fadeIn(),
            exit    = slideOutVertically { it } + fadeOut()
        ) {
            Card(
                modifier  = Modifier.widthIn(max = 260.dp),
                shape     = RoundedCornerShape(16.dp),
                colors    = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(6.dp)
            ) {
                Row(
                    Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (isBusy) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp), color = Saffron, strokeWidth = 2.dp)
                        Text(
                            if (state.stage == VoiceStage.TRANSCRIBING) "सुन रहा हूँ..." else "समझ रहा हूँ...",
                            style = MaterialTheme.typography.bodySmall, color = Saffron
                        )
                    } else if (state.transcript.isNotBlank()) {
                        Text(
                            state.transcript.take(120) + if (state.transcript.length > 120) "…" else "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = {
                when {
                    isRec || isBusy -> vm.stopAndProcess()
                    micPermission.status.isGranted -> vm.startRecording()
                    else -> micPermission.launchPermissionRequest()
                }
            },
            containerColor = when {
                isRec  -> RiskRed
                isBusy -> Saffron.copy(alpha = 0.7f)
                else   -> Saffron
            },
            modifier = Modifier.scale(pulse)
        ) {
            when {
                isBusy -> CircularProgressIndicator(
                    Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp)
                !micPermission.status.isGranted -> Icon(
                    Icons.Default.MicOff, "Mic permission needed", tint = Color.White)
                isRec -> Icon(Icons.Default.Stop, "Stop recording", tint = Color.White)
                else  -> Icon(Icons.Default.Mic, "Voice input", tint = Color.White)
            }
        }
    }
}
