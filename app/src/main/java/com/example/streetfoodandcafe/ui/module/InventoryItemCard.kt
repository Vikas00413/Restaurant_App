package com.example.streetfoodandcafe.ui.module

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.example.streetfoodandcafe.ui.module.data.InventoryItem

@Composable
fun InventoryItemCard(
    item: InventoryItem,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp), // Slightly lower elevation for cleaner look
        modifier = Modifier
            .fillMaxWidth()
            .height(110.dp), // Fixed compact height for the card
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(12.dp) // Defined shape for the card
    ) {
        Row(
            modifier = Modifier.fillMaxSize(), // Row fills the card
            verticalAlignment = Alignment.CenterVertically
        ) {
            // --- 1. IMAGE (Left Side - Full Height) ---
            Box(
                modifier = Modifier
                    .width(110.dp) // Square ratio based on height
                    .fillMaxHeight()
                    // Only round the top-left and bottom-left to match card
                    .clip(RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp))
                    .background(Color.Gray.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                if (item.customImageUri != null) {
                    Image(
                        painter = rememberAsyncImagePainter(item.customImageUri),
                        contentDescription = item.foodName,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop // Ensures image covers the full box area
                    )
                } else {
                    Image(
                        imageVector = item.imageVector,
                        contentDescription = item.foodName,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }

            // --- 2. DETAILS (Middle) ---
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 8.dp, horizontal = 12.dp) // Add padding here since we removed it from Row
            ) {
                Text(
                    text = item.foodName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1 // Prevent overflow
                )

                Spacer(modifier = Modifier.height(4.dp))

                if (item.isMultiPlate) {
                    // Show Full and Half prices
                    item.fullPlatePrice?.let {
                        Text(
                            text = "Full: ₹$it",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    item.halfPlatePrice?.let {
                        Text(
                            text = "Half: ₹$it",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.Normal
                        )
                    }
                } else {
                    // Standard Price
                    Text(
                        text = "Price: ₹${item.price}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // --- 3. ACTION BUTTONS (Right Side) ---
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(end = 4.dp), // Minimal padding on the right edge
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Center
            ) {
                IconButton(onClick = onEditClick) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp) // Slightly smaller icons
                    )
                }
                IconButton(onClick = onDeleteClick) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
