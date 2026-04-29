package com.ashasaathi.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ashasaathi.data.model.Patient
import com.ashasaathi.data.model.Visit
import com.ashasaathi.data.repository.PatientRepository
import com.ashasaathi.data.repository.VisitRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MCPCardViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val patientRepo: PatientRepository,
    private val visitRepo: VisitRepository
) : ViewModel() {

    private val patientId: String = checkNotNull(savedStateHandle["patientId"])

    private val _patient = MutableStateFlow<Patient?>(null)
    val patient: StateFlow<Patient?> = _patient

    val ancVisits: StateFlow<List<Visit>> = visitRepo.observePatientVisits(patientId)
        .map { visits -> visits.filter { it.visitType == "ANC" }.sortedBy { it.visitDate } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())

    init {
        viewModelScope.launch { _patient.value = patientRepo.getPatient(patientId) }
    }
}
