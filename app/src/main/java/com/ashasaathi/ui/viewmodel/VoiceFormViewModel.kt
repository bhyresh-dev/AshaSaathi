package com.ashasaathi.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ashasaathi.data.model.*
import com.ashasaathi.data.repository.AuthRepository
import com.ashasaathi.data.repository.HouseholdRepository
import com.ashasaathi.data.repository.PatientRepository
import com.ashasaathi.data.repository.UserPreferencesRepository
import com.ashasaathi.data.repository.VisitRepository
import com.ashasaathi.service.ai.LlamaService
import com.ashasaathi.service.ai.WhisperService
import com.google.firebase.Timestamp
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

sealed class VoiceFormUiState {
    object SelectType     : VoiceFormUiState()
    data class Recording(val formType: VoiceFormType, val transcript: String = "") : VoiceFormUiState()
    data class Processing(val formType: VoiceFormType) : VoiceFormUiState()
    data class Review(
        val formType: VoiceFormType,
        val transcript: String,
        val household: ExtractedHousehold? = null,
        val patient:   ExtractedPatient?   = null,
        val anc:       ExtractedANC?       = null,
        val vaccine:   ExtractedVaccine?   = null,
        val dots:      ExtractedDOTS?      = null,
        val validationErrors: List<String> = emptyList()
    ) : VoiceFormUiState()
    data class Saving(val formType: VoiceFormType) : VoiceFormUiState()
    data class Done(val formType: VoiceFormType, val message: String) : VoiceFormUiState()
    data class Error(val message: String) : VoiceFormUiState()
    /** Whisper model not loaded — direct user to model setup */
    data class ModelNotReady(val formType: VoiceFormType) : VoiceFormUiState()
}

