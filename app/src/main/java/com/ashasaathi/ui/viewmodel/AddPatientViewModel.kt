package com.ashasaathi.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ashasaathi.data.model.Patient
import com.ashasaathi.data.repository.AuthRepository
import com.ashasaathi.data.repository.PatientRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AddPatientState(
    val saving: Boolean = false,
    val error: String? = null,
    val saved: Boolean = false
)

@HiltViewModel
class AddPatientViewModel @Inject constructor(
    private val authRepo: AuthRepository,
    private val patientRepo: PatientRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val householdId: String = savedStateHandle["householdId"] ?: ""

    private val _state = MutableStateFlow(AddPatientState())
    val state: StateFlow<AddPatientState> = _state.asStateFlow()

    fun savePatient(
        name: String,
        dob: String,
        gender: String,
        phone: String,
        village: String,
        isPregnant: Boolean,
        isChildUnder5: Boolean,
        hasTB: Boolean,
        isElderly: Boolean,
        nikshayId: String,
        onComplete: () -> Unit
    ) {
        val uid = authRepo.currentUserId ?: run {
            _state.value = AddPatientState(error = "Not logged in")
            return
        }
        _state.value = AddPatientState(saving = true)
        viewModelScope.launch {
            runCatching {
                val patient = Patient(
                    householdId   = householdId,
                    workerId      = uid,
                    name          = name.trim(),
                    dob           = dob.trim(),
                    gender        = gender,
                    phone         = phone.trim().ifBlank { null },
                    village       = village.trim().ifBlank { null },
                    isPregnant    = isPregnant,
                    isChildUnder5 = isChildUnder5,
                    hasTB         = hasTB,
                    isElderly     = isElderly,
                    nikshayId     = nikshayId.trim().ifBlank { null },
                    isActive      = true,
                    createdBy     = uid
                )
                patientRepo.createPatient(patient)
            }.onSuccess {
                _state.value = AddPatientState(saved = true)
                onComplete()
            }.onFailure { e ->
                _state.value = AddPatientState(error = e.message ?: "Save failed")
            }
        }
    }
}
