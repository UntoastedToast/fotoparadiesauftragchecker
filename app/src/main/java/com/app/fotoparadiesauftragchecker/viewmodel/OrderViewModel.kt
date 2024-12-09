package com.app.fotoparadiesauftragchecker.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.fotoparadiesauftragchecker.api.FotoparadiesApi
import com.app.fotoparadiesauftragchecker.data.OrderStatus
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class OrderViewModel : ViewModel() {
    private val _orders = MutableLiveData<List<OrderStatus>>(emptyList())
    val orders: LiveData<List<OrderStatus>> = _orders

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    private val api = Retrofit.Builder()
        .baseUrl(FotoparadiesApi.BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(FotoparadiesApi::class.java)

    fun addOrder(shop: Int, order: Int) {
        viewModelScope.launch {
            try {
                val status = api.getOrderStatus(shop = shop, order = order)
                val currentOrders = _orders.value.orEmpty().toMutableList()
                currentOrders.add(0, status)
                _orders.value = currentOrders
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
            } catch (e: Exception) {
                _error.value = e.message ?: "Ein Fehler ist aufgetreten"
            }
        }
    }
}