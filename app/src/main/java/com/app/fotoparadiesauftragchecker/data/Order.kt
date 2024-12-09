package com.app.fotoparadiesauftragchecker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "orders")
data class Order(
    @PrimaryKey
    val orderId: String,
    val status: String,
    val retailerId: String,
    val orderName: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
