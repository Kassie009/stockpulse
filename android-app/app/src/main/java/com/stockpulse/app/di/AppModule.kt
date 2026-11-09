package com.stockpulse.app.di

import com.stockpulse.app.data.remote.StockPulseApi
import com.stockpulse.app.data.repository.HealthRepositoryImpl
import com.stockpulse.app.domain.repository.HealthRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/**
 * Modulos Hilt (seccion 3 del prompt: Dependencias con Hilt).
 *
 * NOTA sobre BASE_URL: el emulador Android usa 10.0.2.2 para hablar con
 * el localhost de la PC host. HTTP sin TLS solo en desarrollo; en
 * produccion sera HTTPS (seccion 12 del prompt).
 */
object AppConstants {
    const val BASE_URL = "http://10.0.2.2:3000/"
}

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient =
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()

    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl(AppConstants.BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    @Provides
    @Singleton
    fun provideStockPulseApi(retrofit: Retrofit): StockPulseApi =
        retrofit.create(StockPulseApi::class.java)
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    abstract fun bindHealthRepository(impl: HealthRepositoryImpl): HealthRepository
}
