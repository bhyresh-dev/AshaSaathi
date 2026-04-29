package com.ashasaathi.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ashasaathi.data.model.GeoPoint
import com.ashasaathi.data.model.Visit
import com.ashasaathi.data.model.VitalSigns
import com.ashasaathi.data.repository.AuthRepository
import com.ashasaathi.data.repository.VisitRepository
import com.google.firebase.Timestamp
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

data class VisitFormState(
    val visitType: String = "GENERAL",
    val bpSystolic: String = "",
    val bpDiastolic: String = "",
    val weight: String = "",
    val hemoglobin: String = "",
    val spo2: String = "",
    val temperature: String = "",
    val hasFever: Boolean = false,
    val clinicalNotes: String = "",
    val referralNeeded: Boolean = false,
    val referralNote: String = "",
    val voiceTranscript: String = "",
    val saving: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class VisitFormViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val authRepo: AuthRepository,
    private val visitRepo: VisitRepository
) : ViewModel() {

    private val patientId: String = checkNotNull(savedStateHandle["patientId"])

    private val _state = MutableStateFlow(VisitFormState())
    val state: StateFlow<VisitFormState> = _state

    fun onVisitTypeChange(v: String) { _state.value = _state.value.copy(visitType = v) }
    fun onBpSystolicChange(v: String) { _state.value = _state.value.copy(bpSystolic = v) }
    fun onBpDiastolicChange(v: String) { _state.value = _state.value.copy(bpDiastolic = v) }
    fun onWeightChange(v: String) { _state.value = _state.value.copy(weight = v) }
    fun onHemoglobinChange(v: String) { _state.value = _state.value.copy(hemoglobin = v) }
    fun onSpo2Change(v: String) { _state.value = _state.value.copy(spo2 = v) }
    fun onTemperatureChange(v: String) { _state.value = _state.value.copy(temperature = v) }
    fun onFeverChange(v: Boolean) { _state.value = _state.value.copy(hasFever = v) }
    fun onNotesChange(v: String) { _state.value = _state.value.copy(clinicalNotes = v) }
    fun onReferralChange(v: Boolean) { _state.value = _state.value.copy(referralNeeded = v) }
    fun onReferralNoteChange(v: String) { _state.value = _state.value.copy(referralNote = v) }
    fun onVoiceTranscript(text: String) {
        _state.value = _state.value.copy(
            voiceTranscript = text,
            clinicalNotes = _state.value.clinicalNotes + "\n[Voice] $text"
        )
    }

    fun saveVisit(onSuccess: () -> Unit) {
        val s = _state.value
        val uid = authRepo.currentUserId ?: return
        _state.value = s.copy(saving = true, error = null)

        viewModelScope.launch {
            runCatching {
                val visit = Visit(
                    patientId = patientId,
                    workerId = uid,
                    visitType = s.visitType,
                    visitDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
                    vitals = VitalSigns(
                        bpSystolic = s.bpSystolic.toIntOrNull(),
                        bpDiastolic = s.bpDiastolic.toIntOrNull(),
                        weightKg = s.weight.toDoubleOrNull(),
                        hemoglobinGdL = s.hemoglobin.toDoubleOrNull(),
                        spo2Percent = s.spo2.toIntOrNull(),
                        temperatureCelsius = s.temperature.toDoubleOrNull()
                    ),
                    hasFever = s.hasFever,
                    clinicalNotes = s.clinicalNotes,
                    voiceTranscript = s.voiceTranscript.takeIf { it.isNotBlank() },
                    referralNeeded = s.referralNeeded,
                    referralNote = s.referralNote.takeIf { it.isNotBlank() },
                    location = GeoPoint(),
                    createdBy = uid
                )
                visitRepo.createVisit(visit)
            }.onSuccess {
                _state.value = _state.value.copy(saving = false)
                onSuccess()
            }.onFailure {
                _state.value = _state.value.copy(saving = false, error = it.message)
            }
        }
    }
}
