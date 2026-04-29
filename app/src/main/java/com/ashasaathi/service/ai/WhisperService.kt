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
    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording

    private val _transcript = MutableStateFlow("")
    val transcript: StateFlow<String> = _transcript

    private var audioRecord: AudioRecord? = null
    private var recordingBuffer = mutableListOf<Short>()

    private val sampleRate = 16000
    private val bufferSize = AudioRecord.getMinBufferSize(
        sampleRate,
        AudioFormat.CHANNEL_IN_MONO,
        AudioFormat.ENCODING_PCM_16BIT
    )

    fun startRecording() {
        if (_isRecording.value) return
        recordingBuffer.clear()
        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSize
        )
        audioRecord?.startRecording()
        _isRecording.value = true

        Thread {
            val buffer = ShortArray(bufferSize / 2)
            while (_isRecording.value) {
                val read = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                if (read > 0) recordingBuffer.addAll(buffer.take(read))
            }
        }.start()
    }

    suspend fun stopRecordingAndTranscribe(): String = withContext(Dispatchers.IO) {
        _isRecording.value = false
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null

        val pcmFile = saveAsPcm(recordingBuffer.toShortArray())
        val result = transcribeWithWhisper(pcmFile)
        _transcript.value = result
        result
    }

    private fun saveAsPcm(samples: ShortArray): File {
        val file = File(context.cacheDir, "recording.pcm")
        FileOutputStream(file).use { fos ->
            val buffer = ByteBuffer.allocate(samples.size * 2).order(ByteOrder.LITTLE_ENDIAN)
            samples.forEach { buffer.putShort(it) }
            fos.write(buffer.array())
        }
        return file
    }

    private fun transcribeWithWhisper(pcmFile: File): String {
        // Whisper.cpp integration via JNI or native library
        // Load model from assets/ggml-tiny.en-q5_1.bin
        val modelFile = File(context.filesDir, "ggml-tiny.en-q5_1.bin")
        if (!modelFile.exists()) {
            copyModelFromAssets(modelFile)
        }
        // Call native whisper transcription
        return runCatching {
            nativeTranscribe(modelFile.absolutePath, pcmFile.absolutePath)
        }.getOrElse { "Transcription failed: ${it.message}" }
    }

    private fun copyModelFromAssets(dest: File) {
        runCatching {
            context.assets.open("ggml-tiny.en-q5_1.bin").use { input ->
                FileOutputStream(dest).use { output -> input.copyTo(output) }
            }
        }
    }

    private external fun nativeTranscribe(modelPath: String, audioPath: String): String

    companion object {
        init {
            runCatching { System.loadLibrary("whisper") }
        }
    }
}
