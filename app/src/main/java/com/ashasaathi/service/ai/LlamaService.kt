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
            hasFever      = t.contains("fever") || t.contains("bukhar") || t.contains("bukhaar") ||
                            t.contains("बुखार") || t.contains("ज्वर") || t.contains("jwar") ||
                            t.contains("ಜ್ವರ"),
            coughDays     = extractCoughDays(t),
            fastingGlucose= extractGlucose(t),
            temperature   = extractTemperature(t),
            spo2          = extractSpo2(t),
            clinicalNotes = transcript.take(300)
        )
    }

    // ── Numbered-point parser (primary extraction path) ───────────────────────
    //
    // User speaks: "1. घर नंबर 5  2. मुखिया राम कुमार  3. गाँव पुरनपुर  4. सदस्य 6..."
    // We split on point boundaries and process each segment in isolation,
    // which prevents numbers from one field bleeding into another.

    /**
     * Split transcript on numbered-point markers.
     * Returns map of point index (1-based) → text of that point.
     * Works for "1.", "1)", "point 1", "बिंदु 1", "अंक 1", "ಅಂಕಿ 1".
     */
    private fun splitByPoints(transcript: String): Map<Int, String> {
        val result = mutableMapOf<Int, String>()
        // Match "1." / "1)" / "point 1" / "बिंदु 1" / "अंक 1" / "ಅಂಕಿ 1" as separators
        val boundary = Regex(
            """(?:^|\s)(?:point|बिंदु|अंक|ಅಂಕಿ)?\s*(\d{1,2})[.)]\s*""",
            setOf(RegexOption.IGNORE_CASE, RegexOption.MULTILINE)
        )
        val matches = boundary.findAll(transcript).toList()
        for (i in matches.indices) {
            val pointNum = matches[i].groupValues[1].toIntOrNull() ?: continue
            val start = matches[i].range.last + 1
            val end   = if (i + 1 < matches.size) matches[i + 1].range.first else transcript.length
            result[pointNum] = transcript.substring(start, end).trim()
        }
        return result
    }

    // ── Government form extractors ────────────────────────────────────────────

    fun extractHousehold(transcript: String): com.ashasaathi.data.model.ExtractedHousehold {
        val points = splitByPoints(transcript)
        val t = transcript.lowercase()

        // Numbered-point extraction (preferred — no cross-field collision)
        if (points.size >= 3) {
            return com.ashasaathi.data.model.ExtractedHousehold(
                houseNumber    = points[1]?.let { extractHouseNumber(it).ifBlank { extractFirstNumber(it) } } ?: extractHouseNumber(transcript),
                headOfFamily   = points[2]?.trim() ?: extractHeadName(transcript),
                village        = points[3]?.trim() ?: extractVillage(transcript),
                totalMembers   = points[4]?.let { extractFirstNumberInt(it) } ?: extractCount(t, listOf("log","sadasya","member","सदस्य","लोग","ಸದಸ್ಯ","people")),
                eligibleCouples= points[5]?.let { extractFirstNumberInt(it) } ?: extractCount(t, listOf("couple","dampati","दंपति","जोड़े")),
                pregnantWomen  = points[6]?.let { extractFirstNumberInt(it) } ?: extractCount(t, listOf("garbhavati","pregnant","गर्भवती","ಗರ್ಭಿಣಿ")),
                childrenUnder5 = points[7]?.let { extractFirstNumberInt(it) } ?: extractCount(t, listOf("bachche","child","बच्चे","ಮಕ್ಕಳು","under 5")),
                elderly        = points[8]?.let { extractFirstNumberInt(it) } ?: extractCount(t, listOf("buzurg","elderly","बुजुर्ग","ಹಿರಿಯ"))
            )
        }

        // Natural-language fallback
        return com.ashasaathi.data.model.ExtractedHousehold(
            houseNumber    = extractHouseNumber(transcript),
            headOfFamily   = extractHeadName(transcript),
            village        = extractVillage(transcript),
            totalMembers   = extractCount(t, listOf("log","sadasya","member","sadasy","hain","hai","सदस्य","लोग","व्यक्ति","जन","ಸದಸ್ಯ","people","person","family member","parivaar","परिवार")),
            pregnantWomen  = extractCount(t, listOf("garbhavati","pregnant","pregnancy","गर्भवती","गर्भवती महिला","प्रेग्नेंट","ಗರ್ಭಿಣಿ")),
            childrenUnder5 = extractCount(t, listOf("bachche","bachcha","bacha","bache","child","children","बच्चे","बच्चा","बच्ची","छोटे बच्चे","शिशु","ಮಕ್ಕಳು","पाँच साल","five year","under 5")),
            elderly        = extractCount(t, listOf("buzurg","bujurg","elderly","old person","senior","बुजुर्ग","वृद्ध","बूढ़े","ಹಿರಿಯ")),
            eligibleCouples= extractCount(t, listOf("couple","dampati","yugal","दंपति","जोड़े","विवाहित जोड़े"))
        )
    }

    fun extractPatient(transcript: String): com.ashasaathi.data.model.ExtractedPatient {
        val points = splitByPoints(transcript)
        val t = transcript.lowercase()

        if (points.size >= 3) {
            return com.ashasaathi.data.model.ExtractedPatient(
                name        = points[1]?.trim() ?: extractPersonNames(transcript).getOrElse(0) { "" },
                husbandName = points[2]?.trim() ?: extractHusbandName(transcript),
                age         = points[3]?.let { extractFirstNumberInt(it) } ?: extractAge(t),
                phone       = points[4]?.let { extractPhone(it.lowercase()) } ?: extractPhone(t),
                village     = points[5]?.trim() ?: extractVillage(transcript),
                rchId       = points[6]?.let { extractRchId(it.lowercase()) } ?: extractRchId(t),
                isPregnant  = t.contains("pregnant") || t.contains("garbhavati") || t.contains("गर्भवती") || t.contains("ಗರ್ಭಿಣಿ"),
                lmpDate     = extractDate(transcript)
            )
        }

        val names = extractPersonNames(transcript)
        return com.ashasaathi.data.model.ExtractedPatient(
            name         = names.getOrElse(0) { "" },
            husbandName  = extractHusbandName(transcript),
            age          = extractAge(t),
            phone        = extractPhone(t),
            village      = extractVillage(transcript),
            rchId        = extractRchId(t),
            isPregnant   = t.contains("pregnant") || t.contains("garbhavati") || t.contains("गर्भवती") || t.contains("ಗರ್ಭಿಣಿ"),
            lmpDate      = extractDate(transcript)
        )
    }

    fun extractANC(transcript: String): com.ashasaathi.data.model.ExtractedANC {
        val points = splitByPoints(transcript)
        val t = transcript.lowercase()

        if (points.size >= 3) {
            val bpText  = points[3]?.lowercase() ?: ""
            val bp      = if (bpText.isNotBlank()) extractBP(bpText) else extractBP(t)
            val feverPt = points[9]?.lowercase() ?: ""
            val hasFev  = if (feverPt.isNotBlank())
                feverPt.contains("haan") || feverPt.contains("yes") || feverPt.contains("hai") ||
                feverPt.contains("हाँ") || feverPt.contains("हां") || feverPt.contains("है") ||
                feverPt.contains("ಹೌದು")
            else
                t.contains("fever") || t.contains("bukhar") || t.contains("बुखार") || t.contains("ಜ್ವರ")

            return com.ashasaathi.data.model.ExtractedANC(
                patientName    = points[1]?.trim() ?: extractPersonNames(transcript).getOrElse(0) { "" },
                lmpDate        = points[2]?.let { extractDate(it) } ?: extractDate(transcript),
                bpSystolic     = bp?.first,
                bpDiastolic    = bp?.second,
                weightKg       = points[4]?.let { extractWeight(it.lowercase()) } ?: extractWeight(t),
                hemoglobinGdL  = points[5]?.let { extractHb(it.lowercase()) ?: extractFirstDouble(it) } ?: extractHb(t),
                ifaTabletsGiven= points[6]?.let { extractFirstNumberInt(it) } ?: extractIFA(t),
                ttDose         = points[7]?.let { extractTTDose(it.lowercase()) } ?: extractTTDose(t),
                urineProtein   = points[8]?.let { extractUrineProtein(it.lowercase()) } ?: extractUrineProtein(t),
                fastingGlucose = extractGlucose(t),
                hasFever       = hasFev,
                complaints     = points[10]?.trim() ?: ""
            )
        }

        val bp = extractBP(t)
        return com.ashasaathi.data.model.ExtractedANC(
            patientName    = extractPersonNames(transcript).getOrElse(0) { "" },
            lmpDate        = extractDate(transcript),
            bpSystolic     = bp?.first,
            bpDiastolic    = bp?.second,
            weightKg       = extractWeight(t),
            hemoglobinGdL  = extractHb(t),
            ifaTabletsGiven= extractIFA(t),
            ttDose         = extractTTDose(t),
            urineProtein   = extractUrineProtein(t),
            fastingGlucose = extractGlucose(t),
            hasFever       = t.contains("fever") || t.contains("bukhar") || t.contains("बुखार") || t.contains("ಜ್ವರ"),
            complaints     = transcript.take(200)
        )
    }

    fun extractVaccine(transcript: String): com.ashasaathi.data.model.ExtractedVaccine {
        val points = splitByPoints(transcript)

        if (points.size >= 2) {
            return com.ashasaathi.data.model.ExtractedVaccine(
                childName   = points[1]?.trim() ?: extractPersonNames(transcript).getOrElse(0) { "" },
                dob         = points[2]?.let { extractDate(it) } ?: extractDate(transcript),
                vaccineName = points[3]?.let { extractVaccineName(it) } ?: extractVaccineName(transcript),
                motherName  = points[4]?.trim() ?: "",
                village     = points[5]?.trim() ?: extractVillage(transcript)
            )
        }

        val names = extractPersonNames(transcript)
        return com.ashasaathi.data.model.ExtractedVaccine(
            childName   = names.getOrElse(0) { "" },
            dob         = extractDate(transcript),
            vaccineName = extractVaccineName(transcript)
        )
    }

    fun extractDOTS(transcript: String): com.ashasaathi.data.model.ExtractedDOTS {
        val points = splitByPoints(transcript)
        val t = transcript.lowercase()

        if (points.size >= 2) {
            val dotsPt = points[3]?.lowercase() ?: ""
            val taken  = if (dotsPt.isNotBlank())
                dotsPt.contains("liya") || dotsPt.contains("taken") || dotsPt.contains("yes") ||
                dotsPt.contains("haan") || dotsPt.contains("हाँ") || dotsPt.contains("हां") ||
                dotsPt.contains("ಹೌದು") || dotsPt.contains("le liya") || dotsPt.contains("लिया")
            else
                t.contains("liya") || t.contains("taken") || t.contains("le liya") ||
                t.contains("लिया") || t.contains("ತೆಗೆದುಕೊಂಡ") ||
                t.contains("yes") || t.contains("haan") || t.contains("हाँ")

            return com.ashasaathi.data.model.ExtractedDOTS(
                patientName = points[1]?.trim() ?: extractPersonNames(transcript).getOrElse(0) { "" },
                nikshayId   = points[2]?.let { extractNikshayId(it.lowercase()) } ?: extractNikshayId(t),
                dotsTaken   = taken,
                sideEffects = points[4]?.trim() ?: ""
            )
        }

        val taken = t.contains("liya") || t.contains("taken") || t.contains("le liya") ||
                    t.contains("लिया") || t.contains("ತೆಗೆದುಕೊಂಡ") ||
                    t.contains("yes") || t.contains("haan") || t.contains("हाँ")
        return com.ashasaathi.data.model.ExtractedDOTS(
            patientName = extractPersonNames(transcript).getOrElse(0) { "" },
            nikshayId   = extractNikshayId(t),
            dotsTaken   = taken,
            sideEffects = if (t.contains("side effect") || t.contains("takleef") || t.contains("तकलीफ")) transcript.take(100) else ""
        )
    }

    // ── Helper extractors ─────────────────────────────────────────────────────────

    /** First standalone number (digits or word) in a short text segment */
    private fun extractFirstNumber(text: String): String =
        Regex("""\b(\d+)\b""").find(text)?.groupValues?.get(1) ?: ""

    private fun extractFirstNumberInt(text: String): Int? =
        Regex("""\b(\d+)\b""").find(text)?.groupValues?.get(1)?.toIntOrNull()
            ?: wordToInt(text.trim())

    private fun extractFirstDouble(text: String): Double? =
        Regex("""(\d+(?:[.,]\d+)?)""").find(text)?.groupValues?.get(1)?.replace(',','.')?.toDoubleOrNull()

    /** Hindi/Kannada/romanized number words → Int */
    private fun wordToInt(word: String): Int? = when (word.trim().lowercase()) {
        "एक" -> 1; "दो" -> 2; "तीन" -> 3; "चार" -> 4
        "पाँच", "पांच" -> 5; "छह", "छः", "छ" -> 6; "सात" -> 7; "आठ" -> 8
        "नौ" -> 9; "दस" -> 10; "ग्यारह" -> 11; "बारह" -> 12
        "तेरह" -> 13; "चौदह" -> 14; "पंद्रह" -> 15; "सोलह" -> 16
        "सत्रह" -> 17; "अठारह" -> 18; "उन्नीस" -> 19; "बीस" -> 20
        "पच्चीस" -> 25; "तीस" -> 30; "पैंतीस" -> 35; "चालीस" -> 40
        "पचास" -> 50; "साठ" -> 60; "सत्तर" -> 70; "अस्सी" -> 80
        "नब्बे" -> 90; "सौ" -> 100
        "ek" -> 1; "do" -> 2; "teen" -> 3; "char", "chaar" -> 4
        "paanch", "panch" -> 5; "chhe", "chah", "chai" -> 6
        "saat" -> 7; "aath" -> 8; "nau", "naw", "nav" -> 9
        "das" -> 10; "gyarah" -> 11; "barah" -> 12; "tera", "terah" -> 13
        "chaudah" -> 14; "pandrah" -> 15; "solah" -> 16
        "satrah" -> 17; "atharah" -> 18; "unnis" -> 19; "bees" -> 20
        "pachees" -> 25; "tees" -> 30; "pachaas" -> 50; "saath", "saatth" -> 70
        "assi" -> 80; "nabbe" -> 90; "sau" -> 100
        "ಒಂದು" -> 1; "ಎರಡು" -> 2; "ಮೂರು" -> 3; "ನಾಲ್ಕು" -> 4
        "ಐದು" -> 5; "ಆರು" -> 6; "ಏಳು" -> 7; "ಎಂಟು" -> 8
        "ಒಂಬತ್ತು" -> 9; "ಹತ್ತು" -> 10
        else -> null
    }

    private fun parseNumberToken(tok: String): Int? =
        tok.toIntOrNull() ?: wordToInt(tok)

    private val NAME_CHARS = "[\\p{L}\\p{M}]"

    private fun extractBP(t: String): Pair<Int, Int>? {
        val patterns = listOf(
            Regex("""(?:bp|blood pressure|ब्लड प्रेशर|रक्तचाप|b\.?p\.?)\s*[:#]?\s*(\d{2,3})\s*[/xX\-]\s*(\d{2,3})""", RegexOption.IGNORE_CASE),
            Regex("""(\d{2,3})\s*(?:by|over|upon|पर|/)\s*(\d{2,3})""", RegexOption.IGNORE_CASE),
            Regex("""(?:bp|blood pressure|ब्लड प्रेशर|रक्तचाप)\s*[:#]?\s*(\d{2,3})\s+(\d{2,3})""", RegexOption.IGNORE_CASE),
            Regex("""(\d{2,3})\s*[/]\s*(\d{2,3})""")
        )
        for (p in patterns) {
            p.find(t)?.let { m ->
                val sys = m.groupValues[1].toIntOrNull()
                val dia = m.groupValues[2].toIntOrNull()
                if (sys != null && dia != null && sys in 60..260 && dia in 30..160)
                    return Pair(sys, dia)
            }
        }
        val wordBP = mapOf(
            "sau bees" to 120, "सौ बीस" to 120,
            "sau tees" to 130, "सौ तीस" to 130,
            "sau chalis" to 140, "सौ चालीस" to 140,
            "sau pachaas" to 150, "dedh sau" to 150, "डेढ़ सौ" to 150,
            "sau saath" to 160, "सौ साठ" to 160,
            "sau sattar" to 170, "सौ सत्तर" to 170
        )
        val diaWords = mapOf(
            "nabbe" to 90, "नब्बे" to 90,
            "assi" to 80, "अस्सी" to 80,
            "sattar" to 70, "सत्तर" to 70,
            "saath" to 60, "साठ" to 60
        )
        val sys = wordBP.entries.firstOrNull { t.contains(it.key) }?.value
        val dia = diaWords.entries.firstOrNull { t.contains(it.key) }?.value
        if (sys != null && dia != null) return Pair(sys, dia)
        return null
    }

    private fun extractWeight(t: String): Double? {
        val r = Regex("""(\d{2,3}(?:[.,]\d)?)\s*(?:kg|kilo|किलो|किलोग्राम|kilo\s*gram)""", RegexOption.IGNORE_CASE)
        return r.find(t)?.groupValues?.get(1)?.replace(',', '.')?.toDoubleOrNull()
            ?: extractFirstDouble(t)?.takeIf { it in 20.0..200.0 }
    }

    private fun extractHb(t: String): Double? {
        val r = Regex("""(?:hb|haemoglobin|hemoglobin|हीमोग्लोबिन|खून|hba|हिमोग्लोबिन)\s*(?:is|level|है)?\s*[:#]?\s*(\d{1,2}(?:[.,]\d)?)""", RegexOption.IGNORE_CASE)
        return r.find(t)?.groupValues?.get(1)?.replace(',', '.')?.toDoubleOrNull()
    }

    private fun extractPregnancyMonth(t: String): Int? {
        val r = Regex("""(\d{1,2})\s*(?:month|mahine|माह|mahina|मास)""")
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
        val r = Regex("""(?:sugar|fasting|glucose|शुगर|ग्लूकोज|ब्लड शुगर)\s*(?:is|है|level)?\s*[:#]?\s*(\d{2,3}(?:[.,]\d)?)""", RegexOption.IGNORE_CASE)
        return r.find(t)?.groupValues?.get(1)?.replace(',', '.')?.toDoubleOrNull()
    }

    private fun extractTemperature(t: String): Double? {
        val r = Regex("""(?:temp|temperature|bukhar|बुखार|तापमान)\s*(?:is|है)?\s*[:#]?\s*(\d{2,3}(?:[.,]\d)?)""", RegexOption.IGNORE_CASE)
        return r.find(t)?.groupValues?.get(1)?.replace(',', '.')?.toDoubleOrNull()?.takeIf { it in 35.0..45.0 }
    }

    private fun extractSpo2(t: String): Int? {
        val r = Regex("""(?:spo2|spo 2|oxygen|saturation|ऑक्सीजन|ऑक्सी)\s*(?:is|है|level)?\s*[:#]?\s*(\d{2,3})""", RegexOption.IGNORE_CASE)
        return r.find(t)?.groupValues?.get(1)?.toIntOrNull()?.takeIf { it in 70..100 }
    }

    private fun extractHouseNumber(text: String): String {
        val r = Regex(
            """(?:ghar|house|makan|घर|ಮನೆ)\s*(?:number|no\.?|num|नंबर|ನಂಬರ್|क्रमांक)?\s*[:#]?\s*(\w+)""",
            RegexOption.IGNORE_CASE
        )
        r.find(text)?.let { m ->
            val tok = m.groupValues[1].trim()
            parseNumberToken(tok)?.toString()?.let { return it }
            if (tok.isNotBlank()) return tok
        }
        Regex("""#\s*([A-Za-z0-9\-/]+)""").find(text)?.let { return it.groupValues[1].trim() }
        return extractFirstNumber(text)
    }

    private fun extractHeadName(text: String): String {
        val r = Regex(
            """(?:mukhiya|head of family|ghar ka mukhiya|मुखिया|ಯಜಮಾನ|owner|malik)\s+(?:ka\s+naam|का\s+नाम|ka\s+name|name)?\s*($NAME_CHARS+(?:\s+$NAME_CHARS+)?)""",
            RegexOption.IGNORE_CASE
        )
        r.find(text)?.let { return it.groupValues[1].trim() }
        val r2 = Regex(
            """(?:naam|name|नाम)\s+(?:hai\s+|है\s+)?($NAME_CHARS+(?:\s+$NAME_CHARS+)?)\s*(?:hai|है)?""",
            RegexOption.IGNORE_CASE
        )
        r2.find(text)?.let { return it.groupValues[1].trim() }
        return ""
    }

    private fun extractVillage(text: String): String {
        val r = Regex(
            """(?:gaon|village|gram|गाँव|गांव|ग्राम|ಗ್ರಾಮ)\s+(?:mein\s+|में\s+)?($NAME_CHARS+(?:\s+$NAME_CHARS+)?)""",
            RegexOption.IGNORE_CASE
        )
        return r.find(text)?.groupValues?.get(1)?.trim()
            ?.substringBefore(" में")?.substringBefore(" mein") ?: ""
    }

    private fun extractCount(t: String, keywords: List<String>): Int? {
        for (kw in keywords) {
            val kwEsc = Regex.escape(kw)
            val r1 = Regex("""(\S+)(?:\s+\S+){0,3}\s+$kwEsc""")
            r1.findAll(t).forEach { m ->
                parseNumberToken(m.groupValues[1])?.let { return it }
            }
            val r2 = Regex("""$kwEsc(?:\s+\S+){0,2}\s+(\S+)""")
            r2.findAll(t).forEach { m ->
                parseNumberToken(m.groupValues[1])?.let { return it }
            }
        }
        return null
    }

    private fun extractPersonNames(text: String): List<String> {
        val names = mutableListOf<String>()
        val r1 = Regex(
            """(?:naam|name|नाम|patient|mahila|महिला|ಮಹಿಳೆ|baccha|bache|child|मरीज़|marij)\s+(?:hai\s+|है\s+|ka\s+naam\s+|का\s+नाम\s+|ka\s+name\s+)?($NAME_CHARS+(?:\s+$NAME_CHARS+)?)""",
            RegexOption.IGNORE_CASE
        )
        r1.findAll(text).forEach { names.add(it.groupValues[1].trim()) }
        if (names.isNotEmpty()) return names.distinct()

        val r2 = Regex("""^($NAME_CHARS+(?:\s+$NAME_CHARS+){0,2})""", RegexOption.MULTILINE)
        r2.findAll(text).forEach { m ->
            val candidate = m.groupValues[1].trim()
            val exclude = setOf("घर", "मुखिया", "कुल", "ब्लड", "वजन", "आज", "गाँव", "गांव")
            if (candidate !in exclude && candidate.length > 2) names.add(candidate)
        }
        if (names.isNotEmpty()) return names.distinct()

        Regex("""([A-Z][a-z]+(?:\s+[A-Z][a-z]+)+)""").findAll(text).forEach { names.add(it.value) }
        return names.distinct()
    }

    private fun extractHusbandName(text: String): String {
        val r = Regex(
            """(?:pati|husband|pita|पति|पिता|ಪತಿ)\s+(?:ka\s+naam|का\s+नाम|ka\s+name)?\s*($NAME_CHARS+(?:\s+$NAME_CHARS+)?)""",
            RegexOption.IGNORE_CASE
        )
        return r.find(text)?.groupValues?.get(1)?.trim() ?: ""
    }

    private fun extractAge(t: String): Int? {
        val r = Regex("""(\S+)\s*(?:saal|sal|year|वर्ष|साल|ವರ್ಷ|age|उम्र)""", RegexOption.IGNORE_CASE)
        r.find(t)?.groupValues?.get(1)?.let { parseNumberToken(it) }?.takeIf { it in 5..110 }?.let { return it }
        val r2 = Regex("""(?:age|umar|उम्र|age is)\s+(\S+)""", RegexOption.IGNORE_CASE)
        r2.find(t)?.groupValues?.get(1)?.let { parseNumberToken(it) }?.takeIf { it in 5..110 }?.let { return it }
        return null
    }

    private fun extractPhone(t: String): String {
        val r = Regex("""\b([6-9]\d{9})\b""")
        return r.find(t)?.groupValues?.get(1) ?: ""
    }

    private fun extractRchId(t: String): String {
        val r = Regex("""(?:rch|mcts)\s*(?:id|number|no)?\s*[:#]?\s*([A-Za-z0-9\-/]+)""", RegexOption.IGNORE_CASE)
        return r.find(t)?.groupValues?.get(1)?.trim() ?: ""
    }

    private fun extractNikshayId(t: String): String {
        val r = Regex("""(?:nikshay|निक्षय)\s*(?:id|number|no|आईडी)?\s*[:#]?\s*([A-Za-z0-9\-/]+)""", RegexOption.IGNORE_CASE)
        return r.find(t)?.groupValues?.get(1)?.trim()
            // fallback: any alphanumeric token 6+ chars that looks like an ID
            ?: Regex("""\b([A-Z]{2,3}\d{4,})\b""").find(t)?.groupValues?.get(1)?.trim() ?: ""
    }

    private fun extractDate(text: String): String {
        val r = Regex("""(\d{1,2})[/\-.](\d{1,2})[/\-.](\d{2,4})""")
        r.find(text)?.let { m ->
            val d  = m.groupValues[1].padStart(2, '0')
            val mo = m.groupValues[2].padStart(2, '0')
            val y  = m.groupValues[3].let { if (it.length == 2) "20$it" else it }
            return "$y-$mo-$d"
        }
        return ""
    }

    private fun extractIFA(t: String): Int? {
        val r = Regex("""(\S+)\s*(?:ifa|iron tablet|आयरन|आईएफए|IFA)\s*(?:tablet|goli|गोली)?""", RegexOption.IGNORE_CASE)
        r.find(t)?.groupValues?.get(1)?.let { parseNumberToken(it) }?.let { return it }
        val r2 = Regex("""(?:ifa|आईएफए|iron|गोली|tablet)\s*(?:goli\s*)?(\S+)""", RegexOption.IGNORE_CASE)
        return r2.find(t)?.groupValues?.get(1)?.let { parseNumberToken(it) }
    }

    private fun extractTTDose(t: String): String = when {
        t.contains("tt booster") || t.contains("tt बूस्टर") || t.contains("booster") -> "BOOSTER"
        t.contains("tt2") || t.contains("tt 2") || t.contains("doosra tt") || t.contains("दूसरा tt") -> "TT2"
        t.contains("tt1") || t.contains("tt 1") || t.contains("pehla tt") || t.contains("पहला tt") -> "TT1"
        t.contains(" tt") || t.contains("tt ") || t.contains("टीटी") -> "TT1"
        else -> ""
    }

    private fun extractUrineProtein(t: String): String = when {
        t.contains("+3") || t.contains("plus 3") || t.contains("teen plus") || t.contains("तीन प्लस") -> "+3"
        t.contains("+2") || t.contains("plus 2") || t.contains("do plus") || t.contains("दो प्लस")   -> "+2"
        t.contains("+1") || t.contains("plus 1") || t.contains("ek plus") || t.contains("एक प्लस")   -> "+1"
        t.contains("trace") || t.contains("थोड़ा") || t.contains("कम")                               -> "trace"
        t.contains("nil") || t.contains("negative") || t.contains("नहीं") && t.contains("पेशाब")     -> "NIL"
        t.contains("urine") || t.contains("peshab") || t.contains("पेशाब")                           -> "NIL"
        else -> ""
    }

    private fun extractVaccineName(text: String): String {
        val vaccines = listOf(
            "BCG", "OPV", "IPV", "Pentavalent", "Penta", "PCV", "Rotavirus",
            "MR", "JE", "DPT", "DT", "Hepatitis B", "Hep B", "Vitamin A",
            "टीका", "tika", "टीकाकरण"
        )
        val t = text.lowercase()
        return vaccines.firstOrNull { t.contains(it.lowercase()) } ?: ""
    }

    fun release() { if (modelLoaded) { nativeFreeModel(); modelLoaded = false } }
}
