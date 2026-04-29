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

data class HomeMetrics(
    val planned: Int = 0,
    val highRisk: Int = 0,
    val vaccines: Int = 0,
    val dots: Int = 0
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val authRepo: AuthRepository,
    private val patientRepo: PatientRepository
) : ViewModel() {

    private val _worker  = MutableStateFlow<Worker?>(null)
    val worker: StateFlow<Worker?> = _worker.asStateFlow()

    private val _patients = MutableStateFlow<List<Patient>>(emptyList())
    val patients: StateFlow<List<Patient>> = _patients.asStateFlow()

    private val _loading = MutableStateFlow(true)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    val metrics: StateFlow<HomeMetrics> = _patients.map { pts ->
        HomeMetrics(
            planned  = pts.count { it.currentRiskLevel != "GREEN" || it.isPregnant || it.isChildUnder5 || it.hasTB },
            highRisk = pts.count { it.currentRiskLevel == "RED" },
            vaccines = pts.count { it.isChildUnder5 && !it.ficStatus },
            dots     = pts.count { it.hasTB }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeMetrics())

    val isOnline = MutableStateFlow(true)

    init {
        authRepo.currentUserId?.let { uid ->
            viewModelScope.launch {
                authRepo.observeWorker(uid)
                    .onEach { w ->
                        _worker.value = w
                        _loading.value = w == null
                    }
                    .filterNotNull()
                    .flatMapLatest { patientRepo.observeWorkerPatients(it.workerId) }
                    .collect {
                        _patients.value = it
                        _loading.value  = false
                    }
            }
        }
    }
}
