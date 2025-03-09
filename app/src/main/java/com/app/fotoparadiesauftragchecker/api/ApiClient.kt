package com.app.fotoparadiesauftragchecker.api

// Wir vermeiden die Abhängigkeit von BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Zentrale Singleton-Klasse für die API-Konfiguration.
 * Stellt eine einheitliche Retrofit-Instanz und API-Service bereit.
 */
object ApiClient {
    private const val TIMEOUT_SECONDS = 30L
    
    // OkHttpClient mit Logging und Timeouts
    private val okHttpClient by lazy {
        OkHttpClient.Builder().apply {
            // Logging immer aktivieren (in Produktionsversionen entfernen oder über Properties steuern)
            val loggingInterceptor = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }
            addInterceptor(loggingInterceptor)
            
            // Timeouts konfigurieren
            connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        }.build()
    }
    
    // Zentrale Retrofit-Instanz
    private val retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(FotoparadiesApi.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
    
    // API-Schnittstelle als Singleton
    val fotoparadiesApi: FotoparadiesApi by lazy {
        retrofit.create(FotoparadiesApi::class.java)
    }
}