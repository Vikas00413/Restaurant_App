package com.example.streetfoodandcafe.db

import androidx.room.Entity
import androidx.room.PrimaryKey


import androidx.room.ColumnInfo

@Entity(tableName = "customer_orders")
data class CustomerOrderEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "order_id")
    val orderId: Int = 0, // Auto-generated ID for the specific order

    @ColumnInfo(name = "customer_name") // New Field
    val customerName: String,

    @ColumnInfo(name = "customer_mobile")
    val mobileNo: String,

    @ColumnInfo(name = "order_date")
    val orderDate: Long, // Store date as Timestamp (Long)

    @ColumnInfo(name = "total_amount")
    val totalAmount: Double
)
