package com.ashasaathi.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ashasaathi.data.repository.AuthRepository
import com.ashasaathi.data.repository.HouseholdRepository
import com.ashasaathi.data.repository.PatientRepository
import com.ashasaathi.data.repository.VaccinationRepository
import com.ashasaathi.data.repository.VisitRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class ReportState(
    val periodLabel: String,
    val totalHouseholds: Int,
    val totalPatients: Int,
    val visitsThisMonth: Int,
    val pregnantWomen: Int,
    val ancRegistered1stTrimester: Int,
    val anc4Plus: Int,
    val ttVaccinated: Int,
    val ifa180Complete: Int,
    val highRiskCount: Int,
    val institutionalDeliveries: Int,
    val childrenUnder5: Int,
    val ficCount: Int,
    val ficPercent: Int,
    val cicCount: Int,
    val vaccinesDueToday: Int,
    val vaccinesMissed: Int,
    val tbPatients: Int,
    val dotsAdherenceGood: Int,
    val dotsAdherencePoor: Int,
    val dbtThisMonth: Int,
    val jsyBeneficiaries: Int,
    val pmmvyBeneficiaries: Int,
    val activitySummary: Map<String, Int>
)

@HiltViewModel
class ReportsViewModel @Inject constructor(
    private val authRepo: AuthRepository,
    private val patientRepo: PatientRepository,
    private val visitRepo: VisitRepository,
    private val householdRepo: HouseholdRepository,
    private val vaccinationRepo: VaccinationRepository
) : ViewModel() {

    private val _reportState = MutableStateFlow<ReportState?>(null)
    val reportState: StateFlow<ReportState?> = _reportState.asStateFlow()

    private val monthFmt = SimpleDateFormat("yyyy-MM", Locale.getDefault())
    private val labelFmt = SimpleDateFormat("MMMM yyyy", Locale("hi"))

    private var householdCount = 0

    init {
        authRepo.currentUserId?.let { uid ->
            viewModelScope.launch {
                householdRepo.observeWorkerHouseholds(uid).collect { hh ->
                    householdCount = hh.size
                }
            }
        }
        load()
    }

    private fun load() {
        val uid = authRepo.currentUserId ?: return
        viewModelScope.launch {
            val thisMonth = monthFmt.format(Date())
            val patients = patientRepo.getActivePatients(uid)
            val visits = visitRepo.getVisitsForWorker(uid)

            val visitsThisMonth = visits.filter { it.visitDate.startsWith(thisMonth) }

            val ficTotal = patients.count { it.ficStatus }
            val under5 = patients.count { it.isChildUnder5 }

            _reportState.value = ReportState(
                periodLabel                = labelFmt.format(Date()),
                totalHouseholds            = householdCount,
                totalPatients              = patients.size,
                visitsThisMonth            = visitsThisMonth.size,
                pregnantWomen              = patients.count { it.isPregnant },
                ancRegistered1stTrimester  = patients.count { it.isPregnant && (it.trimester ?: 4) == 1 },
                anc4Plus                   = patients.count { it.ancCount >= 4 },
                ttVaccinated               = patients.count { it.ttDoseStatus in listOf("TT2", "BOOSTER") },
                ifa180Complete             = patients.count { it.ifaCumulativeCount >= 180 },
                highRiskCount              = patients.count { it.currentRiskLevel == "RED" },
                institutionalDeliveries    = patients.count { it.deliveryPlace in listOf("Hospital", "PHC", "CHC", "FRU") },
                childrenUnder5             = under5,
                ficCount                   = ficTotal,
                ficPercent                 = if (under5 > 0) (ficTotal * 100 / under5) else 0,
                cicCount                   = patients.count { it.cicStatus },
                vaccinesDueToday           = 0,
                vaccinesMissed             = 0,
                tbPatients                 = patients.count { it.hasTB },
                dotsAdherenceGood          = 0,
                dotsAdherencePoor          = 0,
                dbtThisMonth               = 0,
                jsyBeneficiaries           = patients.count { it.jsyInstallments.isNotEmpty() },
                pmmvyBeneficiaries         = patients.count { it.pmmvyInstallments.isNotEmpty() },
                activitySummary            = mapOf(
                    "H1 - गृह भ्रमण" to visitsThisMonth.size,
                    "H2 - ANC विजिट" to visitsThisMonth.count { it.visitType == "ANC" },
                    "H3 - PNC विजिट" to visitsThisMonth.count { it.visitType == "PNC" },
                    "H4 - बच्चा विजिट" to visitsThisMonth.count { it.visitType == "CHILD" },
                    "H5 - TB DOTS" to visitsThisMonth.count { it.visitType == "TB" }
                )
            )
        }
    }

    fun exportReport() {
        // TODO: generate CSV / PDF using iText when called
    }
}
