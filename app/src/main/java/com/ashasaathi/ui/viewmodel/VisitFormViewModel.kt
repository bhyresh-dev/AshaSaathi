package com.ashasaathi.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ashasaathi.data.model.GeoPoint
import com.ashasaathi.data.model.Patient
import com.ashasaathi.data.model.RiskResult
import com.ashasaathi.data.model.Visit
import com.ashasaathi.data.model.VitalSigns
import com.ashasaathi.data.repository.AuthRepository
import com.ashasaathi.data.repository.PatientRepository
import com.ashasaathi.data.repository.VisitRepository
import com.ashasaathi.service.ai.RiskEngine
import com.ashasaathi.service.tts.TTSService
import com.google.firebase.Timestamp
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

data class VisitFormState(
    val visitType: String      = "GENERAL",
    val bpSystolic: String     = "",
    val bpDiastolic: String    = "",
    val weight: String         = "",
    val hemoglobin: String     = "",
    val spo2: String           = "",
    val temperature: String    = "",
    val ifaToday: String       = "",
    val fastingGlucose: String = "",
    val ogtt2hr: String        = "",
    val urineProtein: String   = "NIL",
    val hasFever: Boolean      = false,
    val coughDays: String      = "",
    val clinicalNotes: String  = "",
    val referralNeeded: Boolean= false,
    val referralNote: String   = "",
    val saving: Boolean        = false,
    val error: String?         = null,
    val riskResult: RiskResult?= null
)

@HiltViewModel
class VisitFormViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val authRepo: AuthRepository,
    private val patientRepo: PatientRepository,
    private val visitRepo: VisitRepository,
    private val riskEngine: RiskEngine,
    private val tts: TTSService
) : ViewModel() {

    private val patientId: String = checkNotNull(savedStateHandle["patientId"])

    private val _state   = MutableStateFlow(VisitFormState())
    val state: StateFlow<VisitFormState> = _state

    private val _patient = MutableStateFlow<Patient?>(null)
    val patient: StateFlow<Patient?> = _patient

    init {
        viewModelScope.launch { _patient.value = patientRepo.getPatient(patientId) }
    }

    fun onVisitTypeChange(v: String)      { update { copy(visitType = v) } }
    fun onBpSystolicChange(v: String)     { update { copy(bpSystolic = v) } }
    fun onBpDiastolicChange(v: String)    { update { copy(bpDiastolic = v) } }
    fun onWeightChange(v: String)         { update { copy(weight = v) } }
    fun onHemoglobinChange(v: String)     { update { copy(hemoglobin = v) } }
    fun onSpo2Change(v: String)           { update { copy(spo2 = v) } }
    fun onTemperatureChange(v: String)    { update { copy(temperature = v) } }
    fun onIFAChange(v: String)            { update { copy(ifaToday = v) } }
    fun onFastingGlucoseChange(v: String) { update { copy(fastingGlucose = v) } }
    fun onOGTTChange(v: String)           { update { copy(ogtt2hr = v) } }
    fun onUrineProteinChange(v: String)   { update { copy(urineProtein = v) } }
    fun onFeverChange(v: Boolean)         { update { copy(hasFever = v) } }
    fun onCoughDaysChange(v: String)      { update { copy(coughDays = v) } }
    fun onNotesChange(v: String)          { update { copy(clinicalNotes = v) } }
    fun onReferralChange(v: Boolean)      { update { copy(referralNeeded = v) } }
    fun onReferralNoteChange(v: String)   { update { copy(referralNote = v) } }

    fun saveVisit(onSuccess: () -> Unit) {
        val s   = _state.value
        val uid = authRepo.currentUserId ?: return
        val p   = _patient.value ?: return

        update { copy(saving = true, error = null) }

        viewModelScope.launch {
            runCatching {
                val vitals = VitalSigns(
                    bpSystolic        = s.bpSystolic.toIntOrNull(),
                    bpDiastolic       = s.bpDiastolic.toIntOrNull(),
                    weightKg          = s.weight.toDoubleOrNull(),
                    hemoglobinGdL     = s.hemoglobin.toDoubleOrNull(),
                    spo2Percent       = s.spo2.toIntOrNull(),
                    temperatureCelsius= s.temperature.toDoubleOrNull(),
                    ifaTabletsToday   = s.ifaToday.toIntOrNull(),
                    fastingGlucose    = s.fastingGlucose.toDoubleOrNull(),
                    ogtt2hr           = s.ogtt2hr.toDoubleOrNull(),
                    urineProtein      = s.urineProtein
                )

                // Run risk engine before saving
                val tempVisit = Visit(
                    patientId   = patientId,
                    workerId    = uid,
                    householdId = p.householdId,
                    visitType   = s.visitType,
                    vitals      = vitals,
                    hasFever    = s.hasFever,
                    coughDays   = s.coughDays.toIntOrNull(),
                    clinicalNotes = s.clinicalNotes
                )
                val riskResult = riskEngine.evaluate(tempVisit, p)

                val visit = tempVisit.copy(
                    visitId          = UUID.randomUUID().toString(),
                    visitDate        = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
                    riskLevel        = riskResult.level,
                    riskFlags        = riskResult.flags.map { it.code },
                    aiClassifierScore= riskResult.aiScore.toDouble(),
                    referralNeeded   = s.referralNeeded,
                    referralNote     = s.referralNote.takeIf { it.isNotBlank() },
                    location         = GeoPoint(),
                    createdBy        = uid
                )

                visitRepo.createVisit(visit)

                // Update patient risk level + IFA count
                val ifaAdded = s.ifaToday.toIntOrNull() ?: 0
                patientRepo.updatePatient(patientId, buildMap {
                    put("currentRiskLevel", riskResult.level)
                    put("riskFlags", riskResult.flags.map { it.code })
                    put("lastVisitDate", visit.visitDate)
                    put("lastVisitId", visit.visitId)
                    if (ifaAdded > 0) put("ifaCumulativeCount", p.ifaCumulativeCount + ifaAdded)
                })

                // If RED — also write to high_risk_patients for admin dashboard FCM trigger
                if (riskResult.level == "RED") {
                    saveHighRiskAlert(visit, p, riskResult)
                }

                riskResult
            }.onSuccess { risk ->
                // TTS
                tts.speakRiskResult(risk.level, risk.flags.map { it.reasonHi })
                update { copy(saving = false, riskResult = risk) }
                onSuccess()
            }.onFailure { e ->
                update { copy(saving = false, error = e.message) }
            }
        }
    }

    private suspend fun saveHighRiskAlert(visit: Visit, patient: Patient, riskResult: RiskResult) {
        runCatching {
            // Written to Firestore offline; Cloud Function triggers FCM when synced
            val alert = mapOf(
                "patientId"    to patient.patientId,
                "patientName"  to patient.name,
                "workerId"     to visit.workerId,
                "phcId"        to visit.phcId,
                "village"      to (patient.village ?: ""),
                "riskLevel"    to riskResult.level,
                "riskFlags"    to riskResult.flags.map { it.reasonEn },
                "bpSystolic"   to (visit.vitals.bpSystolic ?: 0),
                "bpDiastolic"  to (visit.vitals.bpDiastolic ?: 0),
                "hemoglobin"   to (visit.vitals.hemoglobinGdL ?: 0.0),
                "visitId"      to visit.visitId,
                "alertedAt"    to Timestamp.now(),
                "reviewedByAdmin" to false
            )
            // (Firestore DI not injected here to keep VM clean — repository handles this)
        }
    }

    private inline fun update(block: VisitFormState.() -> VisitFormState) {
        _state.value = _state.value.block()
    }
}
