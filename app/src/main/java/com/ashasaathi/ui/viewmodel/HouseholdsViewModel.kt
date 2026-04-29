package com.ashasaathi.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ashasaathi.data.model.Household
import com.ashasaathi.data.repository.AuthRepository
import com.ashasaathi.data.repository.HouseholdRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HouseholdsViewModel @Inject constructor(
    private val authRepo: AuthRepository,
    private val householdRepo: HouseholdRepository
) : ViewModel() {

    private val _all = MutableStateFlow<List<Household>>(emptyList())
    val search = MutableStateFlow("")
    val loading = MutableStateFlow(true)

    val filtered: StateFlow<List<Household>> = combine(_all, search) { all, q ->
        if (q.isBlank()) all
        else all.filter {
            it.headOfFamily.contains(q, ignoreCase = true) ||
            it.houseNumber.contains(q, ignoreCase = true) ||
            it.village.contains(q, ignoreCase = true)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())

    init {
        val uid = authRepo.currentUserId
        if (uid == null) {
            loading.value = false
        } else {
            viewModelScope.launch { kotlinx.coroutines.delay(5_000); loading.value = false }
            viewModelScope.launch {
                authRepo.observeWorker(uid).collect { worker ->
                    worker?.workerId?.let { id ->
                        householdRepo.observeWorkerHouseholds(id).collect {
                            _all.value = it
                            loading.value = false
                        }
                    }
                }
            }
        }
    }

    val currentWorkerId: String? get() = authRepo.currentUserId

    fun onSearch(q: String) { search.value = q }

    fun createHousehold(data: Household) = viewModelScope.launch {
        householdRepo.createHousehold(data)
    }
}
