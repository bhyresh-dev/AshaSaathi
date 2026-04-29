package com.ashasaathi.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ashasaathi.data.repository.AuthRepository
import com.ashasaathi.data.repository.PatientRepository
import com.ashasaathi.ui.screens.vaccination.VaccineItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VaccinationViewModel @Inject constructor(
    private val authRepo: AuthRepository,
    private val patientRepo: PatientRepository
) : ViewModel() {

    private val _items = MutableStateFlow<List<VaccineItem>>(emptyList())
    val patientsWithVaccines: StateFlow<List<VaccineItem>> = _items.asStateFlow()

    init {
        val uid = authRepo.currentUserId ?: return
        viewModelScope.launch {
            patientRepo.observeWorkerPatients(uid).collect { patients ->
                val items = patients
                    .filter { it.isChildUnder5 || it.isPregnant }
                    .flatMap { patient ->
                        (patient.vaccinationRecord ?: emptyList()).map { record ->
                            VaccineItem(
                                patientId = patient.patientId,
                                patientName = patient.name,
                                vaccineId = record["vaccineId"] ?: "",
                                vaccineName = record["vaccineId"] ?: "Vaccine",
                                scheduledDate = "-",
                                vaccinated = record["status"] == "ADMINISTERED"
                            )
                        }.ifEmpty {
                            listOf(
                                VaccineItem(patient.patientId, patient.name, "BCG", "BCG", "Pending", false)
                            )
                        }
                    }
                _items.value = items
            }
        }
    }

    fun markVaccinated(patientId: String, vaccineId: String) {
        viewModelScope.launch {
            patientRepo.updatePatient(patientId, mapOf("syncStatus" to "PENDING"))
        }
    }
}
