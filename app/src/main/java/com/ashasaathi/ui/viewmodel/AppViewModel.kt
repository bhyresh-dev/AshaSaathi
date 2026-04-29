package com.ashasaathi.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ashasaathi.data.repository.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class AppViewModel @Inject constructor(
    prefs: UserPreferencesRepository
) : ViewModel() {
    val language: StateFlow<String> = prefs.language.stateIn(
        scope        = viewModelScope,
        started      = SharingStarted.Eagerly,
        initialValue = "hi"
    )
}
