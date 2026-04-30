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

    // Tracks latest records per patientId so we can refresh without re-fetching all
    private val _vaccinationRecords = MutableStateFlow<Map<String, List<VaccinationRecord>>>(emptyMap())

    init {
        val uid = authRepo.currentUserId
        if (uid == null) {
            _loading.value = false
        } else {
            viewModelScope.launch {
                patientRepo.observeWorkerPatients(uid).collect { patients ->
                    val children = patients.filter { it.isChildUnder5 }
                    rebuildSchedules(children)
                    _loading.value = false

                    // Start live vaccination listeners for each child
                    children.forEach { child ->
                        if (!_vaccinationRecords.value.containsKey(child.patientId)) {
                            launch {
                                vaccinationRepo.observePatientVaccinations(child.patientId).collect { records ->
                                    _vaccinationRecords.value = _vaccinationRecords.value + (child.patientId to records)
                                    rebuildScheduleForPatient(child)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private suspend fun rebuildSchedules(children: List<Patient>) {
        val schedules = children.map { patient ->
            val records = _vaccinationRecords.value[patient.patientId]
                ?: vaccinationRepo.getVaccineStatus(patient.patientId).also { r ->
                    _vaccinationRecords.value = _vaccinationRecords.value + (patient.patientId to r)
                }
            Pair(patient, vaccinationRepo.buildSchedule(patient.patientId, patient.dob, records))
        }
        _patientsWithSchedules.value = schedules
    }

    private fun rebuildScheduleForPatient(patient: Patient) {
        val records = _vaccinationRecords.value[patient.patientId] ?: return
        val schedule = vaccinationRepo.buildSchedule(patient.patientId, patient.dob, records)
        _patientsWithSchedules.value = _patientsWithSchedules.value.map {
            if (it.first.patientId == patient.patientId) Pair(patient, schedule) else it
        }
    }

    fun markAdministered(patientId: String, entry: VaccineScheduleEntry) {
        viewModelScope.launch {
            val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
            val record = entry.record?.copy(
                administeredDate = today,
                status = "ADMINISTERED"
            ) ?: VaccinationRecord(
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
            // observePatientVaccinations listener fires automatically and calls rebuildScheduleForPatient
        }
    }

    fun computeFIC(schedule: List<VaccineScheduleEntry>) = vaccinationRepo.computeFIC(schedule)
    fun computeCIC(schedule: List<VaccineScheduleEntry>) = vaccinationRepo.computeCIC(schedule)
}
