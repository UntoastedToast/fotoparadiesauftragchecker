package com.app.fotoparadiesauftragchecker.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.app.fotoparadiesauftragchecker.api.FotoparadiesApi
import com.app.fotoparadiesauftragchecker.data.AppDatabase
import com.app.fotoparadiesauftragchecker.data.Order
import com.app.fotoparadiesauftragchecker.notification.NotificationService
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class OrderCheckWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val api: FotoparadiesApi
    private val orderDao = AppDatabase.getDatabase(context).orderDao()
    private val notificationService = NotificationService(context)

    init {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://spot.photoprintit.com/spotapi/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        api = retrofit.create(FotoparadiesApi::class.java)
    }

    override suspend fun doWork(): Result {
        return try {
            val orders = orderDao.getAllOrdersSync()
            
            orders.forEach { order ->
                try {
                    val status = api.getOrderStatus(
                        order = order.orderId.toInt(),
                        shop = order.retailerId.toIntOrNull() ?: 1320
                    )

                    if (status != null) {
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
