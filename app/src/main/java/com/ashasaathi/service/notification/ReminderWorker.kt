package com.ashasaathi.service.notification

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.ashasaathi.data.model.UIPSchedule
import com.ashasaathi.data.repository.PatientRepository
import com.ashasaathi.data.repository.VaccinationRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

@HiltWorker
class ReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val auth: FirebaseAuth,
    private val patientRepo: PatientRepository,
    private val vaccinationRepo: VaccinationRepository,
    private val notificationService: NotificationService
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val uid = auth.currentUser?.uid ?: return Result.success()

        runCatching {
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val patients = patientRepo.getActivePatients(uid)
            var notifId = 3000

            patients.forEach { patient ->
                // Vaccine due reminders (3 days ahead)
                if (patient.isChildUnder5 && patient.dob.isNotBlank()) {
                    val dob = runCatching {
                        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(patient.dob)
                    }.getOrNull()

                    dob?.let { dobDate ->
                        val ageNow = ((Date().time - dobDate.time) / 86400000L).toInt()
                        UIPSchedule.vaccines.forEach { vaccine ->
                            val dueAgeDay = vaccine.targetAgeDays
                            val daysUntilDue = dueAgeDay - ageNow
                            if (daysUntilDue in 0..3) {
                                val records = vaccinationRepo.getVaccineStatus(patient.patientId)
                                val alreadyGiven = records.any { it.vaccineId == vaccine.id && it.administeredDate != null }
                                if (!alreadyGiven) {
                                    notificationService.showVaccineReminder(
                                        patient.name,
                                        vaccine.nameHi,
                                        notifId++
                                    )
                                }
                            }
                        }
                    }
                }

                // High-risk revisit (not seen in 7 days)
                if (patient.currentRiskLevel == "RED") {
                    patient.lastVisitDate?.let { lvd ->
                        val last = runCatching {
                            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(lvd)
                        }.getOrNull()
                        last?.let {
                            val daysSince = ((Date().time - it.time) / 86400000L).toInt()
                            if (daysSince >= 7) {
                                notificationService.showRiskAlert(
                                    patient.name,
                                    "${daysSince} दिनों से विजिट नहीं — तत्काल जाएं",
                                    notifId++
                                )
                            }
                        }
                    }
                }
            }

            // Daily workplan summary
            val highRisk = patients.count { it.currentRiskLevel == "RED" }
            notificationService.showDailyWorkplan(patients.size, highRisk)
        }

        return Result.success()
    }

    companion object {
        const val WORK_NAME = "asha_daily_reminder"

        fun scheduleDaily(context: Context) {
            val now = Calendar.getInstance()
            val target = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 7)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                if (before(now)) add(Calendar.DAY_OF_YEAR, 1)
            }
            val delay = target.timeInMillis - now.timeInMillis

            val request = OneTimeWorkRequestBuilder<ReminderWorker>()
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                request
            )
        }
    }
}
