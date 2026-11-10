package com.stockpulse.wear.data

import android.content.Context
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.HttpException
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Repositorio unico del reloj. Se crea una vez y se comparte en la
 * actividad (app pequena, sin Hilt).
 */
class WearRepository(context: Context) {

    private val api: WearApi

    init {
        val authInterceptor = Interceptor { chain ->
            val token = runCatching { WearSession.tokenBlocking(context) }.getOrNull()
            val request = if (token.isNullOrBlank()) {
                chain.request()
            } else {
                chain.request().newBuilder()
                    .addHeader("Authorization", "Bearer $token")
                    .build()
            }
            chain.proceed(request)
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()

        // El emulador del reloj ve el backend del PC via 10.0.2.2
        val retrofit = Retrofit.Builder()
            .baseUrl("http://10.0.2.2:3000/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        api = retrofit.create(WearApi::class.java)
    }

    suspend fun login(email: String, password: String): Result<SessionDto> =
        execute {
            val body = api.login(LoginRequest(email.trim(), password))
            val session = body.body()?.session
            if (body.isSuccessful && session?.accessToken != null) {
                session
            } else {
                throw Exception(body.humanError("No se pudo iniciar sesion"))
            }
        }

    suspend fun summary(): Result<SummaryDto> = execute {
        val body = api.summary()
        val s = body.body()?.summary
        if (body.isSuccessful && s != null) s
        else throw Exception(body.humanError("No se pudo leer tu portafolio"))
    }

    suspend fun alerts(): Result<List<AlertDto>> = execute {
        val body = api.alerts()
        if (body.isSuccessful) body.body()?.alerts.orEmpty()
        else throw Exception(body.humanError("No se pudo leer tus alertas"))
    }

    suspend fun registerDevice(token: String): Result<Unit> = execute {
        val body = api.registerDevice(DeviceRequest(token = token))
        if (body.isSuccessful) Unit
        else throw Exception(body.humanError("No se pudo registrar el reloj"))
    }

    private suspend fun <T> execute(block: suspend () -> T): Result<T> =
        try {
            Result.success(block())
        } catch (err: kotlinx.coroutines.CancellationException) {
            throw err
        } catch (err: Exception) {
            val message = when {
                err is HttpException -> "El servidor respondio ${err.code()}"
                err.message.isNullOrBlank() -> "Sin conexion con el servidor"
                else -> err.message
            }
            Result.failure(Exception(message))
        }
}

private fun Response<*>.humanError(fallback: String): String =
    errorBody()?.string()?.takeIf { it.isNotBlank() }?.let { raw ->
        runCatching {
            com.google.gson.Gson().fromJson(raw, Envelope::class.java).error
        }.getOrNull()
    } ?: fallback

private data class Envelope(val ok: Boolean? = null, val error: String? = null)
