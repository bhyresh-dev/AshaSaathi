package com.ashasaathi.service.ai

import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WhisperService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val _isRecording  = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording

    private val _transcript   = MutableStateFlow("")
    val transcript: StateFlow<String> = _transcript

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing

    private var audioRecord: AudioRecord? = null
    private var recordingBuffer = mutableListOf<Short>()
    private var modelLoaded = false

    private val sampleRate = 16_000
    private val bufferSize = AudioRecord.getMinBufferSize(
        sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT
    ).coerceAtLeast(4096)

    companion object {
        private const val MODEL_FILE = "ggml-tiny-q5_1.bin"
        init { runCatching { System.loadLibrary("ashasaathi_jni") } }
    }

    private external fun nativeLoadModel(modelPath: String): Boolean
    private external fun nativeTranscribe(pcmData: FloatArray, sampleRate: Int): String
    private external fun nativeFreeModel()

    suspend fun loadModel() = withContext(Dispatchers.IO) {
        if (modelLoaded) return@withContext
        runCatching {
            val dest = File(context.filesDir, MODEL_FILE)
            if (!dest.exists()) {
                context.assets.open(MODEL_FILE).use { inp ->
                    FileOutputStream(dest).use { out -> inp.copyTo(out) }
                }
            }
            modelLoaded = nativeLoadModel(dest.absolutePath)
        }
    }

    fun startRecording() {
        if (_isRecording.value) return
        recordingBuffer.clear()
        _transcript.value = ""

        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC, sampleRate,
            AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize
        )
        audioRecord?.startRecording()
        _isRecording.value = true

        Thread {
            val buf = ShortArray(bufferSize / 2)
            while (_isRecording.value) {
                val read = audioRecord?.read(buf, 0, buf.size) ?: 0
                if (read > 0) recordingBuffer.addAll(buf.take(read))
            }
        }.also { it.isDaemon = true }.start()
    }

    suspend fun stopRecordingAndTranscribe(): String = withContext(Dispatchers.IO) {
        _isRecording.value = false
        audioRecord?.apply { stop(); release() }
        audioRecord = null
        _isProcessing.value = true

        val result = if (modelLoaded) {
            val pcm = pcm16ToFloat(recordingBuffer.toShortArray())
            nativeTranscribe(pcm, sampleRate)
        } else {
            ""
        }

        _transcript.value = result.trim()
        _isProcessing.value = false
        result.trim()
    }

    private fun pcm16ToFloat(samples: ShortArray): FloatArray {
        val out = FloatArray(samples.size)
        for (i in samples.indices) out[i] = samples[i] / 32768f
        return out
    }

    fun clearTranscript() { _transcript.value = "" }

    fun release() {
        if (modelLoaded) { nativeFreeModel(); modelLoaded = false }
    }
}
