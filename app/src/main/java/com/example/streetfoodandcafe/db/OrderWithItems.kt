package com.example.streetfoodandcafe.db


import androidx.room.Embedded
import androidx.room.Relation

data class OrderWithItems(
    @Embedded val order: CustomerOrderEntity,
    @Relation(
        parentColumn = "order_id",
        entityColumn = "order_owner_id"
    )
    val items: List<OrderItemEntity>
)
