package com.stockpulse.wear.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.security.MessageDigest
import kotlinx.coroutines.flow.first

private val Context.wearPinStore by preferencesDataStore(name = "stockpulse_wear_pin")

/**
 * PIN local de desbloqueo (4 digitos). Se guarda SOLO como hash SHA-256:
 * ni siquiera la app conoce el PIN en texto claro. Si el usuario lo
 * olvida, se cierra la sesion y se crea uno nuevo tras re-autenticar.
 */
object WearPin {

    private val keyHash = stringPreferencesKey("pin_hash")

    suspend fun hasPin(context: Context): Boolean =
        context.wearPinStore.data.first()[keyHash] != null

    suspend fun setPin(context: Context, pin: String) {
        context.wearPinStore.edit { it[keyHash] = hash(pin) }
    }

    suspend fun checkPin(context: Context, pin: String): Boolean {
        val guardado = context.wearPinStore.data.first()[keyHash] ?: return false
        return guardado == hash(pin)
    }

    suspend fun clear(context: Context) {
        context.wearPinStore.edit { it.clear() }
    }

    private fun hash(pin: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(pin.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
}
