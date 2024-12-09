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
    val price: String
)
