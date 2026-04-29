package com.ashasaathi.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ashasaathi.service.ai.WhisperService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VoiceViewModel @Inject constructor(
    private val whisperService: WhisperService
) : ViewModel() {

    val isRecording: StateFlow<Boolean> = whisperService.isRecording
    val transcript: StateFlow<String> = whisperService.transcript

    fun startRecording() = whisperService.startRecording()

    fun stopRecording() {
        viewModelScope.launch { whisperService.stopRecordingAndTranscribe() }
    }
}
