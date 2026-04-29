package com.ashasaathi.service.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TTSService @Inject constructor(
    @ApplicationContext private val context: Context
) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var ready = false

    private val _speaking = MutableStateFlow(false)
    val speaking: StateFlow<Boolean> = _speaking

    init {
        tts = TextToSpeech(context, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            ready = true
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) { _speaking.value = true }
                override fun onDone(utteranceId: String?) { _speaking.value = false }
                override fun onError(utteranceId: String?) { _speaking.value = false }
            })
        }
    }

    fun speak(text: String, language: String = "hi") {
        if (!ready) return
        val locale = when (language) {
            "hi" -> Locale("hi", "IN")
            "kn" -> Locale("kn", "IN")
            else -> Locale.ENGLISH
        }
        val result = tts?.setLanguage(locale)
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            tts?.setLanguage(Locale.ENGLISH)
        }
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, UUID.randomUUID().toString())
    }

    fun speakRiskResult(riskLevel: String, flags: List<String>, language: String = "hi") {
        val text = when (language) {
            "hi" -> buildHindiRiskSpeech(riskLevel, flags)
            "kn" -> buildKannadaRiskSpeech(riskLevel, flags)
            else -> buildEnglishRiskSpeech(riskLevel, flags)
        }
        speak(text, language)
    }

    private fun buildHindiRiskSpeech(level: String, flags: List<String>): String {
        val base = when (level) {
            "RED"    -> "चेतावनी! यह मरीज़ उच्च जोखिम में है। "
            "YELLOW" -> "ध्यान दें। इस मरीज़ की निगरानी जरूरी है। "
            else     -> "अच्छा! मरीज़ ठीक लग रहा है। "
        }
        val detail = flags.firstOrNull() ?: ""
        return base + detail
    }

    private fun buildKannadaRiskSpeech(level: String, flags: List<String>): String {
        val base = when (level) {
            "RED"    -> "ಎಚ್ಚರಿಕೆ! ಈ ರೋಗಿ ಹೆಚ್ಚಿನ ಅಪಾಯದಲ್ಲಿದ್ದಾರೆ. "
            "YELLOW" -> "ಗಮನಿಸಿ. ಈ ರೋಗಿಯನ್ನು ಮೇಲ್ವಿಚಾರಣೆ ಮಾಡಬೇಕು. "
            else     -> "ಒಳ್ಳೆಯದು! ರೋಗಿ ಸಾಮಾನ್ಯ ಸ್ಥಿತಿಯಲ್ಲಿದ್ದಾರೆ. "
        }
        return base
    }

    private fun buildEnglishRiskSpeech(level: String, flags: List<String>): String {
        val base = when (level) {
            "RED"    -> "Warning! This patient is at high risk. "
            "YELLOW" -> "Attention. This patient needs monitoring. "
            else     -> "Good. The patient appears stable. "
        }
        val detail = flags.firstOrNull() ?: ""
        return base + detail
    }

    fun stop() { tts?.stop() }

    fun release() {
        tts?.shutdown()
        tts = null
        ready = false
    }
}
