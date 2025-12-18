package com.example.streetfoodandcafe.db


import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "inventory_table")
data class InventoryEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "inventory_id")
    val id: Int = 0, // 0 triggers auto-generation

    @ColumnInfo(name = "food_name")
    val foodName: String,

    @ColumnInfo(name = "price")
    val price: Double,

    @ColumnInfo(name = "quantity_type")
    val quantityType: String,

    @ColumnInfo(name = "image_uri_string")
    val imageUriString: String? = null // Room cannot store Uri directly, store as String
)
