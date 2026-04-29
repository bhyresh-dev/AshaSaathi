package com.ashasaathi.data.repository

import com.ashasaathi.data.model.Worker
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {
    val currentUserId: String? get() = auth.currentUser?.uid

    val authState: Flow<Boolean> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser != null) }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    suspend fun signInWithPhone(credential: com.google.firebase.auth.PhoneAuthCredential): Result<String> =
        runCatching {
            val result = auth.signInWithCredential(credential).await()
            result.user?.uid ?: error("No user after sign in")
        }

    suspend fun signOut() = auth.signOut()

    suspend fun getWorker(workerId: String): Worker? =
        firestore.collection("workers").document(workerId).get().await()
            .toObject(Worker::class.java)

    fun observeWorker(workerId: String): Flow<Worker?> = callbackFlow {
        val sub = firestore.collection("workers").document(workerId)
            .addSnapshotListener { snap, _ -> trySend(snap?.toObject(Worker::class.java)) }
        awaitClose { sub.remove() }
    }

    suspend fun updateFcmToken(workerId: String, token: String) {
        firestore.collection("workers").document(workerId)
            .update("fcmToken", token).await()
    }
}
