package com.ashasaathi.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ashasaathi.service.model.DownloadProgress
import com.ashasaathi.service.model.ModelDownloadService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ModelSetupState(
    val whisperReady: Boolean     = false,
    val llamaReady: Boolean       = false,
    val whisperProgress: Int      = 0,
    val llamaProgress: Int        = 0,
    val whisperDownloading: Boolean = false,
    val llamaDownloading: Boolean   = false,
    val whisperError: String?     = null,
    val llamaError: String?       = null,
    val allReady: Boolean         = false
)

@HiltViewModel
class ModelSetupViewModel @Inject constructor(
    private val downloader: ModelDownloadService
) : ViewModel() {

    private val _state = MutableStateFlow(ModelSetupState())
    val state: StateFlow<ModelSetupState> = _state.asStateFlow()

    init { checkReady() }

    private fun checkReady() {
        val w = downloader.isWhisperReady()
        val l = downloader.isLlamaReady()
        _state.value = _state.value.copy(
            whisperReady = w,
            llamaReady   = l,
            allReady     = w && l
        )
    }

    fun downloadWhisper() {
        if (_state.value.whisperDownloading || _state.value.whisperReady) return
        viewModelScope.launch {
            _state.value = _state.value.copy(whisperDownloading = true, whisperError = null)
            downloader.downloadWhisper().collect { p -> onWhisperProgress(p) }
        }
    }

    fun downloadLlama() {
        if (_state.value.llamaDownloading || _state.value.llamaReady) return
        viewModelScope.launch {
            _state.value = _state.value.copy(llamaDownloading = true, llamaError = null)
            downloader.downloadLlama().collect { p -> onLlamaProgress(p) }
        }
    }

    fun downloadAll() {
        downloadWhisper()
        downloadLlama()
    }

    private fun onWhisperProgress(p: DownloadProgress) {
        _state.value = _state.value.copy(
            whisperProgress    = p.percent,
            whisperReady       = p.isDone,
            whisperDownloading = !p.isDone && p.error == null,
            whisperError       = p.error,
            allReady           = p.isDone && _state.value.llamaReady
        )
    }

    private fun onLlamaProgress(p: DownloadProgress) {
        _state.value = _state.value.copy(
            llamaProgress    = p.percent,
            llamaReady       = p.isDone,
            llamaDownloading = !p.isDone && p.error == null,
            llamaError       = p.error,
            allReady         = _state.value.whisperReady && p.isDone
        )
    }

    fun skipLlama() {
        // LLaMA is optional — rule-based fallback works offline
        _state.value = _state.value.copy(
            llamaReady   = true,
            allReady     = _state.value.whisperReady
        )
    }
}
