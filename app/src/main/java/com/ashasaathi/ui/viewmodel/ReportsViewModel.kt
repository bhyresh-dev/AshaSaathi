package com.ashasaathi.ui.viewmodel

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Environment
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ashasaathi.data.repository.AuthRepository
import com.ashasaathi.data.repository.HouseholdRepository
import com.ashasaathi.data.repository.PatientRepository
import com.ashasaathi.data.repository.VaccinationRepository
import com.ashasaathi.data.repository.VisitRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class ReportState(
    val periodLabel: String,
    val totalHouseholds: Int,
    val totalPatients: Int,
    val visitsThisMonth: Int,
    val pregnantWomen: Int,
    val ancRegistered1stTrimester: Int,
    val anc4Plus: Int,
    val ttVaccinated: Int,
    val ifa180Complete: Int,
    val highRiskCount: Int,
    val institutionalDeliveries: Int,
    val childrenUnder5: Int,
    val ficCount: Int,
    val ficPercent: Int,
    val cicCount: Int,
    val vaccinesDueToday: Int,
    val vaccinesMissed: Int,
    val tbPatients: Int,
    val dotsAdherenceGood: Int,
    val dotsAdherencePoor: Int,
    val dbtThisMonth: Int,
    val jsyBeneficiaries: Int,
    val pmmvyBeneficiaries: Int,
    val activitySummary: Map<String, Int>
)

