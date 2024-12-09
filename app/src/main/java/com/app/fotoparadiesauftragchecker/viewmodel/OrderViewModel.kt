package com.app.fotoparadiesauftragchecker.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.app.fotoparadiesauftragchecker.api.FotoparadiesApi
import com.app.fotoparadiesauftragchecker.data.AppDatabase
import com.app.fotoparadiesauftragchecker.data.Order
import com.app.fotoparadiesauftragchecker.data.OrderStatus
import com.app.fotoparadiesauftragchecker.notification.NotificationService
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class OrderViewModel(application: Application) : AndroidViewModel(application) {
    private val _orders = MutableLiveData<List<OrderStatus>>()
    val orders: LiveData<List<OrderStatus>> = _orders

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    private val api: FotoparadiesApi
    private val orderDao = AppDatabase.getDatabase(application).orderDao()
    private val notificationService = NotificationService(application)

    init {
        val retrofit = Retrofit.Builder()
            .baseUrl(FotoparadiesApi.BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        api = retrofit.create(FotoparadiesApi::class.java)
        loadSavedOrders()
    }

    fun addOrder(shop: Int, order: Int) {
        viewModelScope.launch {
            try {
                val status = api.getOrderStatus(shop = shop, order = order)?.apply {
                    retailerId = shop.toString()
                }
                if (status != null) {
                    val currentOrders = _orders.value.orEmpty().toMutableList()
                    currentOrders.add(0, status)
                    _orders.value = currentOrders

                    // Save to database
                    orderDao.insertOrder(Order(
                        orderId = order.toString(),
                        retailerId = shop.toString(),
                        status = status.status,
                        orderName = status.orderName
                    ))
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Ein Fehler ist aufgetreten"
            }
        }
    }

    fun refreshOrders() {
        viewModelScope.launch {
            try {
                val currentOrders = _orders.value.orEmpty()
                val updatedOrders = currentOrders.map { order ->
                    try {
                        api.getOrderStatus(
                            order = order.orderNumber,
                            shop = order.retailerId.toIntOrNull() ?: 1320
                        )?.apply {
                            retailerId = order.retailerId
                            orderName = order.orderName
                        } ?: order.copy() // Keep existing order on error
                    } catch (e: Exception) {
                        order.copy() // Keep existing order on error
                    }
                }
                _orders.value = updatedOrders

                // Update database and check for notifications
                updatedOrders.forEach { status ->
                    val existingOrder = orderDao.getOrderById(status.orderNumber.toString())
                    val shouldNotify = existingOrder?.let {
                        !it.notificationSent && 
                        it.status != "DELIVERED" && 
                        status.status == "DELIVERED"
                    } ?: false

                    orderDao.insertOrder(Order(
                        orderId = status.orderNumber.toString(),
                        retailerId = status.retailerId,
                        status = status.status,
                        orderName = status.orderName,
                        notificationSent = existingOrder?.notificationSent ?: false || 
                            (status.status == "DELIVERED")
                    ))

                    if (shouldNotify) {
                        notificationService.showOrderReadyNotification(
                            status.orderNumber.toString(),
                            status.retailerId
                        )
                    }
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Ein Fehler ist aufgetreten"
            }
        }
    }

    fun deleteOrder(order: OrderStatus) {
        viewModelScope.launch {
            try {
                // Remove from LiveData
                val currentOrders = _orders.value.orEmpty().toMutableList()
                currentOrders.remove(order)
                _orders.value = currentOrders

                // Remove from database
                orderDao.deleteOrder(Order(
                    orderId = order.orderNumber.toString(),
                    retailerId = order.retailerId,
                    status = order.status,
                    orderName = order.orderName
                ))
            } catch (e: Exception) {
                _error.value = e.message ?: "Fehler beim Löschen des Auftrags"
            }
        }
    }

    private fun loadSavedOrders() {
        viewModelScope.launch {
            orderDao.getAllOrders().collectLatest { savedOrders ->
                val orderStatuses = savedOrders.map { order ->
                    try {
                        api.getOrderStatus(
                            order = order.orderId.toInt(),
                            shop = order.retailerId.toInt()
                        )?.apply {
                            retailerId = order.retailerId
                            orderName = order.orderName
                        } ?: OrderStatus(
                            orderNumber = order.orderId.toInt(),
                            status = "Fehler beim Laden",
                            price = "",
                            lastUpdate = "",
                            retailerId = order.retailerId,
                            orderName = order.orderName
                        )
                    } catch (e: Exception) {
                        OrderStatus(
                            orderNumber = order.orderId.toInt(),
                            status = "Fehler beim Laden",
                            price = "",
                            lastUpdate = "",
                            retailerId = order.retailerId,
                            orderName = order.orderName
                        )
                    }
                }
                _orders.value = orderStatuses
            }
        }
    }
}
