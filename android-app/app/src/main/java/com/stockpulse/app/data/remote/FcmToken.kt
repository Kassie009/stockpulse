package com.stockpulse.app.data.remote

import com.google.firebase.messaging.FirebaseMessaging
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * Obtiene el token FCM del dispositivo y lo detiene sin lanzar error si
 * Firebase no esta disponible (p.ej. servicios de play ausentes).
 */
suspend fun currentFcmToken(): String? = suspendCoroutine { cont ->
    FirebaseMessaging.getInstance().token
        .addOnSuccessListener { token -> cont.resume(token) }
        .addOnFailureListener { cont.resume(null) }
}
