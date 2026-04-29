package com.ashasaathi.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ashasaathi.data.model.Patient
import com.ashasaathi.data.model.Worker
import com.ashasaathi.data.repository.AuthRepository
import com.ashasaathi.data.repository.PatientRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeMetrics(val planned: Int = 0, val highRisk: Int = 0, val vaccines: Int = 0, val dots: Int = 0)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val authRepo: AuthRepository,
    private val patientRepo: PatientRepository
) : ViewModel() {

    private val _worker = MutableStateFlow<Worker?>(null)
    val worker: StateFlow<Worker?> = _worker.asStateFlow()

    private val _patients = MutableStateFlow<List<Patient>>(emptyList())
    val patients: StateFlow<List<Patient>> = _patients.asStateFlow()

    val metrics: StateFlow<HomeMetrics> = _patients.map { pts ->
        HomeMetrics(
            planned = pts.count { it.currentRiskLevel != "GREEN" || it.isPregnant || it.isChildUnder5 || it.hasTB },
            highRisk = pts.count { it.currentRiskLevel == "RED" },
            vaccines = pts.count { it.isChildUnder5 },
            dots = pts.count { it.hasTB }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), HomeMetrics())

    val isOnline = MutableStateFlow(true)

    init {
        val uid = authRepo.currentUserId ?: return
        viewModelScope.launch {
            authRepo.observeWorker(uid).collect { w ->
                _worker.value = w
                w?.workerId?.let { id ->
                    patientRepo.observeWorkerPatients(id).collect { _patients.value = it }
                }
            }
        }
    }
}
