package com.ashasaathi.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ashasaathi.data.repository.AuthRepository
import com.ashasaathi.data.repository.PatientRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

data class TBPatientDisplay(
    val patientId: String,
    val name: String,
    val nikshayId: String,
    val dotsRegimen: String?,
    val dotsDue: Boolean,
    val adherencePercent: Int,
    val dbtThisMonth: Boolean,
    val dotsCalendar: Map<String, String> = emptyMap()
)

@HiltViewModel
class TBDotsViewModel @Inject constructor(
    private val authRepo: AuthRepository,
    private val patientRepo: PatientRepository
) : ViewModel() {

    private val _tbPatients = MutableStateFlow<List<TBPatientDisplay>>(emptyList())
    val tbPatients: StateFlow<List<TBPatientDisplay>> = _tbPatients.asStateFlow()

    private val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    init {
        authRepo.currentUserId?.let { uid ->
            viewModelScope.launch {
                patientRepo.observeWorkerPatients(uid).collect { patients ->
                    _tbPatients.value = patients
                        .filter { it.hasTB }
                        .map { p ->
                            TBPatientDisplay(
                                patientId        = p.patientId,
                                name             = p.name,
                                nikshayId        = p.nikshayId ?: "N/A",
                                dotsRegimen      = p.dotsRegimen,
                                dotsDue          = isDotsDueToday(p.patientId),
                                adherencePercent = computeAdherence(p.patientId),
                                dbtThisMonth     = false
                            )
                        }
                }
            }
        }
    }

    private fun isDotsDueToday(patientId: String): Boolean {
        val today = fmt.format(Date())
        val patient = _tbPatients.value.firstOrNull { it.patientId == patientId }
        return patient?.dotsCalendar?.get(today) != "TAKEN"
    }

    private fun computeAdherence(patientId: String): Int {
        val patient = _tbPatients.value.firstOrNull { it.patientId == patientId } ?: return 0
        val cal = patient.dotsCalendar
        if (cal.isEmpty()) return 0
        val taken = cal.values.count { it == "TAKEN" }
        return (taken * 100 / cal.size)
    }

    fun getDotsCalendar(patientId: String): Map<String, String> =
        _tbPatients.value.firstOrNull { it.patientId == patientId }?.dotsCalendar ?: emptyMap()

    fun recordDots(patientId: String) {
        val today = fmt.format(Date())
        viewModelScope.launch {
            patientRepo.updatePatient(
                patientId,
                mapOf(
                    "lastVisitDate" to today,
                    "syncStatus"    to "PENDING"
                )
            )
            updateLocalCalendar(patientId, today, "TAKEN")
        }
    }

    fun markDotsDay(patientId: String, date: String, status: String) {
        viewModelScope.launch {
            updateLocalCalendar(patientId, date, status)
            // Persist to Firestore via patient document dots map
            val existing = _tbPatients.value.firstOrNull { it.patientId == patientId }?.dotsCalendar ?: emptyMap()
            val updated = existing + (date to status)
            patientRepo.updatePatient(patientId, mapOf("dotsCalendar" to updated))
        }
    }

    private fun updateLocalCalendar(patientId: String, date: String, status: String) {
        _tbPatients.value = _tbPatients.value.map { patient ->
            if (patient.patientId != patientId) return@map patient
            val newCal = patient.dotsCalendar.toMutableMap().also { it[date] = status }
            val taken = newCal.values.count { it == "TAKEN" }
            val adherence = if (newCal.isEmpty()) 0 else (taken * 100 / newCal.size)
            patient.copy(
                dotsCalendar     = newCal,
                dotsDue          = newCal[fmt.format(Date())] != "TAKEN",
                adherencePercent = adherence
            )
        }
    }
}
