package com.stockpulse.app.data.push

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.stockpulse.app.MainActivity
import com.stockpulse.app.R
import com.stockpulse.app.data.remote.currentFcmToken
import dagger.hilt.android.AndroidEntryPoint
import com.stockpulse.app.domain.repository.AlertsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * FASE 7 - Servicio que recibe las notificaciones de alertas (FCM).
 * En primer plano muestra la notificacion manualmente; en segundo plano
 * el sistema la muestra solo (comportamiento FCM estandar).
 */
@AndroidEntryPoint
class AppMessagingService : FirebaseMessagingService() {

    @Inject
    lateinit var alertsRepository: AlertsRepository

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        val notification = remoteMessage.notification ?: return
        showNotification(
            title = notification.title ?: "Alerta de precio",
            body = notification.body ?: ""
        )
    }

    /** Token renovado: lo re-registratamos con el backend para no perder avisos. */
    override fun onNewToken(token: String) {
        CoroutineScope(Dispatchers.IO).launch {
            alertsRepository.registerDevice(token, "android-phone")
        }
    }

    private fun showNotification(title: String, body: String) {
        val channelId = "stockpulse_alerts"
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Alertas de precio",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = "Avisos cuando se cumplen tus alertas del mercado" }
            manager.createNotificationChannel(channel)
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pending = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pending)
            .build()

        manager.notify((System.currentTimeMillis() % Int.MAX_VALUE).toInt(), notification)
    }
}
