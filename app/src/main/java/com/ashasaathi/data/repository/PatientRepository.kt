package com.ashasaathi.data.repository

import com.ashasaathi.data.model.Patient
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PatientRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private val col get() = firestore.collection("patients")

    fun observeWorkerPatients(workerId: String): Flow<List<Patient>> = callbackFlow {
        val sub = col.whereEqualTo("workerId", workerId)
            .addSnapshotListener { snap, _ ->
                trySend(snap?.documents?.mapNotNull { it.toObject(Patient::class.java) }
                    ?.filter { it.isActive } ?: emptyList())
            }
        awaitClose { sub.remove() }
    }

    fun observePatient(patientId: String): Flow<Patient?> = callbackFlow {
        val sub = col.document(patientId)
            .addSnapshotListener { snap, _ -> trySend(snap?.toObject(Patient::class.java)) }
        awaitClose { sub.remove() }
    }

    suspend fun getActivePatients(workerId: String): List<Patient> =
        col.whereEqualTo("workerId", workerId)
            .get().await()
            .documents.mapNotNull { it.toObject(Patient::class.java) }
            .filter { it.isActive }

    suspend fun getPatient(patientId: String): Patient? =
        col.document(patientId).get().await().toObject(Patient::class.java)

    fun observeHouseholdPatients(householdId: String): Flow<List<Patient>> = callbackFlow {
        val sub = col.whereEqualTo("householdId", householdId)
            .addSnapshotListener { snap, _ ->
                trySend(snap?.documents?.mapNotNull { it.toObject(Patient::class.java) }
                    ?.filter { it.isActive } ?: emptyList())
            }
        awaitClose { sub.remove() }
    }

    suspend fun getPatientsForHousehold(householdId: String): List<Patient> =
        col.whereEqualTo("householdId", householdId)
            .get().await()
            .documents.mapNotNull { it.toObject(Patient::class.java) }
            .filter { it.isActive }

    suspend fun getHighRiskPatients(workerId: String): List<Patient> =
        col.whereEqualTo("workerId", workerId)
            .whereEqualTo("currentRiskLevel", "RED")
            .whereEqualTo("isActive", true)
            .get().await()
            .documents.mapNotNull { it.toObject(Patient::class.java) }

    suspend fun createPatient(data: Patient): Patient {
        val id = if (data.patientId.isBlank()) UUID.randomUUID().toString() else data.patientId
        val p = data.copy(patientId = id)
        col.document(id).set(p).await()
        return p
    }

    suspend fun updatePatient(patientId: String, updates: Map<String, Any>) {
        col.document(patientId).update(updates + mapOf("syncStatus" to "PENDING")).await()
    }

    suspend fun searchPatients(workerId: String, query: String): List<Patient> {
        val snap = col.whereEqualTo("workerId", workerId)
            .whereEqualTo("isActive", true)
            .limit(100)
            .get().await()
        val q = query.lowercase()
        return snap.documents.mapNotNull { it.toObject(Patient::class.java) }
            .filter {
                it.name.lowercase().contains(q) ||
                it.rchMctsId?.lowercase()?.contains(q) == true ||
                it.phone?.contains(q) == true
            }
    }
}
