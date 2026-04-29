package com.ashasaathi.service.ai

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

data class ExtractedVisitData(
    val patientName: String? = null,
    val bpSystolic: Int? = null,
    val bpDiastolic: Int? = null,
    val weightKg: Double? = null,
    val hemoglobinGdL: Double? = null,
    val pregnancyMonths: Int? = null,
    val hasFever: Boolean? = null,
    val coughDays: Int? = null,
    val fastingGlucose: Double? = null,
    val vaccineName: String? = null,
    val temperature: Double? = null,
    val spo2: Int? = null,
    val clinicalNotes: String? = null
)

@Singleton
class LlamaService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var modelLoaded = false

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    companion object {
        private const val MODEL_FILE = "ggml-tinyllama-1.1b-chat-q4_0.gguf"

        init { runCatching { System.loadLibrary("ashasaathi_jni") } }
    }

    private external fun nativeLoadModel(modelPath: String): Boolean
    private external fun nativeGenerate(prompt: String, maxTokens: Int): String
    private external fun nativeFreeModel()

    suspend fun loadModel() = withContext(Dispatchers.IO) {
        if (modelLoaded) return@withContext
        _isLoading.value = true
        runCatching {
            val dest = File(context.filesDir, MODEL_FILE)
            if (!dest.exists()) {
                context.assets.open(MODEL_FILE).use { inp ->
                    FileOutputStream(dest).use { out -> inp.copyTo(out) }
                }
            }
            modelLoaded = nativeLoadModel(dest.absolutePath)
        }
        _isLoading.value = false
    }

    suspend fun extractVisitData(transcript: String): ExtractedVisitData = withContext(Dispatchers.IO) {
        if (!modelLoaded) {
            return@withContext extractWithRules(transcript)
        }
        val prompt = buildExtractionPrompt(transcript)
        val json = nativeGenerate(prompt, 256)
        parseJson(json) ?: extractWithRules(transcript)
    }

    private fun buildExtractionPrompt(transcript: String): String = """<|system|>
You are a medical data extractor for Indian ASHA health workers. Extract structured data from Hindi/Kannada/English speech transcript. Return ONLY valid JSON, nothing else.

Number word mappings (Hindi): ek=1, do=2, teen=3, char=4, paanch=5, chhe=6, saat=7, aath=8, nau=9, das=10, bees=20, tees=30, chalis=40, pachaas=50, saath=60 (note: "saath" when used in BP context means 70), sattar=70, assi=80, nabbe=90, sau=100, ek sau=100, sau paanch=105, sau saath=160, sau sattar=170, dedh sau=150, do sau=200.

Extract these fields if mentioned (null if not mentioned):
- patientName: string
- bpSystolic: integer (mmHg)
- bpDiastolic: integer (mmHg)
- weightKg: float
- hemoglobinGdL: float
- pregnancyMonths: integer (1-9)
- hasFever: boolean
- coughDays: integer
- fastingGlucose: float (mg/dL)
- vaccineName: string
- temperature: float (Celsius)
- spo2: integer (%)
- clinicalNotes: string (any additional observations)
<|user|>
Transcript: "${transcript}"
<|assistant|>
{""".trimIndent()

    private fun parseJson(raw: String): ExtractedVisitData? = runCatching {
        val jsonStr = "{${raw.substringBefore('}')}}".let {
            if (raw.startsWith("{")) raw.substringBefore('}') + "}" else it
        }
        val j = JSONObject(jsonStr)
        ExtractedVisitData(
            patientName      = j.optString("patientName").takeIf { it.isNotBlank() },
            bpSystolic       = j.optInt("bpSystolic").takeIf { it > 0 },
            bpDiastolic      = j.optInt("bpDiastolic").takeIf { it > 0 },
            weightKg         = j.optDouble("weightKg").takeIf { it > 0 },
            hemoglobinGdL    = j.optDouble("hemoglobinGdL").takeIf { it > 0 },
            pregnancyMonths  = j.optInt("pregnancyMonths").takeIf { it > 0 },
            hasFever         = if (j.has("hasFever")) j.getBoolean("hasFever") else null,
            coughDays        = j.optInt("coughDays").takeIf { it > 0 },
            fastingGlucose   = j.optDouble("fastingGlucose").takeIf { it > 0 },
            vaccineName      = j.optString("vaccineName").takeIf { it.isNotBlank() },
            temperature      = j.optDouble("temperature").takeIf { it > 0 },
            spo2             = j.optInt("spo2").takeIf { it > 0 },
            clinicalNotes    = j.optString("clinicalNotes").takeIf { it.isNotBlank() }
        )
    }.getOrNull()

    // ── Keyword rule fallback (works without LLM) ─────────────────────────────

    fun extractWithRules(transcript: String): ExtractedVisitData {
        val t = transcript.lowercase()
        return ExtractedVisitData(
            bpSystolic    = extractBP(t)?.first,
            bpDiastolic   = extractBP(t)?.second,
            weightKg      = extractWeight(t),
            hemoglobinGdL = extractHb(t),
            pregnancyMonths = extractPregnancyMonth(t),
            hasFever      = t.contains("fever") || t.contains("bukhar") || t.contains("بخار") ||
                            t.contains("bukhaar") || t.contains("jwar") || t.contains("jwara") ||
                            t.contains("ಜ್ವರ"),
            coughDays     = extractCoughDays(t),
            fastingGlucose= extractGlucose(t),
            temperature   = extractTemperature(t),
            spo2          = extractSpo2(t),
            clinicalNotes = transcript.take(300)
        )
    }

    private fun extractBP(t: String): Pair<Int, Int>? {
        // "bp 120 over 80", "120/80", "120 by 80", "ek sau bees saath"
        val slashRegex = Regex("""(?:bp\s*)?(\d{2,3})\s*[/x]\s*(\d{2,3})""")
        slashRegex.find(t)?.let { m ->
            val sys = m.groupValues[1].toIntOrNull()
            val dia = m.groupValues[2].toIntOrNull()
            if (sys != null && dia != null && sys in 60..250 && dia in 40..150)
                return Pair(sys, dia)
        }
        // Hindi word numbers for common BP values
        val wordBP = mapOf(
            "sau bees" to 120, "sau tees" to 130, "sau chalis" to 140,
            "sau pachaas" to 150, "sau saath" to 160, "sau sattar" to 170,
            "dedh sau" to 150, "nabbe" to 90, "assi" to 80, "sattar" to 70
        )
        val sys = wordBP.entries.firstOrNull { t.contains(it.key) }?.value
        val dia = when {
            t.contains("nabbe") -> 90
            t.contains("assi") -> 80
            t.contains("sattar") -> 70
            else -> null
        }
        if (sys != null && dia != null) return Pair(sys, dia)
        return null
    }

    private fun extractWeight(t: String): Double? {
        val r = Regex("""(\d{2,3}(?:\.\d)?)\s*(?:kg|kilo|किलो|kilo gram)""")
        return r.find(t)?.groupValues?.get(1)?.toDoubleOrNull()
    }

    private fun extractHb(t: String): Double? {
        val r = Regex("""(?:hb|haemoglobin|hemoglobin|खून|hba)\s*(?:is\s*)?(\d{1,2}(?:\.\d)?)""")
        return r.find(t)?.groupValues?.get(1)?.toDoubleOrNull()
    }

    private fun extractPregnancyMonth(t: String): Int? {
        val r = Regex("""(\d{1,2})\s*(?:month|mahine|माह|mahina|मास|ماه)""")
        return r.find(t)?.groupValues?.get(1)?.toIntOrNull()?.takeIf { it in 1..9 }
    }

    private fun extractCoughDays(t: String): Int? {
        val r = Regex("""(\d{1,2})\s*(?:days?|din|दिन|ದಿನ)\s*(?:se\s*)?(?:khansi|cough|khaans|खांसी|ಕೆಮ್ಮು)""")
        return r.find(t)?.groupValues?.get(1)?.toIntOrNull()
            ?: run {
                val r2 = Regex("""(?:khansi|cough|khaans|खांसी)\s*(\d{1,2})\s*(?:days?|din|दिन)""")
                r2.find(t)?.groupValues?.get(1)?.toIntOrNull()
            }
    }

    private fun extractGlucose(t: String): Double? {
        val r = Regex("""(?:sugar|fasting|glucose|शुगर)\s*(?:is\s*)?(\d{2,3}(?:\.\d)?)""")
        return r.find(t)?.groupValues?.get(1)?.toDoubleOrNull()
    }

    private fun extractTemperature(t: String): Double? {
        val r = Regex("""(?:temp|temperature|bukhar)\s*(?:is\s*)?(\d{2,3}(?:\.\d)?)""")
        return r.find(t)?.groupValues?.get(1)?.toDoubleOrNull()?.let { v ->
            if (v > 45.0) null else v // sanity check
        }
    }

    private fun extractSpo2(t: String): Int? {
        val r = Regex("""(?:spo2|oxygen|saturation|ऑक्सीजन)\s*(?:is\s*)?(\d{2,3})""")
        return r.find(t)?.groupValues?.get(1)?.toIntOrNull()?.takeIf { it in 70..100 }
    }

    fun release() { if (modelLoaded) { nativeFreeModel(); modelLoaded = false } }
}
