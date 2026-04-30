package com.ashasaathi.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ashasaathi.data.model.Worker
import com.ashasaathi.data.repository.AuthRepository
import com.ashasaathi.data.repository.UserPreferencesRepository
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val authRepo: AuthRepository,
    private val firestore: FirebaseFirestore,
    private val prefs: UserPreferencesRepository
) : ViewModel() {

    private val _worker = MutableStateFlow<Worker?>(null)
    val worker: StateFlow<Worker?> = _worker.asStateFlow()

    private val _language = MutableStateFlow("hi")
    val language: StateFlow<String> = _language.asStateFlow()

    init {
        // Seed from DataStore immediately (survives before worker profile loads)
        viewModelScope.launch {
            prefs.language.collect { lang -> _language.value = lang }
        }
        authRepo.currentUserId?.let { uid ->
            viewModelScope.launch {
                runCatching {
                    authRepo.observeWorker(uid).collect { w ->
                        _worker.value = w
                        if (w?.languagePreference != null) _language.value = w.languagePreference
                    }
                }
            }
        }
    }

    fun setLanguage(code: String) {
        val uid = authRepo.currentUserId ?: return
        _language.value = code
        viewModelScope.launch {
            prefs.setLanguage(code)
            runCatching {
                firestore.collection("workers").document(uid)
                    .update("languagePreference", code).await()
            }
        }
    }

    fun logout() {
        viewModelScope.launch { authRepo.signOut() }
    }

}
