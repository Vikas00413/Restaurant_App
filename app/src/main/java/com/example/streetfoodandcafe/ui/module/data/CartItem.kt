package com.example.streetfoodandcafe.ui.module.data

import com.example.streetfoodandcafe.db.InventoryEntity

data class CartItem(
    val item: InventoryEntity,
    val variant: String, // "Standard", "Full", or "Half"
    val unitPrice: Double,
    var count: Int
)