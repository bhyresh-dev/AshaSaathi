package com.ashasaathi.data.repository

import com.ashasaathi.data.model.Household
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HouseholdRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private val col get() = firestore.collection("households")

    fun observeWorkerHouseholds(workerId: String): Flow<List<Household>> = callbackFlow {
        val sub = col.whereEqualTo("workerId", workerId)
            .addSnapshotListener { snap, _ ->
                trySend(snap?.documents?.mapNotNull { it.toObject(Household::class.java) } ?: emptyList())
            }
        awaitClose { sub.remove() }
    }

    suspend fun getHousehold(householdId: String): Household? =
        col.document(householdId).get().await().toObject(Household::class.java)

    suspend fun createHousehold(data: Household): Household {
        val id = UUID.randomUUID().toString()
        val h = data.copy(householdId = id)
        col.document(id).set(h).await()
        return h
    }

    suspend fun updateHousehold(householdId: String, updates: Map<String, Any>) {
        col.document(householdId).update(updates + mapOf("syncStatus" to "PENDING")).await()
    }
}
