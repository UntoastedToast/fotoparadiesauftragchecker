package com.app.fotoparadiesauftragchecker.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface OrderDao {
    @Query("SELECT * FROM orders ORDER BY timestamp DESC")
    fun getAllOrders(): Flow<List<Order>>

    @Query("SELECT * FROM orders WHERE retailerId = :retailerId ORDER BY timestamp DESC")
    fun getOrdersByRetailer(retailerId: String): Flow<List<Order>>

    @Query("SELECT * FROM orders ORDER BY timestamp DESC")
    suspend fun getAllOrdersSync(): List<Order>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrder(order: Order)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrders(orders: List<Order>)

    @Delete
    suspend fun deleteOrder(order: Order)

    @Query("DELETE FROM orders WHERE retailerId = :retailerId")
    suspend fun deleteOrdersByRetailer(retailerId: String)

    @Query("SELECT * FROM orders WHERE orderId = :orderId LIMIT 1")
    suspend fun getOrderById(orderId: String): Order?
}
