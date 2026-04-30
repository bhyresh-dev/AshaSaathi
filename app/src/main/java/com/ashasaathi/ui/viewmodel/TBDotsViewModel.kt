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
                    val today = fmt.format(Date())
                    _tbPatients.value = patients
                        .filter { it.hasTB }
                        .map { p ->
                            val cal = p.dotsCalendar
                            val taken = cal.values.count { it == "TAKEN" }
                            val adherence = if (cal.isEmpty()) 0 else (taken * 100 / cal.size)
                            TBPatientDisplay(
                                patientId        = p.patientId,
                                name             = p.name,
                                nikshayId        = p.nikshayId ?: "N/A",
                                dotsRegimen      = p.dotsRegimen,
                                dotsDue          = cal[today] != "TAKEN",
                                adherencePercent = adherence,
                                dbtThisMonth     = false,
                                dotsCalendar     = cal
                            )
                        }
                }
            }
        }
    }

    fun getDotsCalendar(patientId: String): Map<String, String> =
        _tbPatients.value.firstOrNull { it.patientId == patientId }?.dotsCalendar ?: emptyMap()

    fun recordDots(patientId: String) {
        val today = fmt.format(Date())
        markDotsDay(patientId, today, "TAKEN")
    }

    fun markDotsDay(patientId: String, date: String, status: String) {
        viewModelScope.launch {
            val existing = _tbPatients.value.firstOrNull { it.patientId == patientId }?.dotsCalendar ?: emptyMap()
            val updated = existing + (date to status)
            // Write to Firestore — observeWorkerPatients listener fires and rebuilds display
            patientRepo.updatePatient(patientId, mapOf("dotsCalendar" to updated))
        }
    }
}
