package com.app.fotoparadiesauftragchecker.repository

import com.app.fotoparadiesauftragchecker.data.Order
import com.app.fotoparadiesauftragchecker.data.OrderStatus
import kotlinx.coroutines.flow.Flow

/**
 * Repository-Interface für alle Order-bezogenen Operationen.
 * Definiert eine klare Schnittstelle zwischen Datenquellen und ViewModel.
 */
interface OrderRepository {
    /**
     * Gibt einen Flow aller gespeicherten Aufträge zurück.
     */
    fun getAllOrders(): Flow<List<OrderStatus>>
    
    /**
     * Fügt einen neuen Auftrag hinzu oder aktualisiert einen bestehenden.
     * 
     * @param shop Die Filial-ID
     * @param orderNumber Die Auftragsnummer
     * @param orderName Optionaler Name für den Auftrag
     * @return Der hinzugefügte oder aktualisierte Auftrag
     */
    suspend fun addOrder(shop: Int, orderNumber: Int, orderName: String? = null): OrderStatus?
    
    /**
     * Löscht einen Auftrag.
     * 
     * @param order Der zu löschende Auftrag
     */
    suspend fun deleteOrder(order: OrderStatus)
    
    /**
     * Aktualisiert alle Auftragsdaten von der API.
     */
    suspend fun refreshOrders(): List<OrderStatus>
    
    /**
     * Aktualisiert den Status eines lokalen Auftrags (z.B. für Benachrichtigungen).
     */
    suspend fun updateOrderStatus(orderId: String, status: String, notificationSent: Boolean)
    
    /**
     * Gibt einen bestimmten Auftrag anhand seiner ID zurück.
     */
    suspend fun getOrderById(orderId: String): Order?
}