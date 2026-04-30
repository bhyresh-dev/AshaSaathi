package com.ashasaathi.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ashasaathi.data.model.Patient
import com.ashasaathi.data.model.Worker
import com.ashasaathi.data.repository.AuthRepository
import com.ashasaathi.data.repository.HouseholdRepository
import com.ashasaathi.data.repository.PatientRepository
import com.ashasaathi.data.repository.VisitRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class HomeMetrics(
    val planned:  Int = 0,
    val highRisk: Int = 0,
    val vaccines: Int = 0,
    val dots:     Int = 0
)

data class TotalRecords(
    val households:     Int = 0,
    val patients:       Int = 0,
    val visitsThisMonth: Int = 0
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val authRepo:      AuthRepository,
    private val patientRepo:   PatientRepository,
    private val householdRepo: HouseholdRepository,
    private val visitRepo:     VisitRepository
) : ViewModel() {

    private val _worker   = MutableStateFlow<Worker?>(null)
    val worker: StateFlow<Worker?> = _worker.asStateFlow()

    private val _patients = MutableStateFlow<List<Patient>>(emptyList())
    val patients: StateFlow<List<Patient>> = _patients.asStateFlow()

    private val _loading  = MutableStateFlow(true)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _totalRecords = MutableStateFlow(TotalRecords())
    val totalRecords: StateFlow<TotalRecords> = _totalRecords.asStateFlow()

    val metrics: StateFlow<HomeMetrics> = _patients.map { pts ->
        HomeMetrics(
            planned  = pts.count { it.currentRiskLevel != "GREEN" || it.isPregnant || it.isChildUnder5 || it.hasTB },
            highRisk = pts.count { it.currentRiskLevel == "RED" },
            vaccines = pts.count { it.isChildUnder5 && !it.ficStatus },
            dots     = pts.count { it.hasTB }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeMetrics())

    val isOnline = MutableStateFlow(true)

    private val monthFmt = SimpleDateFormat("yyyy-MM", Locale.getDefault())

    init {
        val uid = authRepo.currentUserId
        if (uid == null) {
            _loading.value = false
        } else {
            viewModelScope.launch { delay(3_000); _loading.value = false }

            viewModelScope.launch {
                authRepo.observeWorker(uid).collect { w -> _worker.value = w }
            }

            viewModelScope.launch {
                patientRepo.observeWorkerPatients(uid).collect { pts ->
                    _patients.value = pts
                    _loading.value  = false
                }
            }

            viewModelScope.launch {
                val thisMonth = monthFmt.format(Date())
                combine(
                    householdRepo.observeWorkerHouseholds(uid),
                    patientRepo.observeWorkerPatients(uid),
                    visitRepo.observeWorkerVisits(uid)
                ) { households, patients, visits ->
                    TotalRecords(
                        households      = households.size,
                        patients        = patients.size,
                        visitsThisMonth = visits.count { it.visitDate.startsWith(thisMonth) }
                    )
                }.collect { _totalRecords.value = it }
            }
        }
    }
}