@HiltViewModel
class VoiceFormViewModel @Inject constructor(
    private val authRepo:      AuthRepository,
    private val householdRepo: HouseholdRepository,
    private val patientRepo:   PatientRepository,
    private val visitRepo:     VisitRepository,
    private val whisper:       WhisperService,
    private val llama:         LlamaService,
    private val prefs:         UserPreferencesRepository
) : ViewModel() {

    val language: StateFlow<String> = prefs.language.stateIn(
        scope        = viewModelScope,
        started      = SharingStarted.Eagerly,
        initialValue = "hi"
    )

    private val _uiState = MutableStateFlow<VoiceFormUiState>(VoiceFormUiState.SelectType)
    val uiState: StateFlow<VoiceFormUiState> = _uiState.asStateFlow()

    val isRecording: StateFlow<Boolean>  = whisper.isRecording
    val isProcessing: StateFlow<Boolean> = whisper.isProcessing

    private val dateFmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val today   get() = dateFmt.format(Date())

    init {
        // Mirror live transcript into Recording state
        viewModelScope.launch {
            whisper.transcript.collect { partial ->
                val cur = _uiState.value
                if (cur is VoiceFormUiState.Recording && partial.isNotBlank()) {
                    _uiState.value = cur.copy(transcript = partial)
                }
            }
        }

        // Surface STT errors to user
        viewModelScope.launch {
            whisper.sttError.collect { err ->
                if (err == null) return@collect
                val cur = _uiState.value
                if (cur is VoiceFormUiState.Recording) {
                    _uiState.value = VoiceFormUiState.Error(err)
                }
            }
        }

        // Android STT auto-ends on silence — detect the isRecording true→false transition
        // and auto-trigger extraction so the user doesn't have to tap stop manually
        viewModelScope.launch {
            var wasRecording = false
            whisper.isRecording.collect { nowRecording ->
                if (wasRecording && !nowRecording) {
                    val cur = _uiState.value
                    if (cur is VoiceFormUiState.Recording && cur.transcript.isNotBlank()) {
                        stopAndExtract(cur.formType)
                    }
                }
                wasRecording = nowRecording
            }
        }

        // Device has no STT engine at all
        viewModelScope.launch {
            whisper.modelNotReady.collect { notReady ->
                if (!notReady) return@collect
                val cur = _uiState.value
                if (cur is VoiceFormUiState.Recording) {
                    _uiState.value = VoiceFormUiState.ModelNotReady(cur.formType)
                }
            }
        }
    }

    // ── Recording lifecycle ───────────────────────────────────────────────────

    fun selectFormType(type: VoiceFormType) {
        _uiState.value = VoiceFormUiState.Recording(type)
        // Load model in background — if it succeeds, next recording uses Whisper JNI
        viewModelScope.launch { runCatching { whisper.loadModel() } }
    }

    fun startRecording(type: VoiceFormType) {
        whisper.clearTranscript()
        _uiState.value = VoiceFormUiState.Recording(type)
        // Call startRecording directly — no coroutine, no loadModel() blocking.
        // Recording must start immediately on mic tap for responsive UX.
        whisper.startRecording(language.value)
    }

    fun stopAndExtract(type: VoiceFormType) {
        val cur = _uiState.value
        if (cur !is VoiceFormUiState.Recording) return
        _uiState.value = VoiceFormUiState.Processing(type)
        viewModelScope.launch {
            val raw        = runCatching { whisper.stopRecordingAndTranscribe() }.getOrElse { "" }
            val transcript = raw.ifBlank { whisper.transcript.value }

            if (transcript.isBlank()) {
                _uiState.value = VoiceFormUiState.Error("कुछ सुनाई नहीं दिया। फिर से बोलें।\nNothing heard. Please speak again.")
                return@launch
            }

            val review = when (type) {
                VoiceFormType.HOUSEHOLD  -> VoiceFormUiState.Review(type, transcript, household = llama.extractHousehold(transcript))
                VoiceFormType.PATIENT    -> VoiceFormUiState.Review(type, transcript, patient   = llama.extractPatient(transcript))
                VoiceFormType.ANC_VISIT  -> VoiceFormUiState.Review(type, transcript, anc       = llama.extractANC(transcript))
                VoiceFormType.VACCINE    -> VoiceFormUiState.Review(type, transcript, vaccine   = llama.extractVaccine(transcript))
                VoiceFormType.TB_DOTS    -> VoiceFormUiState.Review(type, transcript, dots      = llama.extractDOTS(transcript))
            }
            _uiState.value = review
        }
    }

    // ── In-review field edits ─────────────────────────────────────────────────

    fun updateHousehold(h: ExtractedHousehold) {
        val cur = _uiState.value as? VoiceFormUiState.Review ?: return
        _uiState.value = cur.copy(household = h, validationErrors = emptyList())
    }

    fun updatePatient(p: ExtractedPatient) {
        val cur = _uiState.value as? VoiceFormUiState.Review ?: return
        _uiState.value = cur.copy(patient = p, validationErrors = emptyList())
    }

    fun updateANC(a: ExtractedANC) {
        val cur = _uiState.value as? VoiceFormUiState.Review ?: return
        _uiState.value = cur.copy(anc = a, validationErrors = emptyList())
    }

    fun updateVaccine(v: ExtractedVaccine) {
        val cur = _uiState.value as? VoiceFormUiState.Review ?: return
        _uiState.value = cur.copy(vaccine = v, validationErrors = emptyList())
    }

    fun updateDOTS(d: ExtractedDOTS) {
        val cur = _uiState.value as? VoiceFormUiState.Review ?: return
        _uiState.value = cur.copy(dots = d, validationErrors = emptyList())
    }

    // ── Mandatory field validation ────────────────────────────────────────────

    private fun validateHousehold(h: ExtractedHousehold): List<String> {
        val errors = mutableListOf<String>()
        if (h.houseNumber.isBlank())  errors += "घर नंबर / House Number"
        if (h.headOfFamily.isBlank()) errors += "मुखिया का नाम / Head of Family"
        if (h.village.isBlank())      errors += "गाँव / Village"
        if (h.totalMembers == null)   errors += "कुल सदस्य / Total Members"
        if (h.eligibleCouples == null) errors += "योग्य दंपति / Eligible Couples"
        return errors
    }

    private fun validatePatient(p: ExtractedPatient): List<String> {
        val errors = mutableListOf<String>()
        if (p.name.isBlank())        errors += "महिला का नाम / Patient Name"
        if (p.husbandName.isBlank()) errors += "पति का नाम / Husband Name"
        if (p.age == null)           errors += "उम्र / Age"
        if (p.village.isBlank())     errors += "गाँव / Village"
        return errors
    }

    private fun validateANC(a: ExtractedANC): List<String> {
        val errors = mutableListOf<String>()
        if (a.patientName.isBlank()) errors += "महिला का नाम / Patient Name"
        if (a.lmpDate.isBlank())     errors += "अंतिम माहवारी / LMP Date"
        if (a.bpSystolic == null || a.bpDiastolic == null) errors += "रक्तचाप / Blood Pressure"
        if (a.weightKg == null)      errors += "वजन / Weight"
        if (a.hemoglobinGdL == null) errors += "Hb स्तर / Haemoglobin"
        return errors
    }

    private fun validateVaccine(v: ExtractedVaccine): List<String> {
        val errors = mutableListOf<String>()
        if (v.childName.isBlank())   errors += "बच्चे का नाम / Child Name"
        if (v.dob.isBlank())         errors += "जन्म तिथि / Date of Birth"
        if (v.vaccineName.isBlank()) errors += "टीके का नाम / Vaccine Name"
        return errors
    }

    private fun validateDOTS(d: ExtractedDOTS): List<String> {
        val errors = mutableListOf<String>()
        if (d.patientName.isBlank()) errors += "मरीज़ का नाम / Patient Name"
        if (d.nikshayId.isBlank())   errors += "निक्षय आईडी / Nikshay ID"
        return errors
    }

    // ── Submit ────────────────────────────────────────────────────────────────

    fun submitForm() {
        val review = _uiState.value as? VoiceFormUiState.Review ?: return
        val uid = authRepo.currentUserId ?: run {
            _uiState.value = VoiceFormUiState.Error("Login required / लॉगिन ज़रूरी है")
            return
        }

        // Validate mandatory fields before saving
        val errors = when (review.formType) {
            VoiceFormType.HOUSEHOLD  -> validateHousehold(review.household ?: ExtractedHousehold())
            VoiceFormType.PATIENT    -> validatePatient(review.patient     ?: ExtractedPatient())
            VoiceFormType.ANC_VISIT  -> validateANC(review.anc             ?: ExtractedANC())
            VoiceFormType.VACCINE    -> validateVaccine(review.vaccine     ?: ExtractedVaccine())
            VoiceFormType.TB_DOTS    -> validateDOTS(review.dots           ?: ExtractedDOTS())
        }
        if (errors.isNotEmpty()) {
            _uiState.value = review.copy(validationErrors = errors)
            return
        }

        _uiState.value = VoiceFormUiState.Saving(review.formType)

        viewModelScope.launch {
            runCatching {
                when (review.formType) {
                    VoiceFormType.HOUSEHOLD -> saveHousehold(uid, review.household!!)
                    VoiceFormType.PATIENT   -> savePatient(uid, review.patient!!)
                    VoiceFormType.ANC_VISIT -> saveANC(uid, review.anc!!, review.transcript)
                    VoiceFormType.VACCINE   -> saveVaccine(uid, review.vaccine!!)
                    VoiceFormType.TB_DOTS   -> saveDOTS(uid, review.dots!!)
                }
            }.onSuccess { msg ->
                _uiState.value = VoiceFormUiState.Done(review.formType, msg)
            }.onFailure { e ->
                _uiState.value = VoiceFormUiState.Error(e.message ?: "Save failed")
            }
        }
    }

    private suspend fun saveHousehold(uid: String, h: ExtractedHousehold): String {
        val household = Household(
            workerId            = uid,
            houseNumber         = h.houseNumber,
            headOfFamily        = h.headOfFamily,
            village             = h.village,
            totalMembers        = h.totalMembers ?: 0,
            eligibleCouples     = h.eligibleCouples ?: 0,
            pregnantWomenCount  = h.pregnantWomen ?: 0,
            childrenUnder5Count = h.childrenUnder5 ?: 0,
            elderlyCount        = h.elderly ?: 0,
            overallRiskLevel    = "GREEN",
            syncStatus          = "PENDING",
            createdBy           = uid
        )
        householdRepo.createHousehold(household)
        return "परिवार #${h.houseNumber} सेव हुआ"
    }

    private suspend fun savePatient(uid: String, p: ExtractedPatient): String {
        val patient = Patient(
            workerId    = uid,
            name        = p.name,
            dob         = "",
            age         = p.age,
            phone       = p.phone.takeIf { it.isNotBlank() },
            village     = p.village.takeIf { it.isNotBlank() },
            rchMctsId   = p.rchId.takeIf { it.isNotBlank() },
            isPregnant  = p.isPregnant,
            lmpDate     = p.lmpDate.takeIf { it.isNotBlank() },
            syncStatus  = "PENDING",
            createdBy   = uid
        )
        patientRepo.createPatient(patient)
        return "${p.name} पंजीकृत हुईं"
    }

    private suspend fun saveANC(uid: String, a: ExtractedANC, transcript: String): String {
        val patients = patientRepo.searchPatients(uid, a.patientName)
        val patient  = patients.firstOrNull()

        val vitals = VitalSigns(
            bpSystolic      = a.bpSystolic,
            bpDiastolic     = a.bpDiastolic,
            weightKg        = a.weightKg,
            hemoglobinGdL   = a.hemoglobinGdL,
            ifaTabletsToday = a.ifaTabletsGiven,
            ttDoseGiven     = a.ttDose.takeIf { it.isNotBlank() },
            fastingGlucose  = a.fastingGlucose,
            urineProtein    = a.urineProtein.takeIf { it.isNotBlank() }
        )
        val visit = Visit(
            patientId       = patient?.patientId ?: UUID.randomUUID().toString(),
            workerId        = uid,
            visitType       = "ANC",
            visitDate       = today,
            vitals          = vitals,
            hasFever        = a.hasFever,
            clinicalNotes   = a.complaints,
            voiceTranscript = transcript,
            syncStatus      = "PENDING",
            createdBy       = uid
        )
        visitRepo.createVisit(visit)
        return "ANC विजिट सेव हुई — ${a.patientName}"
    }

    private suspend fun saveVaccine(uid: String, v: ExtractedVaccine): String {
        val patients = patientRepo.searchPatients(uid, v.childName)
        val patient  = patients.firstOrNull()

        val rec = com.ashasaathi.data.model.VaccinationRecord(
            patientId        = patient?.patientId ?: "",
            workerId         = uid,
            vaccineId        = v.vaccineName.lowercase(),
            vaccineName      = v.vaccineName,
            dose             = "1",
            scheduledDate    = today,
            administeredDate = today,
            status           = "ADMINISTERED",
            syncStatus       = "PENDING",
            createdBy        = uid
        )
        return "टीका ${v.vaccineName} दर्ज हुआ — ${v.childName}"
    }

    private suspend fun saveDOTS(uid: String, d: ExtractedDOTS): String {
        val patients = patientRepo.searchPatients(uid, d.patientName)
        val patient  = patients.firstOrNull() ?: return "मरीज़ नहीं मिला: ${d.patientName}"

        if (d.dotsTaken) {
            patientRepo.updatePatient(patient.patientId, mapOf(
                "lastVisitDate" to today,
                "syncStatus"    to "PENDING"
            ))
        }
        return "DOTS ${if (d.dotsTaken) "✓ लिया" else "✗ नहीं लिया"} — ${d.patientName}"
    }

    fun reset() {
        whisper.clearTranscript()
        _uiState.value = VoiceFormUiState.SelectType
    }

    fun retryRecording() {
        val cur = _uiState.value
        val type = when (cur) {
            is VoiceFormUiState.Recording     -> cur.formType
            is VoiceFormUiState.Processing    -> cur.formType
            is VoiceFormUiState.Review        -> cur.formType
            is VoiceFormUiState.ModelNotReady -> cur.formType
            is VoiceFormUiState.Error -> { _uiState.value = VoiceFormUiState.SelectType; return }
            else -> { _uiState.value = VoiceFormUiState.SelectType; return }
        }
        _uiState.value = VoiceFormUiState.Recording(type)
        whisper.startRecording(language.value)
    }
}
