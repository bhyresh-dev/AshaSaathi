package com.ashasaathi.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ashasaathi.service.ai.VoiceAIPipeline
import com.ashasaathi.service.ai.VoicePipelineState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VoiceViewModel @Inject constructor(
    private val pipeline: VoiceAIPipeline
) : ViewModel() {

    val pipelineState: StateFlow<VoicePipelineState> = pipeline.state

    fun startRecording() { pipeline.startRecording() }

    fun stopAndProcess() {
        viewModelScope.launch { pipeline.stopAndProcess() }
    }

    fun reset() { pipeline.reset() }

    override fun onCleared() {
        super.onCleared()
        pipeline.reset()
    }
}
