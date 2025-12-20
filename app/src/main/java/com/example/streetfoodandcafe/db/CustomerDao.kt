package com.example.streetfoodandcafe.db


import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomerDao {// 1. Insert the main order and return the new Order ID
@Insert
suspend fun insertOrder(order: CustomerOrderEntity): Long

    // 2. Insert the list of food items for that order
    @Insert
    suspend fun insertOrderItems(items: List<OrderItemEntity>)

    // 3. Get all orders with their items
    @Transaction
    @Query("SELECT * FROM customer_orders ORDER BY order_date DESC")
    fun getAllOrdersWithItems(): Flow<List<OrderWithItems>>

    // 4. Get orders specific to a mobile number
    @Transaction
    @Query("SELECT * FROM customer_orders WHERE customer_mobile = :mobile ORDER BY order_date DESC")
    fun getOrdersByMobile(mobile: String): Flow<List<OrderWithItems>>
}
