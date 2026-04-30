package com.ashasaathi.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ashasaathi.data.model.Household
import com.ashasaathi.data.model.Patient
import com.ashasaathi.data.repository.AuthRepository
import com.ashasaathi.data.repository.HouseholdRepository
import com.ashasaathi.data.repository.PatientRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MapMarker(
    val id: String,
    val lat: Double,
    val lng: Double,
    val title: String,
    val subtitle: String,
    val riskLevel: String = "GREEN",
    val patientId: String? = null
)

@HiltViewModel
class MapViewModel @Inject constructor(
    private val authRepo: AuthRepository,
    private val householdRepo: HouseholdRepository,
    private val patientRepo: PatientRepository
) : ViewModel() {

    private val _markers = MutableStateFlow<List<MapMarker>>(emptyList())
    val markers: StateFlow<List<MapMarker>> = _markers.asStateFlow()

    private val _centerLat = MutableStateFlow(20.5937)
    val centerLat: StateFlow<Double> = _centerLat.asStateFlow()

    private val _centerLng = MutableStateFlow(78.9629)
    val centerLng: StateFlow<Double> = _centerLng.asStateFlow()

    private val _loading = MutableStateFlow(true)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    init { load() }

    private fun load() {
        val uid = authRepo.currentUserId ?: return
        viewModelScope.launch {
            combine(
                householdRepo.observeWorkerHouseholds(uid),
                patientRepo.observeWorkerPatients(uid)
            ) { households, patients ->
                buildMarkers(households, patients)
            }.collect { markers ->
                _markers.value = markers
                _loading.value = false
                markers.firstOrNull()?.let {
                    _centerLat.value = it.lat
                    _centerLng.value = it.lng
                }
            }
        }
    }

    private fun buildMarkers(
        households: List<Household>,
        patients: List<Patient>
    ): List<MapMarker> {
        val patientsByHousehold = patients.groupBy { it.householdId }
        val result = mutableListOf<MapMarker>()

        households.forEach { h ->
            if (h.location.latitude == 0.0 && h.location.longitude == 0.0) return@forEach
            val hhPatients = patientsByHousehold[h.householdId] ?: emptyList()
            val worstRisk = when {
                hhPatients.any { it.currentRiskLevel == "RED" }    -> "RED"
                hhPatients.any { it.currentRiskLevel == "YELLOW" } -> "YELLOW"
                else -> "GREEN"
            }
            result += MapMarker(
                id        = h.householdId,
                lat       = h.location.latitude,
                lng       = h.location.longitude,
                title     = h.headOfFamily,
                subtitle  = "${h.houseNumber}, ${h.village} • ${hhPatients.size} सदस्य",
                riskLevel = worstRisk
            )
        }

        patients.forEach { p ->
            if (p.location.latitude == 0.0 && p.location.longitude == 0.0) return@forEach
            if (households.any { it.householdId == p.householdId }) return@forEach
            result += MapMarker(
                id        = p.patientId,
                lat       = p.location.latitude,
                lng       = p.location.longitude,
                title     = p.name,
                subtitle  = listOfNotNull(p.village, if (p.isPregnant) "गर्भवती" else null).joinToString(" • "),
                riskLevel = p.currentRiskLevel,
                patientId = p.patientId
            )
        }

        return result
    }
}
