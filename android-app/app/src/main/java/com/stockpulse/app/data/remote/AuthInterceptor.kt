package com.stockpulse.app.data.remote

import com.stockpulse.app.data.local.SessionStore
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

/**
 * Adjunta el token de sesion (Supabase JWT) a cada peticion al backend.
 * El backend se apoya en ese token para autorizar y aplicar RLS.
 */
class AuthInterceptor @Inject constructor(
    private val sessionStore: SessionStore
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val token = sessionStore.currentTokenBlocking()
        val request = if (!token.isNullOrEmpty()) {
            chain.request().newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .build()
        } else {
            chain.request()
        }
        return chain.proceed(request)
    }
}
