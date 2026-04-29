package com.ashasaathi.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ashasaathi.data.model.Worker
import com.ashasaathi.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepo: AuthRepository
) : ViewModel() {

    val isLoggedIn = authRepo.authState

    private val _worker = MutableStateFlow<Worker?>(null)
    val worker: StateFlow<Worker?> = _worker.asStateFlow()

    init {
        val uid = authRepo.currentUserId
        if (uid != null) loadWorker(uid)
    }

    fun loadWorker(workerId: String) {
        viewModelScope.launch {
            authRepo.observeWorker(workerId).collect { _worker.value = it }
        }
    }

    fun signOut() = viewModelScope.launch { authRepo.signOut() }
}
