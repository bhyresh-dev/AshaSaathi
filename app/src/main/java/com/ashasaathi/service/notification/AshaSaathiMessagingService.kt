package com.ashasaathi.service.notification

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class AshaSaathiMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        // Handle FCM data/notification messages here
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // Store new FCM token — update via AuthRepository when needed
    }
}
