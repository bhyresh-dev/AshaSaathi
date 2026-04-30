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
import javax.inject.Inject

@HiltViewModel
class PatientDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val patientRepo: PatientRepository,
    private val visitRepo: VisitRepository
) : ViewModel() {

    private val patientId: String = checkNotNull(savedStateHandle["patientId"])

    val patient: StateFlow<Patient?> = patientRepo.observePatient(patientId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)

    val visits: StateFlow<List<Visit>> = visitRepo.observePatientVisits(patientId)
        .map { it.sortedByDescending { v -> v.visitDate } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())
}
