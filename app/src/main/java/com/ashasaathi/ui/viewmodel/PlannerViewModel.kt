package com.ashasaathi.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ashasaathi.data.model.Patient
import com.ashasaathi.data.repository.AuthRepository
import com.ashasaathi.data.repository.PatientRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlannerViewModel @Inject constructor(
    private val authRepo: AuthRepository,
    private val patientRepo: PatientRepository
) : ViewModel() {

    private val _loading = MutableStateFlow(true)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _visitedIds = MutableStateFlow<Set<String>>(emptySet())

    private val _allPatients = MutableStateFlow<List<Patient>>(emptyList())

    val prioritizedPatients: StateFlow<List<Patient>> = _allPatients
        .map { patients ->
            patients.sortedWith(compareBy {
                when (it.currentRiskLevel) {
                    "RED"    -> 0
                    "YELLOW" -> 1
                    else     -> 2
                }
            })
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        val uid = authRepo.currentUserId
        if (uid == null) {
            _loading.value = false
        } else {
            viewModelScope.launch { kotlinx.coroutines.delay(5_000); _loading.value = false }
            viewModelScope.launch {
                patientRepo.observeWorkerPatients(uid).collect { patients ->
                    _allPatients.value = patients
                    _loading.value = false
                }
            }
        }
    }

    fun markVisited(patientId: String) {
        _visitedIds.value = _visitedIds.value + patientId
        viewModelScope.launch {
            patientRepo.updatePatient(
                patientId,
                mapOf("lastVisitDate" to java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date()))
            )
        }
    }

    fun isVisited(patientId: String): Boolean = patientId in _visitedIds.value
}
