package com.stockpulse.wear.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.core.DataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

private val Context.wearSessionStore by preferencesDataStore(name = "stockpulse_wear_session")

/**
 * Sesion minima del reloj: solo guarda el token de acceso para no
 * volver a escribir el email en la muneca en cada uso.
 */
object WearSession {

    private val keyToken = stringPreferencesKey("access_token")

    suspend fun tokenOrNull(context: Context): String? =
        context.wearSessionStore.data.first()[keyToken]

    /** Version sincrona para usarla dentro del interceptor de OkHttp. */
    fun tokenBlocking(context: Context): String? =
        runBlocking { (context.wearSessionStore as DataStore<Preferences>).data.first()[keyToken] }

    suspend fun save(context: Context, token: String) {
        context.wearSessionStore.edit { prefs -> prefs[keyToken] = token }
    }

    suspend fun clear(context: Context) {
        context.wearSessionStore.edit { it.clear() }
    }
}
