package com.ashasaathi.service.ai

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

enum class VoiceStage {
    IDLE, RECORDING, TRANSCRIBING, EXTRACTING, DONE, ERROR
}

data class VoicePipelineState(
    val stage: VoiceStage = VoiceStage.IDLE,
    val transcript: String = "",
    val extracted: ExtractedVisitData? = null,
    val error: String? = null
)

@Singleton
class VoiceAIPipeline @Inject constructor(
    @ApplicationContext private val context: Context,
    val whisper: WhisperService,
    val llama: LlamaService
) {
    private val _state = MutableStateFlow(VoicePipelineState())
    val state: StateFlow<VoicePipelineState> = _state

    val isRecording: StateFlow<Boolean> = whisper.isRecording

    fun startRecording(lang: String = "hi") {
        _state.value = VoicePipelineState(stage = VoiceStage.RECORDING)
        if (!whisper.isRecording.value) whisper.startRecording(lang)
    }

    suspend fun stopAndProcess(): ExtractedVisitData {
        _state.value = _state.value.copy(stage = VoiceStage.TRANSCRIBING)

        val transcript = runCatching { whisper.stopRecordingAndTranscribe() }
            .getOrElse { "" }

        if (transcript.isBlank()) {
            _state.value = VoicePipelineState(stage = VoiceStage.ERROR, error = "Transcription empty")
            return ExtractedVisitData()
        }

        _state.value = _state.value.copy(stage = VoiceStage.EXTRACTING, transcript = transcript)

        val extracted = runCatching { llama.extractVisitData(transcript) }
            .getOrElse { llama.extractWithRules(transcript) }

        _state.value = VoicePipelineState(
            stage = VoiceStage.DONE,
            transcript = transcript,
            extracted = extracted
        )
        return extracted
    }

    fun reset() {
        whisper.clearTranscript()
        _state.value = VoicePipelineState()
    }
}
