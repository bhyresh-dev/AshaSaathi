package com.ashasaathi.data.repository

import com.ashasaathi.data.model.UIPSchedule
import com.ashasaathi.data.model.UIPVaccine
import com.ashasaathi.data.model.VaccinationRecord
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

enum class VaccineStatus { ADMINISTERED, DUE_TODAY, UPCOMING, MISSED }

data class VaccineScheduleEntry(
    val vaccine: UIPVaccine,
    val record: VaccinationRecord?,
    val status: VaccineStatus,
    val scheduledDate: String,
    val daysOverdue: Int = 0
)

@Singleton
class VaccinationRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private val col get() = firestore.collection("vaccinations")
    private val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    fun observePatientVaccinations(patientId: String): Flow<List<VaccinationRecord>> = callbackFlow {
        val sub = col.whereEqualTo("patientId", patientId)
            .addSnapshotListener { snap, _ ->
                trySend(snap?.documents?.mapNotNull { it.toObject(VaccinationRecord::class.java) } ?: emptyList())
            }
        awaitClose { sub.remove() }
    }

    suspend fun getVaccineStatus(patientId: String): List<VaccinationRecord> =
        col.whereEqualTo("patientId", patientId).get().await()
            .documents.mapNotNull { it.toObject(VaccinationRecord::class.java) }

    fun buildSchedule(patientId: String, dobStr: String, records: List<VaccinationRecord>): List<VaccineScheduleEntry> {
        val dob = runCatching { fmt.parse(dobStr) }.getOrNull() ?: return emptyList()
        val today = Date()
        val ageNow = ((today.time - dob.time) / 86400000L).toInt()

        return UIPSchedule.vaccines.map { vaccine ->
            val dueDate = Date(dob.time + vaccine.targetAgeDays.toLong() * 86400000L)
            val dueDateStr = fmt.format(dueDate)
            val existing = records.firstOrNull { it.vaccineId == vaccine.id }
            val status = when {
                existing?.administeredDate != null -> VaccineStatus.ADMINISTERED
                ageNow > vaccine.ageWindowDays.last -> VaccineStatus.MISSED
                ageNow >= vaccine.ageWindowDays.first -> VaccineStatus.DUE_TODAY
                else -> VaccineStatus.UPCOMING
            }
            val daysOverdue = if (status == VaccineStatus.MISSED) ageNow - vaccine.ageWindowDays.last else 0
            VaccineScheduleEntry(vaccine, existing, status, dueDateStr, daysOverdue)
        }
    }

    suspend fun recordVaccine(record: VaccinationRecord): VaccinationRecord {
        val id = if (record.recordId.isBlank()) UUID.randomUUID().toString() else record.recordId
        val r = record.copy(recordId = id, administeredDate = record.administeredDate ?: fmt.format(Date()))
        col.document(id).set(r).await()
        return r
    }

    // FIC = BCG + OPV3 + Penta3 + Measles1 administered before 1 year
    fun computeFIC(entries: List<VaccineScheduleEntry>): Boolean {
        val ficVaccines = setOf("bcg", "opv3", "penta3", "mr1")
        return ficVaccines.all { id -> entries.any { it.vaccine.id == id && it.status == VaccineStatus.ADMINISTERED } }
    }

    // CIC = FIC + booster doses
    fun computeCIC(entries: List<VaccineScheduleEntry>): Boolean {
        val cicVaccines = setOf("bcg", "opv3", "penta3", "mr1", "dptb1", "mr2", "opvb")
        return cicVaccines.all { id -> entries.any { it.vaccine.id == id && it.status == VaccineStatus.ADMINISTERED } }
    }
}
