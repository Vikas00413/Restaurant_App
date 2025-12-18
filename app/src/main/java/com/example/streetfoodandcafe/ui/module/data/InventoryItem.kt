package com.example.streetfoodandcafe.ui.module.data

import android.net.Uri
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.ui.graphics.vector.ImageVector

data class InventoryItem(
    val id: Int,
    val foodName: String,
    val price: Double,
    val quantityType: String, // e.g., "Full", "Half"
    val imageVector: ImageVector = Icons.Default.Favorite, // Default placeholder
    val customImageUri: Uri? = null // <--- Add this new field
)
