package com.stockpulse.app.data.sync

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

/**
 * Señal global de sincronización (FASE 9.5).
 * El AppMessagingService la emite al recibir el mensaje FCM silencioso
 * de tipo "sync" (creado/borrada de alertas, compra/venta, pago
 * acreditado); los ViewModels abiertos refrescan al instante.
 */
object SyncBus {
    private val internal = MutableSharedFlow<Long>(extraBufferCapacity = 8)
    val signal: SharedFlow<Long> = internal

    fun notify() {
        internal.tryEmit(System.currentTimeMillis())
    }
}
