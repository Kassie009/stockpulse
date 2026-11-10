package com.stockpulse.wear

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/**
 * Recibe las alertas de precio (FCM) y las muestra en la muneca.
 * En background el sistema crea la notificacion con los datos
 * nucleo; en foreground procedemos nosotros para no perder ninguna.
 */
class WearMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(message: RemoteMessage) {
        // Mensaje silencioso de sincronizacion: recarga datos sin avisar
        if (message.data["type"] == "sync") {
            com.stockpulse.wear.data.WearSyncBus.notify()
            return
        }
        val notification = message.notification ?: return
        show(notification.title ?: "Alerta de precio", notification.body ?: "")
    }

    private fun show(title: String, body: String) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    "Alertas de precio",
                    NotificationManager.IMPORTANCE_HIGH
                )
            )
        }
        val open = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val pending = PendingIntent.getActivity(
            this, 0, open,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val notif = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()
        manager.notify(1001, notif)
    }

    private companion object {
        const val CHANNEL_ID = "stockpulse_alerts"
    }
}