@HiltViewModel
class ReportsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val authRepo: AuthRepository,
    private val patientRepo: PatientRepository,
    private val visitRepo: VisitRepository,
    private val householdRepo: HouseholdRepository,
    private val vaccinationRepo: VaccinationRepository
) : ViewModel() {

    private val _reportState = MutableStateFlow<ReportState?>(null)
    val reportState: StateFlow<ReportState?> = _reportState.asStateFlow()

    /** Non-null when a PDF has been generated — contains the absolute file path. */
    private val _pdfPath = MutableStateFlow<String?>(null)
    val pdfPath: StateFlow<String?> = _pdfPath.asStateFlow()

    private val _exportError = MutableStateFlow<String?>(null)
    val exportError: StateFlow<String?> = _exportError.asStateFlow()

    private val monthFmt = SimpleDateFormat("yyyy-MM", Locale.getDefault())
    private val labelFmt = SimpleDateFormat("MMMM yyyy", Locale("hi"))

    private var householdCount = 0

    init {
        authRepo.currentUserId?.let { uid ->
            viewModelScope.launch {
                householdRepo.observeWorkerHouseholds(uid).collect { hh ->
                    householdCount = hh.size
                }
            }
        }
        load()
    }

    private fun load() {
        val uid = authRepo.currentUserId ?: return
        viewModelScope.launch {
            val thisMonth = monthFmt.format(Date())
            val patients  = patientRepo.getActivePatients(uid)
            val visits    = visitRepo.getVisitsForWorker(uid)
            val visitsThisMonth = visits.filter { it.visitDate.startsWith(thisMonth) }
            val ficTotal  = patients.count { it.ficStatus }
            val under5    = patients.count { it.isChildUnder5 }

            _reportState.value = ReportState(
                periodLabel               = labelFmt.format(Date()),
                totalHouseholds           = householdCount,
                totalPatients             = patients.size,
                visitsThisMonth           = visitsThisMonth.size,
                pregnantWomen             = patients.count { it.isPregnant },
                ancRegistered1stTrimester = patients.count { it.isPregnant && (it.trimester ?: 4) == 1 },
                anc4Plus                  = patients.count { it.ancCount >= 4 },
                ttVaccinated              = patients.count { it.ttDoseStatus in listOf("TT2", "BOOSTER") },
                ifa180Complete            = patients.count { it.ifaCumulativeCount >= 180 },
                highRiskCount             = patients.count { it.currentRiskLevel == "RED" },
                institutionalDeliveries   = patients.count { it.deliveryPlace in listOf("Hospital", "PHC", "CHC", "FRU") },
                childrenUnder5            = under5,
                ficCount                  = ficTotal,
                ficPercent                = if (under5 > 0) ficTotal * 100 / under5 else 0,
                cicCount                  = patients.count { it.cicStatus },
                vaccinesDueToday          = 0,
                vaccinesMissed            = 0,
                tbPatients                = patients.count { it.hasTB },
                dotsAdherenceGood         = 0,
                dotsAdherencePoor         = 0,
                dbtThisMonth              = 0,
                jsyBeneficiaries          = patients.count { it.jsyInstallments.isNotEmpty() },
                pmmvyBeneficiaries        = patients.count { it.pmmvyInstallments.isNotEmpty() },
                activitySummary           = mapOf(
                    "H1 - गृह भ्रमण"  to visitsThisMonth.size,
                    "H2 - ANC विजिट"  to visitsThisMonth.count { it.visitType == "ANC" },
                    "H3 - PNC विजिट"  to visitsThisMonth.count { it.visitType == "PNC" },
                    "H4 - बच्चा विजिट" to visitsThisMonth.count { it.visitType == "CHILD" },
                    "H5 - TB DOTS"    to visitsThisMonth.count { it.visitType == "TB" }
                )
            )
        }
    }

    fun exportReport() {
        val report = _reportState.value ?: return
        viewModelScope.launch {
            runCatching {
                val path = withContext(Dispatchers.IO) { generatePdf(report) }
                _pdfPath.value = path
            }.onFailure { e ->
                _exportError.value = "PDF बना नहीं। ${e.message}"
            }
        }
    }

    fun clearPdfPath()    { _pdfPath.value    = null }
    fun clearExportError() { _exportError.value = null }

    // ── PDF generation ────────────────────────────────────────────────────────

    private fun generatePdf(r: ReportState): String {
        val document = PdfDocument()
        val pageW = 595; val pageH = 842   // A4 points
        var pageNum = 1

        var page     = document.startPage(PdfDocument.PageInfo.Builder(pageW, pageH, pageNum).create())
        var canvas   = page.canvas
        var y        = 0f

        fun newPage() {
            document.finishPage(page)
            pageNum++
            page   = document.startPage(PdfDocument.PageInfo.Builder(pageW, pageH, pageNum).create())
            canvas = page.canvas
            y      = 0f
        }

        fun ensureSpace(needed: Float) { if (y + needed > pageH - 40f) newPage() }

        val titlePaint = Paint().apply {
            textSize  = 20f
            typeface  = Typeface.DEFAULT_BOLD
            color     = Color.rgb(0xE6, 0x5C, 0x00)  // Saffron
        }
        val headPaint = Paint().apply {
            textSize  = 13f
            typeface  = Typeface.DEFAULT_BOLD
            color     = Color.rgb(0x33, 0x33, 0x33)
        }
        val bodyPaint = Paint().apply {
            textSize = 11f
            color    = Color.rgb(0x44, 0x44, 0x44)
        }
        val valuePaint = Paint().apply {
            textSize  = 11f
            typeface  = Typeface.DEFAULT_BOLD
            color     = Color.rgb(0x00, 0x6D, 0x77)
        }
        val linePaint = Paint().apply {
            color       = Color.rgb(0xEE, 0xEE, 0xEE)
            strokeWidth = 1f
        }

        // ── Title block ───────────────────────────────────────────────────────
        val bgPaint = Paint().apply { color = Color.rgb(0xE6, 0x5C, 0x00) }
        canvas.drawRect(0f, 0f, pageW.toFloat(), 72f, bgPaint)

        val wPaint = Paint().apply {
            textSize  = 18f
            typeface  = Typeface.DEFAULT_BOLD
            color     = Color.WHITE
        }
        canvas.drawText("HMIS मासिक रिपोर्ट", 24f, 32f, wPaint)
        val subPaint = Paint().apply { textSize = 11f; color = Color.rgb(0xFF, 0xE0, 0xB2) }
        canvas.drawText(r.periodLabel, 24f, 52f, subPaint)
        canvas.drawText("NHM — National Health Mission", 24f, 65f, subPaint)
        y = 88f

        // ── Helper: section heading ───────────────────────────────────────────
        fun sectionHead(title: String) {
            ensureSpace(28f)
            y += 10f
            canvas.drawText(title, 24f, y, headPaint)
            y += 4f
            canvas.drawLine(24f, y, (pageW - 24).toFloat(), y, linePaint)
            y += 10f
        }

        fun row(label: String, value: String, shade: Boolean) {
            ensureSpace(22f)
            if (shade) {
                val shadePaint = Paint().apply { color = Color.rgb(0xFA, 0xF7, 0xF2) }
                canvas.drawRect(22f, y - 12f, (pageW - 22).toFloat(), y + 6f, shadePaint)
            }
            canvas.drawText(label, 28f, y, bodyPaint)
            val valX = pageW - 28f - valuePaint.measureText(value)
            canvas.drawText(value, valX, y, valuePaint)
            y += 18f
        }

        // ── Summary ───────────────────────────────────────────────────────────
        sectionHead("कार्यक्षेत्र सारांश / Coverage Summary")
        listOf(
            "कुल परिवार"          to r.totalHouseholds.toString(),
            "कुल मरीज / रोगी"     to r.totalPatients.toString(),
            "इस माह विजिट"        to r.visitsThisMonth.toString(),
            "उच्च जोखिम (RED)"    to r.highRiskCount.toString()
        ).forEachIndexed { i, (l, v) -> row(l, v, i % 2 == 0) }

        sectionHead("ANC सेवाएँ / Antenatal Care")
        listOf(
            "गर्भवती महिलाएं"              to r.pregnantWomen.toString(),
            "ANC पंजीकृत (1st Trimester)"  to r.ancRegistered1stTrimester.toString(),
            "4+ ANC प्राप्त"               to r.anc4Plus.toString(),
            "TT2 / Booster"                to r.ttVaccinated.toString(),
            "IFA 180 पूर्ण"                to r.ifa180Complete.toString(),
            "संस्थागत प्रसव"               to r.institutionalDeliveries.toString()
        ).forEachIndexed { i, (l, v) -> row(l, v, i % 2 == 0) }

        sectionHead("UIP टीकाकरण / Immunisation")
        listOf(
            "5 वर्ष से कम बच्चे"    to r.childrenUnder5.toString(),
            "FIC (पूर्ण टीकाकरण)"  to r.ficCount.toString(),
            "FIC %"                 to "${r.ficPercent}%",
            "CIC (पूर्ण श्रेणी)"   to r.cicCount.toString()
        ).forEachIndexed { i, (l, v) -> row(l, v, i % 2 == 0) }

        sectionHead("TB DOTS / क्षय रोग")
        listOf(
            "TB मरीज पंजीकृत" to r.tbPatients.toString()
        ).forEachIndexed { i, (l, v) -> row(l, v, i % 2 == 0) }

        sectionHead("सरकारी योजनाएँ / Schemes")
        listOf(
            "JSY लाभार्थी"   to r.jsyBeneficiaries.toString(),
            "PMMVY लाभार्थी" to r.pmmvyBeneficiaries.toString()
        ).forEachIndexed { i, (l, v) -> row(l, v, i % 2 == 0) }

        sectionHead("NHM गतिविधियाँ / Activity Codes")
        r.activitySummary.entries.forEachIndexed { i, (code, cnt) ->
            row(code, cnt.toString(), i % 2 == 0)
        }

        // Footer
        ensureSpace(30f)
        y += 20f
        val footPaint = Paint().apply { textSize = 9f; color = Color.GRAY }
        canvas.drawText("Generated by AshaSaathi • ${SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault()).format(Date())}", 24f, y, footPaint)

        document.finishPage(page)

        val dir = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "AshaSaathi")
        dir.mkdirs()
        val file = File(dir, "HMIS_Report_${SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())}.pdf")
        FileOutputStream(file).use { document.writeTo(it) }
        document.close()
        return file.absolutePath
    }
}
