package com.ashasaathi.service.ai

import android.content.Context
import com.ashasaathi.data.model.Patient
import com.ashasaathi.data.model.RiskFlag
import com.ashasaathi.data.model.RiskResult
import com.ashasaathi.data.model.Visit
import com.ashasaathi.data.model.VitalSigns
import dagger.hilt.android.qualifiers.ApplicationContext
import org.tensorflow.lite.Interpreter
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RiskEngine @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var tflite: Interpreter? = null

    init { loadModel() }

    private fun loadModel() = runCatching {
        val f = File(context.filesDir, "risk_model.tflite")
        if (!f.exists()) {
            context.assets.open("risk_model.tflite").use { i -> FileOutputStream(f).use { o -> i.copyTo(o) } }
        }
        tflite = Interpreter(f)
    }

    // ── Main entry ────────────────────────────────────────────────────────────

    fun evaluate(visit: Visit, patient: Patient): RiskResult {
        val flags = mutableListOf<RiskFlag>()

        applyPregnancyRules(visit.vitals, patient, flags)
        applyAnaemiaRules(visit.vitals, flags)
        applyTBRules(visit, flags)
        applyGDMRules(visit.vitals, flags)
        applyObstetricEmergencyRules(visit.clinicalNotes, flags)
        applyGeneralRules(visit, patient, flags)

        // TFLite on borderline cases
        var aiScore = 0f
        if (flags.isEmpty() || flags.all { it.severity == "YELLOW" }) {
            aiScore = runTFLite(visit, patient)
            if (aiScore >= 0.75f) {
                flags += RiskFlag("AI_HIGH_RISK", "RED",
                    "AI मॉडल: उच्च जोखिम (${(aiScore * 100).toInt()}%)",
                    "AI Model: High Risk (${(aiScore * 100).toInt()}%)")
            } else if (aiScore >= 0.50f) {
                flags += RiskFlag("AI_MODERATE_RISK", "YELLOW",
                    "AI मॉडल: मध्यम जोखिम",
                    "AI Model: Moderate Risk")
            }
        }

        val level = when {
            flags.any { it.severity == "RED" }    -> "RED"
            flags.any { it.severity == "YELLOW" } -> "YELLOW"
            else                                   -> "GREEN"
        }
        return RiskResult(level, flags, aiScore)
    }

    // ── Rule sets ─────────────────────────────────────────────────────────────

    private fun applyPregnancyRules(v: VitalSigns, patient: Patient, flags: MutableList<RiskFlag>) {
        if (!patient.isPregnant) return
        val sys = v.bpSystolic ?: return
        val dia = v.bpDiastolic ?: return

        when {
            sys >= 160 || dia >= 110 -> flags += RiskFlag(
                "SEVERE_HTN_PREGNANCY", "RED",
                "रक्तचाप बहुत अधिक (${sys}/${dia}) — प्रसवाक्षेप का गंभीर खतरा",
                "BP critically high (${sys}/${dia}) — Severe Pre-eclampsia"
            )
            sys >= 140 || dia >= 90  -> flags += RiskFlag(
                "HTN_PREGNANCY", "RED",
                "गर्भावस्था में उच्च रक्तचाप (${sys}/${dia}) — प्रसवाक्षेप खतरा",
                "High BP in pregnancy (${sys}/${dia}) — Eclampsia risk"
            )
            sys >= 130 || dia >= 80  -> flags += RiskFlag(
                "ELEVATED_BP_PREGNANCY", "YELLOW",
                "रक्तचाप बढ़ा हुआ (${sys}/${dia}) — निगरानी जरूरी",
                "Elevated BP in pregnancy (${sys}/${dia}) — Monitor closely"
            )
        }
    }

    private fun applyAnaemiaRules(v: VitalSigns, flags: MutableList<RiskFlag>) {
        val hb = v.hemoglobinGdL ?: return
        when {
            hb < 7.0  -> flags += RiskFlag(
                "SEVERE_ANAEMIA", "RED",
                "गंभीर रक्ताल्पता Hb ${hb} — तत्काल रेफरल जरूरी",
                "Severe Anaemia Hb ${hb} — Immediate referral needed"
            )
            hb < 9.0  -> flags += RiskFlag(
                "MODERATE_ANAEMIA", "YELLOW",
                "मध्यम रक्ताल्पता Hb ${hb} — IFA बढ़ाएं",
                "Moderate Anaemia Hb ${hb} — Increase IFA"
            )
            hb < 11.0 -> flags += RiskFlag(
                "MILD_ANAEMIA", "YELLOW",
                "हल्की रक्ताल्पता Hb ${hb}",
                "Mild Anaemia Hb ${hb}"
            )
        }
    }

    private fun applyTBRules(visit: Visit, flags: MutableList<RiskFlag>) {
        val cough = visit.coughDays ?: 0
        when {
            visit.hasFever && cough >= 14 -> flags += RiskFlag(
                "TB_SUSPECT", "RED",
                "TB संदिग्ध — ${cough} दिन खांसी + बुखार",
                "TB Suspect — ${cough} days cough + fever"
            )
            cough >= 14 -> flags += RiskFlag(
                "CHRONIC_COUGH", "YELLOW",
                "${cough} दिनों से खांसी — TB जांच करें",
                "Cough for ${cough} days — Screen for TB"
            )
            visit.hasFever && cough >= 7 -> flags += RiskFlag(
                "FEVER_WITH_COUGH", "YELLOW",
                "बुखार और ${cough} दिन खांसी",
                "Fever with ${cough} days cough"
            )
        }
    }

    private fun applyGDMRules(v: VitalSigns, flags: MutableList<RiskFlag>) {
        v.fastingGlucose?.let { fg ->
            when {
                fg >= 126 -> flags += RiskFlag(
                    "GDM_FASTING", "RED",
                    "फास्टिंग शुगर ${fg} mg/dL — GDM की पुष्टि",
                    "Fasting glucose ${fg} mg/dL — GDM confirmed"
                )
                fg >= 92  -> flags += RiskFlag(
                    "GDM_BORDERLINE", "YELLOW",
                    "फास्टिंग शुगर ${fg} mg/dL — GDM संभव",
                    "Fasting glucose ${fg} mg/dL — GDM borderline"
                )
            }
        }
        v.ogtt2hr?.let { og ->
            when {
                og >= 200 -> flags += RiskFlag(
                    "GDM_OGTT_HIGH", "RED",
                    "OGTT 2hr ${og} mg/dL — GDM की पुष्टि",
                    "OGTT 2hr ${og} mg/dL — GDM confirmed"
                )
                og >= 153 -> flags += RiskFlag(
                    "GDM_OGTT_BORDERLINE", "YELLOW",
                    "OGTT 2hr ${og} mg/dL — GDM संभव",
                    "OGTT 2hr ${og} mg/dL — GDM borderline"
                )
            }
        }
    }

    private val EMERGENCY_WORDS = listOf(
        // English
        "seizure","convulsion","unconscious","unresponsive","heavy bleeding",
        "placenta previa","cord prolapse","eclampsia","stroke","fits",
        "excessive bleeding","not breathing","fetal distress","abruption",
        // Hindi
        "दौरे","बेहोश","बहुत खून","झटके","नाल आगे","सांस नहीं","सांस बंद",
        "नाभि नाल बाहर","अचेत","दाैरे","रक्तस्राव","बेहोशी","नाड़ी नहीं",
        // Kannada
        "ಮೂರ್ಛೆ","ಬೇಟೆ","ರಕ್ತಸ್ರಾವ","ಉಸಿರಾಟ ಇಲ್ಲ","ಸೆಳೆತ"
    )

    private fun applyObstetricEmergencyRules(notes: String, flags: MutableList<RiskFlag>) {
        val lower = notes.lowercase()
        val matched = EMERGENCY_WORDS.firstOrNull { lower.contains(it.lowercase()) }
        if (matched != null) {
            flags += RiskFlag(
                "OBSTETRIC_EMERGENCY", "RED",
                "आपातकालीन संकेत: \"$matched\" — तत्काल 108 बुलाएं",
                "Emergency sign: \"$matched\" — Call 108 immediately"
            )
        }
    }

    private fun applyGeneralRules(visit: Visit, patient: Patient, flags: MutableList<RiskFlag>) {
        // High temperature
        visit.vitals.temperatureCelsius?.let { temp ->
            if (temp >= 39.0) {
                flags += RiskFlag(
                    "HIGH_FEVER", "YELLOW",
                    "तेज बुखार ${temp}°C",
                    "High Fever ${temp}°C"
                )
            }
        }

        // Low SpO2
        visit.vitals.spo2Percent?.let { spo2 ->
            when {
                spo2 < 90 -> flags += RiskFlag("LOW_SPO2", "RED",
                    "SpO2 ${spo2}% — ऑक्सीजन बहुत कम",
                    "SpO2 ${spo2}% — Critically low oxygen")
                spo2 < 94 -> flags += RiskFlag("LOW_SPO2_MILD", "YELLOW",
                    "SpO2 ${spo2}%",
                    "SpO2 ${spo2}% — Low oxygen")
            }
        }

        // Protein in urine for pregnant
        if (patient.isPregnant) {
            val urine = visit.vitals.urineProtein ?: ""
            if (urine.contains("+2") || urine.contains("+3")) {
                flags += RiskFlag("PROTEINURIA", "RED",
                    "मूत्र में प्रोटीन $urine — प्रीक्लैम्पसिया संकेत",
                    "Urine protein $urine — Pre-eclampsia sign")
            } else if (urine.contains("+1") || urine.contains("trace", ignoreCase = true)) {
                flags += RiskFlag("PROTEINURIA_MILD", "YELLOW",
                    "मूत्र में प्रोटीन $urine",
                    "Urine protein $urine — Monitor")
            }
        }

        // Not seen in 7 days (high-risk patient)
        if (patient.currentRiskLevel == "RED" && patient.lastVisitDate != null) {
            // computed outside during reminder checks
        }

        // IFA check
        val ifaTotal = patient.ifaCumulativeCount + (visit.vitals.ifaTabletsToday ?: 0)
        val trimester = patient.trimester ?: 0
        if (patient.isPregnant && trimester >= 2 && ifaTotal < 90) {
            flags += RiskFlag("IFA_LOW", "YELLOW",
                "IFA गोलियां कम (${ifaTotal}/180)",
                "IFA tablets low (${ifaTotal}/180)")
        }
    }

    // ── TFLite model ──────────────────────────────────────────────────────────

    private fun runTFLite(visit: Visit, patient: Patient): Float {
        val interp = tflite ?: return 0f
        return runCatching {
            val features = extractFeatures(visit, patient)
            val input = ByteBuffer.allocateDirect(features.size * 4).order(ByteOrder.nativeOrder())
            features.forEach { input.putFloat(it) }
            val output = Array(1) { FloatArray(3) } // [GREEN, YELLOW, RED]
            interp.run(input, output)
            output[0][2] // RED probability
        }.getOrElse { 0f }
    }

    private fun extractFeatures(visit: Visit, patient: Patient): FloatArray = floatArrayOf(
        if (patient.isPregnant) 1f else 0f,
        patient.gestationalAgeWeeks?.toFloat() ?: 0f,
        patient.trimester?.toFloat() ?: 0f,
        visit.vitals.bpSystolic?.toFloat() ?: 0f,
        visit.vitals.bpDiastolic?.toFloat() ?: 0f,
        visit.vitals.hemoglobinGdL?.toFloat() ?: 0f,
        visit.vitals.weightKg?.toFloat() ?: 0f,
        visit.vitals.temperatureCelsius?.toFloat() ?: 0f,
        visit.vitals.spo2Percent?.toFloat() ?: 0f,
        visit.vitals.fastingGlucose?.toFloat() ?: 0f,
        if (patient.isChildUnder5) 1f else 0f,
        patient.age?.toFloat() ?: 0f,
        if (patient.hasTB) 1f else 0f,
        if (patient.isElderly) 1f else 0f,
        patient.chronicConditions.size.toFloat(),
        if (visit.hasFever) 1f else 0f,
        visit.coughDays?.toFloat() ?: 0f,
        patient.ancCount.toFloat(),
        patient.riskFlags.size.toFloat(),
        patient.ifaCumulativeCount.toFloat()
    )
}
