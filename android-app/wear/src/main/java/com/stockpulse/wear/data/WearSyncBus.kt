package com.stockpulse.wear.data

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

/**
 * Señal de sincronización del reloj: se emite cuando el backend manda
 * el mensaje FCM silencioso "sync" (por ejemplo, borraste una alerta
 * en el telefono). MainActivity la escucha y recarga al instante.
 */
object WearSyncBus {
    private val internal = MutableSharedFlow<Long>(extraBufferCapacity = 8)
    val signal: SharedFlow<Long> = internal

    fun notify() {
        internal.tryEmit(System.currentTimeMillis())
    }
}
