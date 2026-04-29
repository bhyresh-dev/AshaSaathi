package com.ashasaathi.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ashasaathi.data.repository.AuthRepository
import com.ashasaathi.data.repository.PatientRepository
import com.ashasaathi.ui.screens.tb.TBPatientDisplay
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TBDotsViewModel @Inject constructor(
    private val authRepo: AuthRepository,
    private val patientRepo: PatientRepository
) : ViewModel() {

    private val _tbPatients = MutableStateFlow<List<TBPatientDisplay>>(emptyList())
    val tbPatients: StateFlow<List<TBPatientDisplay>> = _tbPatients.asStateFlow()

    init {
        val uid = authRepo.currentUserId ?: return
        viewModelScope.launch {
            patientRepo.observeWorkerPatients(uid).collect { patients ->
                _tbPatients.value = patients
                    .filter { it.hasTB }
                    .map { p ->
                        TBPatientDisplay(
                            patientId = p.patientId,
                            name = p.name,
                            nikshayId = p.rchMctsId ?: "N/A",
                            dotsDue = true
                        )
                    }
            }
        }
    }

    fun recordDots(patientId: String) {
        viewModelScope.launch {
            patientRepo.updatePatient(patientId, mapOf("lastVisitDate" to java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())))
        }
    }
}
