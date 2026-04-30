package com.ashasaathi.service.ai

import android.content.Context
import android.content.Intent
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.io.FileOutputStream
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

    /** Non-null when STT fails — message shown to user. Reset on next startRecording(). */
    private val _sttError = MutableStateFlow<String?>(null)
    val sttError: StateFlow<String?> = _sttError

    /** True only when device has no speech recognition engine at all. */
    private val _modelNotReady = MutableStateFlow(false)
    val modelNotReady: StateFlow<Boolean> = _modelNotReady

    // Whisper JNI
    private var audioRecord: AudioRecord? = null
    private var recordingBuffer = mutableListOf<Short>()
    private var modelLoaded = false

    // Android STT
    private var speechRecognizer: SpeechRecognizer? = null
    private var pendingResult = CompletableDeferred<String>()
    private var usingAndroidStt = false
    private var isContinuous = false
    private val accumulated = StringBuilder()
    private var transientRetryCount = 0
    private val mainHandler = Handler(Looper.getMainLooper())

    private val sampleRate = 16_000
    private val bufferSize = AudioRecord.getMinBufferSize(
        sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT
    ).coerceAtLeast(4096)

    companion object {
        private const val MODEL_FILE = "ggml-tiny-q5_1.bin"
        private const val RESTART_DELAY_MS = 300L

        init { runCatching { System.loadLibrary("ashasaathi_jni") } }

        fun langToLocale(lang: String) = when (lang) {
            "kn" -> "kn-IN"
            "en" -> "en-IN"
            else -> "hi-IN"
        }
    }

    private external fun nativeLoadModel(modelPath: String): Boolean
    private external fun nativeTranscribe(pcmData: FloatArray, sampleRate: Int): String
    private external fun nativeFreeModel()

    /** Load Whisper model — background, silent. Falls back to Android STT if not loaded. */
    suspend fun loadModel() = withContext(Dispatchers.IO) {
        if (modelLoaded) return@withContext
        runCatching {
            val dest = File(context.filesDir, MODEL_FILE)
            if (!dest.exists()) {
                runCatching {
                    context.assets.open(MODEL_FILE).use { inp ->
                        FileOutputStream(dest).use { out -> inp.copyTo(out) }
                    }
                }
            }
            if (dest.exists()) modelLoaded = nativeLoadModel(dest.absolutePath)
        }
    }

    /**
     * Start recording immediately — does NOT wait for Whisper model load.
     * Uses Whisper JNI if model already loaded, Android STT otherwise.
     * Call this from the main thread directly; do NOT wrap in a coroutine that first loads the model.
     */
    fun startRecording(lang: String = "hi") {
        if (_isRecording.value) return
        _sttError.value = null
        _modelNotReady.value = false
        accumulated.clear()
        _transcript.value = ""
        transientRetryCount = 0

        if (modelLoaded) {
            startWhisperRecording()
            return
        }

        // Android STT path — works online and on most devices offline (on-device language packs)
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            _modelNotReady.value = true
            return
        }
        startAndroidSttRecording()
    }

    // ── Whisper JNI path ─────────────────────────────────────────────────────

    private fun startWhisperRecording() {
        usingAndroidStt = false
        recordingBuffer.clear()
        runCatching {
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
        }.onFailure { e ->
            _sttError.value = "माइक खुला नहीं। Mic error: ${e.message}"
        }
    }

    // ── Android STT path ─────────────────────────────────────────────────────

    private fun startAndroidSttRecording() {
        usingAndroidStt = true
        isContinuous = true
        pendingResult = CompletableDeferred()
        // Set isRecording BEFORE posting to handler so UI updates immediately
        _isRecording.value = true
        mainHandler.post { createAndStartRecognizer() }
    }

    private fun createAndStartRecognizer() {
        if (!isContinuous) return
        runCatching {
            speechRecognizer?.destroy()
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
            if (speechRecognizer == null) {
                _sttError.value = "SpeechRecognizer बना नहीं। Please check microphone permission."
                _isRecording.value = false
                return
            }
            speechRecognizer!!.setRecognitionListener(buildListener())
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                // No EXTRA_LANGUAGE — use device default (avoids ERROR_LANGUAGE_UNAVAILABLE on all devices)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 1000L)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 4000L)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 3000L)
                // Suppresses the "listening" beep on many OEM ROMs
                putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
            }
            // Mute notification/ring streams to silence the STT activation beep
            val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            am.adjustStreamVolume(AudioManager.STREAM_NOTIFICATION, AudioManager.ADJUST_MUTE, 0)
            am.adjustStreamVolume(AudioManager.STREAM_RING, AudioManager.ADJUST_MUTE, 0)
            speechRecognizer!!.startListening(intent)
            // Restore volume after beep would have played (~200ms)
            mainHandler.postDelayed({
                am.adjustStreamVolume(AudioManager.STREAM_NOTIFICATION, AudioManager.ADJUST_UNMUTE, 0)
                am.adjustStreamVolume(AudioManager.STREAM_RING, AudioManager.ADJUST_UNMUTE, 0)
            }, 200L)
        }.onFailure { e ->
            _sttError.value = "रिकॉर्डिंग शुरू नहीं हुई। Recording failed: ${e.message}"
            _isRecording.value = false
        }
    }

    private fun buildListener() = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {}
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEvent(eventType: Int, params: Bundle?) {}
        override fun onBeginningOfSpeech() {}
        override fun onEndOfSpeech() { _isProcessing.value = true }

        override fun onPartialResults(partials: Bundle) {
            val partial = partials.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull()?.trim() ?: return
            if (partial.isNotBlank()) {
                _transcript.value = if (accumulated.isNotEmpty()) "${accumulated.trim()} $partial" else partial
            }
        }

        override fun onResults(results: Bundle) {
            _isProcessing.value = false
            transientRetryCount = 0
            val text = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull()?.trim() ?: ""
            if (text.isNotBlank()) {
                if (accumulated.isNotEmpty()) accumulated.append(" ")
                accumulated.append(text)
                _transcript.value = accumulated.toString()
            }
            if (isContinuous) {
                mainHandler.postDelayed({ createAndStartRecognizer() }, RESTART_DELAY_MS)
            } else {
                finishRecording()
            }
        }

        override fun onError(error: Int) {
            _isProcessing.value = false
            when (error) {
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT,
                SpeechRecognizer.ERROR_NO_MATCH -> {
                    // Normal silence or no speech detected — restart, not an error
                    if (isContinuous) mainHandler.postDelayed({ createAndStartRecognizer() }, RESTART_DELAY_MS)
                    else finishRecording()
                }
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> {
                    // Previous session still alive — destroy it then retry
                    mainHandler.postDelayed({
                        destroyRecognizer()
                        if (isContinuous) createAndStartRecognizer()
                    }, 600L)
                }
                SpeechRecognizer.ERROR_NETWORK,
                SpeechRecognizer.ERROR_NETWORK_TIMEOUT,
                SpeechRecognizer.ERROR_SERVER,
                11 /* ERROR_SERVER_DISCONNECTED — API 23+ */ -> {
                    transientRetryCount++
                    if (transientRetryCount <= 3) {
                        if (isContinuous) mainHandler.postDelayed({ createAndStartRecognizer() }, 1500L)
                        else finishRecording()
                    } else {
                        _sttError.value = "इंटरनेट नहीं है। No internet — voice needs connection. Download offline model from Settings."
                        finishRecording()
                    }
                }
                SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED,
                13 /* ERROR_LANGUAGE_UNAVAILABLE — API 31+ */ -> {
                    // Shouldn't happen since we no longer set EXTRA_LANGUAGE.
                    // If it still does, just restart — device default always works.
                    if (isContinuous) mainHandler.postDelayed({ createAndStartRecognizer() }, RESTART_DELAY_MS)
                    else finishRecording()
                }
                SpeechRecognizer.ERROR_AUDIO -> {
                    // Audio hardware issue — surface as error
                    _sttError.value = "ऑडियो एरर। Audio error — check mic permission and try again."
                    finishRecording()
                }
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> {
                    _sttError.value = "RECORD_AUDIO अनुमति नहीं है। Grant microphone permission."
                    finishRecording()
                }
                else -> {
                    // Unknown error — surface code for debugging
                    _sttError.value = "STT एरर $error। STT error $error — tap mic to retry."
                    finishRecording()
                }
            }
        }
    }

    private fun finishRecording() {
        isContinuous = false
        _isRecording.value = false
        _isProcessing.value = false
        destroyRecognizer()
        val final = accumulated.toString().trim()
        _transcript.value = final
        accumulated.clear()
        if (!pendingResult.isCompleted) pendingResult.complete(final)
    }

    // ── Stop & transcribe ─────────────────────────────────────────────────────

    suspend fun stopRecordingAndTranscribe(): String {
        return if (usingAndroidStt) {
            isContinuous = false
            withContext(Dispatchers.Main) {
                runCatching { speechRecognizer?.stopListening() }
            }
            val result = withTimeoutOrNull(4_000L) {
                runCatching { pendingResult.await() }.getOrElse { "" }
            } ?: run {
                val acc = accumulated.toString().trim()
                if (!pendingResult.isCompleted) pendingResult.complete(acc)
                acc
            }
            _isRecording.value = false
            _isProcessing.value = false
            destroyRecognizer()
            _transcript.value = result
            result
        } else {
            withContext(Dispatchers.IO) {
                _isRecording.value = false
                audioRecord?.apply { runCatching { stop(); release() } }
                audioRecord = null
                if (!modelLoaded || recordingBuffer.isEmpty()) return@withContext ""
                _isProcessing.value = true
                val pcm = pcm16ToFloat(recordingBuffer.toShortArray())
                val result = runCatching { nativeTranscribe(pcm, sampleRate) }.getOrElse { "" }
                recordingBuffer.clear()
                _transcript.value = result.trim()
                _isProcessing.value = false
                result.trim()
            }
        }
    }

    private fun destroyRecognizer() {
        runCatching { speechRecognizer?.destroy() }
        speechRecognizer = null
    }

    private fun pcm16ToFloat(samples: ShortArray): FloatArray {
        val out = FloatArray(samples.size)
        for (i in samples.indices) out[i] = samples[i] / 32768f
        return out
    }

    fun clearTranscript() {
        accumulated.clear()
        _transcript.value = ""
    }

    fun release() {
        isContinuous = false
        _isRecording.value = false
        audioRecord?.apply { runCatching { stop(); release() } }
        audioRecord = null
        if (modelLoaded) { runCatching { nativeFreeModel() }; modelLoaded = false }
        mainHandler.post { destroyRecognizer() }
    }
}
