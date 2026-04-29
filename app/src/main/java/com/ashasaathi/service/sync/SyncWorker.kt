package com.ashasaathi.service.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val uid = auth.currentUser?.uid ?: return Result.success()

        return runCatching {
            // Firestore offline persistence handles sync automatically.
            // This worker triggers a re-connect and flushes the pending writes
            // queue by enabling network access for a period.
            firestore.enableNetwork()
            Result.success()
        }.getOrElse {
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }

    companion object {
        const val WORK_NAME = "asha_background_sync"

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 5, TimeUnit.MINUTES)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
