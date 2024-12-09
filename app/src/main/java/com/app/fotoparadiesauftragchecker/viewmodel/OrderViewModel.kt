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
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class OrderViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val orderDao = database.orderDao()

    private val _orders = MutableLiveData<List<OrderStatus>>(emptyList())
    val orders: LiveData<List<OrderStatus>> = _orders

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    private val api = Retrofit.Builder()
        .baseUrl(FotoparadiesApi.BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(FotoparadiesApi::class.java)

    init {
        loadSavedOrders()
    }

    private fun loadSavedOrders() {
        viewModelScope.launch {
            orderDao.getAllOrders().collectLatest { savedOrders ->
                val orderStatuses = savedOrders.map { order ->
                    try {
                        api.getOrderStatus(order = order.orderId.toInt(), shop = order.retailerId.toInt())
                    } catch (e: Exception) {
                        OrderStatus(
                            orderNumber = order.orderId.toInt(),
                            status = "Fehler beim Laden",
                            price = "",
                            lastUpdate = ""
                        )
                    }
                }
                _orders.value = orderStatuses
            }
        }
    }

    fun addOrder(shop: Int, order: Int) {
        viewModelScope.launch {
            try {
                val status = api.getOrderStatus(shop = shop, order = order)
                val currentOrders = _orders.value.orEmpty().toMutableList()
                currentOrders.add(0, status)
                _orders.value = currentOrders

                // Save to database
                orderDao.insertOrder(Order(
                    orderId = order.toString(),
                    retailerId = shop.toString(),
                    status = status.status
                ))
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
                    api.getOrderStatus(order = order.orderNumber, shop = 1320) // TODO: Store shop number
                }
                _orders.value = updatedOrders

                // Update database
                updatedOrders.forEach { status ->
                    orderDao.insertOrder(Order(
                        orderId = status.orderNumber.toString(),
                        retailerId = "1320", // TODO: Use actual shop number
                        status = status.status
                    ))
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Ein Fehler ist aufgetreten"
            }
        }
    }
}
