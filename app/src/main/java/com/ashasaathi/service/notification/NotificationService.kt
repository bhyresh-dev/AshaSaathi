package com.ashasaathi.service.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.ashasaathi.MainActivity
import com.ashasaathi.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        const val CHANNEL_REMINDERS  = "asha_reminders"
        const val CHANNEL_RISK       = "asha_risk_alerts"
        const val CHANNEL_SYNC       = "asha_sync"
    }

    init { createChannels() }

    private fun createChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        nm.createNotificationChannel(NotificationChannel(
            CHANNEL_REMINDERS, "Visit Reminders",
            NotificationManager.IMPORTANCE_DEFAULT
        ).also { it.description = "Daily visit reminders for ASHA workers" })

        nm.createNotificationChannel(NotificationChannel(
            CHANNEL_RISK, "Risk Alerts",
            NotificationManager.IMPORTANCE_HIGH
        ).also {
            it.description = "High-risk patient alerts"
            it.enableVibration(true)
        })

        nm.createNotificationChannel(NotificationChannel(
            CHANNEL_SYNC, "Sync Status",
            NotificationManager.IMPORTANCE_LOW
        ).also { it.description = "Background sync status" })
    }

    fun showRiskAlert(patientName: String, riskReason: String, notifId: Int = 1000) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pi = PendingIntent.getActivity(context, notifId, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val notif = NotificationCompat.Builder(context, CHANNEL_RISK)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("⚠️ उच्च जोखिम: $patientName")
            .setContentText(riskReason)
            .setStyle(NotificationCompat.BigTextStyle().bigText(riskReason))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pi)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(notifId, notif)
    }

    fun showVaccineReminder(patientName: String, vaccineName: String, notifId: Int) {
        val notif = NotificationCompat.Builder(context, CHANNEL_REMINDERS)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle("💉 टीका: $patientName")
            .setContentText("$vaccineName आज देना है")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(notifId, notif)
    }

    fun showDailyWorkplan(visitCount: Int, highRiskCount: Int) {
        val notif = NotificationCompat.Builder(context, CHANNEL_REMINDERS)
            .setSmallIcon(android.R.drawable.ic_menu_agenda)
            .setContentTitle("🌅 आज का कार्यक्रम")
            .setContentText("$visitCount विजिट • $highRiskCount उच्च जोखिम")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(2000, notif)
    }
}
