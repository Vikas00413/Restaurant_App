package com.example.streetfoodandcafe.ui.module.data

import android.net.Uri
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.ui.graphics.vector.ImageVector

data class InventoryItem(
    val id: Int,
    val foodName: String,

    // Standard price (used if isMultiPlate is false)
    val price: Double,

    // New Multi-Plate Logic
    val isMultiPlate: Boolean = false,
    val fullPlatePrice: Double? = null,
    val halfPlatePrice: Double? = null,

    val imageVector: ImageVector = Icons.Default.Favorite, // Default placeholder
    val customImageUri: Uri? = null
)