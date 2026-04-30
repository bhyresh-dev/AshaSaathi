package com.ashasaathi.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ashasaathi.data.model.Patient
import com.ashasaathi.data.model.VaccinationRecord
import com.ashasaathi.data.repository.AuthRepository
import com.ashasaathi.data.repository.PatientRepository
import com.ashasaathi.data.repository.VaccinationRepository
import com.ashasaathi.data.repository.VaccineScheduleEntry
import com.ashasaathi.data.repository.VaccineStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VaccinationViewModel @Inject constructor(
    private val authRepo: AuthRepository,
    private val patientRepo: PatientRepository,
    private val vaccinationRepo: VaccinationRepository
) : ViewModel() {

    private val _patientsWithSchedules = MutableStateFlow<List<Pair<Patient, List<VaccineScheduleEntry>>>>(emptyList())
    val patientsWithSchedules: StateFlow<List<Pair<Patient, List<VaccineScheduleEntry>>>> = _patientsWithSchedules.asStateFlow()

    private val _loading = MutableStateFlow(true)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    val ficCount: StateFlow<Int> = _patientsWithSchedules.map { list ->
        list.count { (_, schedule) -> vaccinationRepo.computeFIC(schedule) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), 0)

    private val records = mutableMapOf<String, List<VaccinationRecord>>()
    private val activeListeners = mutableSetOf<String>()

    init {
        val uid = authRepo.currentUserId
        if (uid == null) {
            _loading.value = false
        } else {
            viewModelScope.launch {
                patientRepo.observeWorkerPatients(uid).collect { patients ->
                    val children = patients.filter { it.isChildUnder5 }
                    // Start live listener for each child not yet observed
                    children.forEach { child ->
                        if (activeListeners.add(child.patientId)) {
                            launch {
                                vaccinationRepo.observePatientVaccinations(child.patientId)
                                    .collect { recs ->
                                        records[child.patientId] = recs
                                        rebuildAll(patients.filter { it.isChildUnder5 })
                                    }
                            }
                        }
                    }
                    // If no children, clear and stop loading
                    if (children.isEmpty()) {
                        _patientsWithSchedules.value = emptyList()
                        _loading.value = false
                    }
                }
            }
        }
    }

    private fun rebuildAll(children: List<Patient>) {
        _patientsWithSchedules.value = children.map { patient ->
            val recs = records[patient.patientId] ?: emptyList()
            Pair(patient, vaccinationRepo.buildSchedule(patient.patientId, patient.dob, recs))
        }
        _loading.value = false
    }

    fun markAdministered(patientId: String, entry: VaccineScheduleEntry) {
        viewModelScope.launch {
            val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
            val record = entry.record?.copy(administeredDate = today, status = "ADMINISTERED")
                ?: VaccinationRecord(
                    patientId        = patientId,
                    workerId         = authRepo.currentUserId ?: "",
                    vaccineId        = entry.vaccine.id,
                    vaccineName      = entry.vaccine.nameEn,
                    dose             = entry.vaccine.dose,
                    scheduledDate    = entry.scheduledDate,
                    administeredDate = today,
                    status           = "ADMINISTERED"
                )
            vaccinationRepo.recordVaccine(record)
            // observePatientVaccinations fires automatically → rebuildAll
        }
    }

    fun computeFIC(schedule: List<VaccineScheduleEntry>) = vaccinationRepo.computeFIC(schedule)
    fun computeCIC(schedule: List<VaccineScheduleEntry>) = vaccinationRepo.computeCIC(schedule)
}
