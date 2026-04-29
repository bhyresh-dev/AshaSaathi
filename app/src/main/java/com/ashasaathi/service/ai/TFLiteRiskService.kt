package com.ashasaathi.service.ai

import android.content.Context
import com.ashasaathi.data.model.Patient
import dagger.hilt.android.qualifiers.ApplicationContext
import org.tensorflow.lite.Interpreter
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TFLiteRiskService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var interpreter: Interpreter? = null

    init { loadModel() }

    private fun loadModel() {
        runCatching {
            val modelFile = File(context.filesDir, "risk_model.tflite")
            if (!modelFile.exists()) copyModelFromAssets(modelFile)
            interpreter = Interpreter(modelFile)
        }
    }

    private fun copyModelFromAssets(dest: File) {
        runCatching {
            context.assets.open("risk_model.tflite").use { input ->
                FileOutputStream(dest).use { output -> input.copyTo(output) }
            }
        }
    }

    fun classifyRisk(patient: Patient): Triple<String, Float, List<String>> {
        val interp = interpreter ?: return Triple("GREEN", 0f, emptyList())

        val features = extractFeatures(patient)
        val input = ByteBuffer.allocateDirect(features.size * 4).order(ByteOrder.nativeOrder())
        features.forEach { input.putFloat(it) }

        val output = Array(1) { FloatArray(3) } // GREEN, YELLOW, RED
        runCatching { interp.run(input, output) }.onFailure {
            return Triple("GREEN", 0f, emptyList())
        }

        val scores = output[0]
        val maxIdx = scores.indices.maxByOrNull { scores[it] } ?: 0
        val riskLevel = when (maxIdx) { 2 -> "RED"; 1 -> "YELLOW"; else -> "GREEN" }
        val flags = buildRiskFlags(patient, scores)

        return Triple(riskLevel, scores[maxIdx], flags)
    }

    private fun extractFeatures(patient: Patient): FloatArray = floatArrayOf(
        if (patient.isPregnant) 1f else 0f,
        patient.gestationalAgeWeeks?.toFloat() ?: 0f,
        patient.trimester?.toFloat() ?: 0f,
        if (patient.isChildUnder5) 1f else 0f,
        patient.age?.toFloat() ?: 0f,
        if (patient.hasTB) 1f else 0f,
        if (patient.isElderly) 1f else 0f,
        patient.chronicConditions.size.toFloat(),
        if (patient.rchMctsId != null) 1f else 0f,
        patient.riskFlags.size.toFloat()
    )

    private fun buildRiskFlags(patient: Patient, scores: FloatArray): List<String> {
        val flags = mutableListOf<String>()
        if (patient.isPregnant && (patient.trimester ?: 0) >= 3) flags.add("HIGH_RISK_PREGNANCY")
        if (patient.hasTB) flags.add("TB_ACTIVE")
        if (patient.chronicConditions.contains("HYPERTENSION")) flags.add("HYPERTENSION")
        if (patient.chronicConditions.contains("DIABETES")) flags.add("DIABETES")
        if (scores[2] > 0.6f) flags.add("AI_HIGH_RISK")
        return flags
    }
}
