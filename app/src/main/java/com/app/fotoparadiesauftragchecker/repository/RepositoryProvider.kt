package com.app.fotoparadiesauftragchecker.repository

import android.content.Context
import com.app.fotoparadiesauftragchecker.api.ApiClient
import com.app.fotoparadiesauftragchecker.data.AppDatabase

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
            
            // Die zentrale API-Instanz aus ApiClient verwenden
            val api = ApiClient.fotoparadiesApi
            
            orderRepository = OrderRepositoryImpl(api, orderDao)
        }
        return orderRepository!!
    }
}