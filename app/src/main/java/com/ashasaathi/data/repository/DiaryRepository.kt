package com.ashasaathi.data.repository

import com.ashasaathi.data.model.DiaryEntry
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DiaryRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private val col get() = firestore.collection("diary")

    fun observeWorkerEntries(workerId: String): Flow<List<DiaryEntry>> = callbackFlow {
        val sub = col.whereEqualTo("workerId", workerId)
            .addSnapshotListener { snap, _ ->
                val entries = snap?.documents
                    ?.mapNotNull { it.toObject(DiaryEntry::class.java) }
                    ?.sortedByDescending { it.date }
                    ?: emptyList()
                trySend(entries)
            }
        awaitClose { sub.remove() }
    }

    suspend fun addEntry(entry: DiaryEntry): DiaryEntry {
        val id = if (entry.entryId.isBlank()) UUID.randomUUID().toString() else entry.entryId
        val e = entry.copy(entryId = id)
        col.document(id).set(e).await()
        return e
    }
}
