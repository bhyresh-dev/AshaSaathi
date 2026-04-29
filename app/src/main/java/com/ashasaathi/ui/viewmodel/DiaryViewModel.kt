package com.ashasaathi.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ashasaathi.data.model.DiaryEntry
import com.ashasaathi.data.repository.AuthRepository
import com.ashasaathi.data.repository.DiaryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class DiaryViewModel @Inject constructor(
    private val authRepo: AuthRepository,
    private val diaryRepo: DiaryRepository
) : ViewModel() {

    private val _entries = MutableStateFlow<List<DiaryEntry>>(emptyList())
    val entries: StateFlow<List<DiaryEntry>> = _entries

    init {
        authRepo.currentUserId?.let { uid ->
            viewModelScope.launch {
                diaryRepo.observeWorkerEntries(uid).collect { _entries.value = it }
            }
        }
    }

    fun addEntry(content: String) {
        val uid = authRepo.currentUserId ?: return
        val entry = DiaryEntry(
            workerId = uid,
            date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
            content = content,
            createdBy = uid
        )
        viewModelScope.launch { diaryRepo.addEntry(entry) }
    }
}
