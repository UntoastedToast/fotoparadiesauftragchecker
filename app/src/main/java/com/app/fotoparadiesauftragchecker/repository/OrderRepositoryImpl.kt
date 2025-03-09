package com.app.fotoparadiesauftragchecker.repository

import com.app.fotoparadiesauftragchecker.api.FotoparadiesApi
import com.app.fotoparadiesauftragchecker.data.AppDatabase
import com.app.fotoparadiesauftragchecker.data.Order
import com.app.fotoparadiesauftragchecker.data.OrderDao
import com.app.fotoparadiesauftragchecker.data.OrderStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * Implementierung des OrderRepository-Interfaces.
 * Diese Klasse koordiniert den Zugriff auf verschiedene Datenquellen (API und lokale Datenbank).
 */
class OrderRepositoryImpl(
    private val api: FotoparadiesApi,
    private val orderDao: OrderDao
) : OrderRepository {

    override fun getAllOrders(): Flow<List<OrderStatus>> {
        return orderDao.getAllOrders()
            .map { orders -> 
                // Konvertiere Order-Objekte zu OrderStatus-Objekten
                orders.map { order -> 
                    fetchOrCreateOrderStatus(order)
                }
            }
            .flowOn(Dispatchers.IO)
    }

    override suspend fun addOrder(shop: Int, orderNumber: Int, orderName: String?): OrderStatus? = 
        withContext(Dispatchers.IO) {
            try {
                // Von API Status abrufen
                val status = api.getOrderStatus(shop = shop, order = orderNumber)?.apply {
                    retailerId = shop.toString()
                    this.orderName = orderName
                }
                
                // In Datenbank speichern
                status?.let {
                    val order = Order(
                        orderId = orderNumber.toString(),
                        retailerId = shop.toString(),
                        status = it.status,
                        orderName = orderName
                    )
                    orderDao.insertOrder(order)
                }
                
                status
            } catch (e: Exception) {
                // Fehlerbehandlung - im echten Code könnte man hier loggen oder einen Fehler zurückgeben
                null
            }
        }

    override suspend fun deleteOrder(order: OrderStatus) = withContext(Dispatchers.IO) {
        try {
            // Aus Datenbank löschen
            orderDao.deleteOrder(
                Order(
                    orderId = order.orderNumber.toString(),
                    retailerId = order.retailerId,
                    status = order.status,
                    orderName = order.orderName
                )
            )
        } catch (e: Exception) {
            // Fehlerbehandlung
        }
    }

    override suspend fun refreshOrders(): List<OrderStatus> = withContext(Dispatchers.IO) {
        try {
            // Alle Aufträge aus der Datenbank holen
            val savedOrders = orderDao.getAllOrdersSync()
            
            // Für jeden Auftrag den aktuellen Status von der API abrufen
            val updatedStatuses = savedOrders.map { order ->
                try {
                    val status = api.getOrderStatus(
                        order = order.orderId.toInt(),
                        shop = order.retailerId.toInt()
                    )?.apply {
                        retailerId = order.retailerId
                        orderName = order.orderName
                    }
                    
                    // In Datenbank aktualisieren
                    status?.let {
                        orderDao.insertOrder(
                            Order(
                                orderId = order.orderId,
                                retailerId = order.retailerId,
                                status = it.status,
                                orderName = order.orderName,
                                timestamp = System.currentTimeMillis(),
                                notificationSent = order.notificationSent || it.status == "DELIVERED"
                            )
                        )
                    }
                    
                    status ?: createErrorOrderStatus(order)
                } catch (e: Exception) {
                    createErrorOrderStatus(order)
                }
            }
            
            updatedStatuses
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun updateOrderStatus(orderId: String, status: String, notificationSent: Boolean) {
        withContext(Dispatchers.IO) {
            val existingOrder = orderDao.getOrderById(orderId)
            existingOrder?.let {
                val updatedOrder = it.copy(
                    status = status,
                    notificationSent = notificationSent
                )
                orderDao.insertOrder(updatedOrder)
            }
        }
    }

    override suspend fun getOrderById(orderId: String): Order? = withContext(Dispatchers.IO) {
        orderDao.getOrderById(orderId)
    }

    /**
     * Hilfsmethode zum Abrufen eines OrderStatus-Objekts aus einem Order-Objekt.
     * Versucht erst, die Daten von der API zu holen, erzeugt sonst ein lokales Objekt.
     */
    private suspend fun fetchOrCreateOrderStatus(order: Order): OrderStatus {
        return try {
            api.getOrderStatus(
                order = order.orderId.toInt(),
                shop = order.retailerId.toInt()
            )?.apply {
                retailerId = order.retailerId
                orderName = order.orderName
            } ?: createErrorOrderStatus(order)
        } catch (e: Exception) {
            createErrorOrderStatus(order)
        }
    }

    /**
     * Hilfsmethode zum Erstellen eines OrderStatus-Objekts im Fehlerfall.
     */
    private fun createErrorOrderStatus(order: Order): OrderStatus {
        return OrderStatus(
            orderNumber = order.orderId.toInt(),
            status = "Fehler beim Laden",
            price = "",
            lastUpdate = "",
            retailerId = order.retailerId,
            orderName = order.orderName
        )
    }
}