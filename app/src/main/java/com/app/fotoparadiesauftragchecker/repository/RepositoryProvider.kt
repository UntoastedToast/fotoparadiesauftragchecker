package com.app.fotoparadiesauftragchecker.repository

import android.content.Context
import com.app.fotoparadiesauftragchecker.api.FotoparadiesApi
import com.app.fotoparadiesauftragchecker.data.AppDatabase
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Singleton-Klasse für den Zugriff auf Repositories.
 * Später könnte dies durch ein richtiges Dependency Injection Framework wie Hilt ersetzt werden.
 */
object RepositoryProvider {
    private var orderRepository: OrderRepository? = null

    /**
     * Liefert die Instanz des OrderRepository.
     * Erstellt die Instanz bei Bedarf.
     */
    fun getOrderRepository(context: Context): OrderRepository {
        if (orderRepository == null) {
            val database = AppDatabase.getDatabase(context)
            val orderDao = database.orderDao()
            
            val retrofit = Retrofit.Builder()
                .baseUrl(FotoparadiesApi.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
            
            val api = retrofit.create(FotoparadiesApi::class.java)
            
            orderRepository = OrderRepositoryImpl(api, orderDao)
        }
        return orderRepository!!
    }
}