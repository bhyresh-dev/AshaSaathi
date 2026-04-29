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

    init {
        authRepo.currentUserId?.let { uid ->
            viewModelScope.launch {
                patientRepo.observeWorkerPatients(uid).collect { patients ->
                    val children = patients.filter { it.isChildUnder5 }
                    val schedules = children.map { patient ->
                        val records = vaccinationRepo.getVaccineStatus(patient.patientId)
                        val schedule = vaccinationRepo.buildSchedule(patient.patientId, patient.dob, records)
                        Pair(patient, schedule)
                    }
                    _patientsWithSchedules.value = schedules
                    _loading.value = false
                }
            }
        }
    }

    fun markAdministered(patientId: String, entry: VaccineScheduleEntry) {
        viewModelScope.launch {
            val record = entry.record?.copy(
                administeredDate = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date()),
                status = "ADMINISTERED"
            ) ?: VaccinationRecord(
                patientId = patientId,
                workerId  = authRepo.currentUserId ?: "",
                vaccineId = entry.vaccine.id,
                vaccineName = entry.vaccine.nameEn,
                dose = entry.vaccine.dose,
                scheduledDate = entry.scheduledDate,
                administeredDate = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date()),
                status = "ADMINISTERED"
            )
            vaccinationRepo.recordVaccine(record)
        }
    }

    fun computeFIC(schedule: List<VaccineScheduleEntry>) = vaccinationRepo.computeFIC(schedule)
    fun computeCIC(schedule: List<VaccineScheduleEntry>) = vaccinationRepo.computeCIC(schedule)
}
