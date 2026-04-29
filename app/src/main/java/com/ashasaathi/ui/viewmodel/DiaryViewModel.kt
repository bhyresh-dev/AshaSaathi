package com.ashasaathi.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ashasaathi.data.model.DiaryEntry
import com.ashasaathi.data.repository.AuthRepository
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class DiaryViewModel @Inject constructor(
    private val authRepo: AuthRepository,
    private val firestore: FirebaseFirestore
) : ViewModel() {

    private val _entries = MutableStateFlow<List<DiaryEntry>>(emptyList())
    val entries: StateFlow<List<DiaryEntry>> = _entries

    init { loadEntries() }

    private fun loadEntries() {
        val uid = authRepo.currentUserId ?: return
        firestore.collection("diary")
            .whereEqualTo("workerId", uid)
            .addSnapshotListener { snap, _ ->
                _entries.value = snap?.documents
                    ?.mapNotNull { it.toObject(DiaryEntry::class.java) }
                    ?.sortedByDescending { it.date }
                    ?: emptyList()
            }
    }

    fun addEntry(content: String) {
        val uid = authRepo.currentUserId ?: return
        val entry = DiaryEntry(
            entryId = UUID.randomUUID().toString(),
            workerId = uid,
            date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
            content = content,
            createdBy = uid
        )
        viewModelScope.launch {
            firestore.collection("diary").document(entry.entryId).set(entry).await()
        }
    }
}
