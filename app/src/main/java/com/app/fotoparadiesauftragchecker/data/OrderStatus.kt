package com.app.fotoparadiesauftragchecker.data

import com.google.gson.annotations.SerializedName

data class OrderStatus(
    @SerializedName("orderNo")
    val orderNumber: Int,
    @SerializedName("summaryStateCode")
    val status: String,
    @SerializedName("summaryDate")
    val lastUpdate: String,
    @SerializedName("summaryPriceText")
    val price: String,
    var retailerId: String = "",  // Shop number
    var orderName: String? = null // Custom order name, if set
) {
    companion object {
        const val DEFAULT_CONFIG = 1320
    }
}
