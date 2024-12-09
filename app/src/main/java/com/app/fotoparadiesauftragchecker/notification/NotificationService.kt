package com.app.fotoparadiesauftragchecker.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.app.fotoparadiesauftragchecker.MainActivity
import com.app.fotoparadiesauftragchecker.R

class NotificationService(private val context: Context) {
    companion object {
        private const val CHANNEL_ID = "order_ready_channel"
        private const val CHANNEL_NAME = "Auftragsbenachrichtigungen"
        private const val CHANNEL_DESCRIPTION = "Benachrichtigungen für abholbereite Aufträge"
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESCRIPTION
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showOrderReadyNotification(orderId: String, retailerId: String) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_photo)  // Hier verwenden wir das neue Foto-Icon
            .setContentTitle("Auftrag abholbereit")
            .setContentText("Dein Auftrag $orderId kann jetzt in der DM Filiale $retailerId abgeholt werden.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).apply {
                notify(orderId.hashCode(), notification)
            }
        } catch (e: SecurityException) {
            // Handle the case where notification permission is not granted
            e.printStackTrace()
        }
    }
}
