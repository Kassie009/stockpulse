package com.stockpulse.app.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.stockpulse.app.domain.repository.UserSession
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

private val Context.sessionDataStore by preferencesDataStore(name = "stockpulse_session")

/**
 * Almacenamiento local de la sesion (DataStore, seccion 3 del prompt).
 */
@Singleton
class SessionStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val keyAccess = stringPreferencesKey("access_token")
    private val keyRefresh = stringPreferencesKey("refresh_token")
    private val keyUserId = stringPreferencesKey("user_id")
    private val keyEmail = stringPreferencesKey("email")

    val sessionFlow: Flow<UserSession?> = context.sessionDataStore.data.map { prefs ->
        prefs[keyAccess]?.let {
            UserSession(
                accessToken = it,
                refreshToken = prefs[keyRefresh] ?: "",
                userId = prefs[keyUserId] ?: "",
                email = prefs[keyEmail] ?: ""
            )
        }
    }

    suspend fun currentToken(): String? =
        context.sessionDataStore.data.first()[keyAccess]

    /** Version sincrona para usar dentro del interceptor de OkHttp. */
    fun currentTokenBlocking(): String? = runBlocking { currentToken() }

    suspend fun save(session: UserSession) {
        context.sessionDataStore.edit { prefs ->
            prefs[keyAccess] = session.accessToken
            prefs[keyRefresh] = session.refreshToken
            prefs[keyUserId] = session.userId
            prefs[keyEmail] = session.email
        }
    }

    suspend fun clear() {
        context.sessionDataStore.edit { it.clear() }
    }
}
