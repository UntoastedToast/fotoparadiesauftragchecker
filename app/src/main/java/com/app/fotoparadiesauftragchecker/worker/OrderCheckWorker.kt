package com.app.fotoparadiesauftragchecker.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.app.fotoparadiesauftragchecker.api.ApiClient
import com.app.fotoparadiesauftragchecker.data.AppDatabase
import com.app.fotoparadiesauftragchecker.data.Order
import com.app.fotoparadiesauftragchecker.notification.NotificationService

class OrderCheckWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    // Die zentrale API-Instanz aus ApiClient verwenden
    private val api = ApiClient.fotoparadiesApi
    private val orderDao = AppDatabase.getDatabase(context).orderDao()
    private val notificationService = NotificationService(context)

    override suspend fun doWork(): Result {
        return try {
            val orders = orderDao.getAllOrdersSync()
            
            orders.forEach { order ->
                try {
                    // Status vom API abrufen
                    val status = api.getOrderStatus(
                        order = order.orderId.toInt(),
                        shop = order.retailerId.toIntOrNull() ?: 1320
                    )

                    // Prüfen, ob eine Benachrichtigung gesendet werden soll
                    val shouldNotify = !order.notificationSent && 
                                     order.status != "DELIVERED" && 
                                     status.status == "DELIVERED"

                    orderDao.insertOrder(Order(
                        orderId = order.orderId,
                        retailerId = order.retailerId,
                        status = status.status,
                        orderName = order.orderName,
                        notificationSent = order.notificationSent || 
                            (status.status == "DELIVERED")
                    ))

                    if (shouldNotify) {
                        notificationService.showOrderReadyNotification(
                            order.orderId,
                            order.retailerId
                        )
                    }
                } catch (e: Exception) {
                    // Log error but continue with other orders
                    return@forEach
                }
            }
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}