package com.app.fotoparadiesauftragchecker.api

import com.app.fotoparadiesauftragchecker.data.OrderStatus
import retrofit2.http.GET
import retrofit2.http.Query

interface FotoparadiesApi {
    @GET("orderInfo/forShop")
    suspend fun getOrderStatus(
        @Query("config") config: Int = OrderStatus.DEFAULT_CONFIG,
        @Query("shop") shop: Int,
        @Query("order") order: Int
    ): OrderStatus

    companion object {
        const val BASE_URL = "https://spot.photoprintit.com/spotapi/"
    }
}
