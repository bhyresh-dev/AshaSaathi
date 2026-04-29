package com.ashasaathi.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ashasaathi.data.model.Household
import com.ashasaathi.data.model.Patient
import com.ashasaathi.data.repository.HouseholdRepository
import com.ashasaathi.data.repository.PatientRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HouseholdDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val householdRepo: HouseholdRepository,
    private val patientRepo: PatientRepository
) : ViewModel() {

    private val householdId: String = checkNotNull(savedStateHandle["householdId"])

    private val _household = MutableStateFlow<Household?>(null)
    val household: StateFlow<Household?> = _household

    private val _members = MutableStateFlow<List<Patient>>(emptyList())
    val members: StateFlow<List<Patient>> = _members

    init {
        viewModelScope.launch {
            _household.value = householdRepo.getHousehold(householdId)
            _members.value = patientRepo.getPatientsForHousehold(householdId)
        }
    }
}
