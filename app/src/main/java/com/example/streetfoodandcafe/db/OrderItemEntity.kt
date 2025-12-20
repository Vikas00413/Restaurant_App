package com.example.streetfoodandcafe.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "order_items",
    foreignKeys = [
        // Link to the parent Order
        ForeignKey(
            entity = CustomerOrderEntity::class,
            parentColumns = ["order_id"],
            childColumns = ["order_owner_id"],
            onDelete = ForeignKey.CASCADE // If order is deleted, delete these items
        ),
        // Link to the Inventory Item (Food)
        ForeignKey(
            entity = InventoryEntity::class,
            parentColumns = ["inventory_id"],
            childColumns = ["food_item_id"],
            onDelete = ForeignKey.RESTRICT // Don't allow deleting food if it's in an active order
        )
    ]
)
data class OrderItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    @ColumnInfo(name = "order_owner_id")
    val orderOwnerId: Int, // Foreign Key to CustomerOrderEntity

    @ColumnInfo(name = "food_item_id")
    val foodItemId: Int,   // Foreign Key to InventoryEntity

    @ColumnInfo(name = "quantity_count")
    val quantityCount: Int, // How many of this item (e.g., 2 Burgers)

    @ColumnInfo(name = "item_price_at_time")
    val itemPriceAtTime: Double // Store price at time of order (in case menu price changes later)
)
