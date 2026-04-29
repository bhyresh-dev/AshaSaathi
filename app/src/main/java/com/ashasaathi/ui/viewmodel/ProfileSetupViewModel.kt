package com.ashasaathi.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ashasaathi.data.model.Worker
import com.ashasaathi.data.repository.AuthRepository
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

data class ProfileSetupState(
    val saving: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class ProfileSetupViewModel @Inject constructor(
    private val authRepo: AuthRepository,
    private val firestore: FirebaseFirestore
) : ViewModel() {

    private val _state = MutableStateFlow(ProfileSetupState())
    val state: StateFlow<ProfileSetupState> = _state.asStateFlow()

    fun saveProfile(
        name: String,
        rchId: String,
        phcName: String,
        subCentre: String,
        village: String,
        district: String,
        onComplete: () -> Unit
    ) {
        val uid = authRepo.currentUserId ?: return
        _state.value = ProfileSetupState(saving = true)
        viewModelScope.launch {
            runCatching {
                val worker = Worker(
                    workerId = uid,
                    name = name,
                    rchPortalId = rchId,
                    phcName = phcName,
                    subCentre = subCentre,
                    village = village,
                    district = district,
                    createdBy = uid
                )
                firestore.collection("workers").document(uid).set(worker).await()
            }.onSuccess {
                _state.value = ProfileSetupState()
                onComplete()
            }.onFailure {
                _state.value = ProfileSetupState(error = it.message)
            }
        }
    }
}
