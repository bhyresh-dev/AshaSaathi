package com.ashasaathi.data.repository

import com.ashasaathi.data.model.Visit
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VisitRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private val col get() = firestore.collection("visits")

    fun observePatientVisits(patientId: String): Flow<List<Visit>> = callbackFlow {
        val sub = col.whereEqualTo("patientId", patientId)
            .addSnapshotListener { snap, _ ->
                trySend(snap?.documents?.mapNotNull { it.toObject(Visit::class.java) } ?: emptyList())
            }
        awaitClose { sub.remove() }
    }

    suspend fun getVisitsForWorker(workerId: String): List<Visit> =
        col.whereEqualTo("workerId", workerId)
            .limit(100)
            .get().await()
            .documents.mapNotNull { it.toObject(Visit::class.java) }

    suspend fun createVisit(data: Visit): Visit {
        val id = UUID.randomUUID().toString()
        val v = data.copy(visitId = id)
        col.document(id).set(v).await()
        return v
    }

    suspend fun updateVisit(visitId: String, updates: Map<String, Any>) {
        col.document(visitId).update(updates + mapOf("syncStatus" to "PENDING")).await()
    }
}
