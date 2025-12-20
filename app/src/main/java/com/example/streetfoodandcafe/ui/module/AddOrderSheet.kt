package com.example.streetfoodandcafe.ui.module


import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.streetfoodandcafe.db.AppDatabase
import com.example.streetfoodandcafe.db.CustomerOrderEntity
import com.example.streetfoodandcafe.db.InventoryEntity
import com.example.streetfoodandcafe.db.OrderItemEntity
import com.example.streetfoodandcafe.ui.module.data.CartItem
import kotlinx.coroutines.launch


@Composable
fun AddOrderSheet(
    inventoryList: List<InventoryEntity>,
    onOrderSaved: () -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val db = remember { AppDatabase.getDatabase(context) }
    val customerDao = remember { db.customerDao() }

    // --- State ---
    var customerName by remember { mutableStateOf("") }
    var mobileNo by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }

    // The Cart List
    val cartItems = remember { mutableStateListOf<CartItem>() }

    // Dialog State for Multi-Plate Selection
    var showVariantDialog by remember { mutableStateOf(false) }
    var selectedMultiPlateItem by remember { mutableStateOf<InventoryEntity?>(null) }

    // Filter Logic for Search
    val filteredInventory = if (searchQuery.isEmpty()) {
        inventoryList
    } else {
        inventoryList.filter { it.foodName.contains(searchQuery, ignoreCase = true) }
    }

    // Calculate Total (Accessing .intValue)
    val currentTotal = cartItems.sumOf { it.unitPrice * it.count.intValue }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // --- Header ---
        Text(
            text = "Create New Order",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))

        // --- Customer Details ---
        OutlinedTextField(
            value = mobileNo,
            onValueChange = { if (it.length <= 10 && it.all { c -> c.isDigit() }) mobileNo = it },
            label = { Text("Mobile Number") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = customerName,
            onValueChange = { customerName = it },
            label = { Text("Customer Name (Optional)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // --- Search Bar ---
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search Food Items...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // --- Content Area (Menu + Cart) ---
        Column(modifier = Modifier.weight(1f)) {

            // 1. MENU LIST
            Text("Menu", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .border(1.dp, Color.LightGray.copy(alpha = 0.5f), RoundedCornerShape(8.dp)),
                contentPadding = PaddingValues(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredInventory) { item ->
                    InventorySelectionCard(
                        item = item,
                        onAddClick = {
                            if (item.isMultiPlate) {
                                selectedMultiPlateItem = item
                                showVariantDialog = true
                            } else {
                                addToCart(cartItems, item, "Standard", item.price)
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 2. CART LIST
            if (cartItems.isNotEmpty()) {
                Text("Your Cart", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                LazyColumn(
                    modifier = Modifier.weight(0.8f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 8.dp)
                ) {
                    items(cartItems) { cartItem ->
                        CartItemCard(
                            cartItem = cartItem,
                            // LOGIC: Increase count
                            onIncrease = { cartItem.count.intValue++ },
                            // LOGIC: Decrease count or remove
                            onDecrease = {
                                if (cartItem.count.intValue > 1) {
                                    cartItem.count.intValue--
                                } else {
                                    cartItems.remove(cartItem)
                                }
                            }
                        )
                    }
                }
            }
        }

        Divider(modifier = Modifier.padding(vertical = 8.dp))

        // --- Bottom Actions ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Total", style = MaterialTheme.typography.bodySmall)
                Text(
                    text = "₹${currentTotal}",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Row {
                TextButton(onClick = onCancel) { Text("Cancel") }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        if (mobileNo.isNotEmpty() && cartItems.isNotEmpty()) {
                            scope.launch {
                                val finalName = customerName.ifBlank { mobileNo }

                                // 1. Save Order
                                val orderId = customerDao.insertOrder(
                                    CustomerOrderEntity(
                                        customerName = finalName,
                                        mobileNo = mobileNo,
                                        orderDate = System.currentTimeMillis(),
                                        totalAmount = currentTotal
                                    )
                                )

                                // 2. Save Items
                                val orderItemsEntities = cartItems.map { cItem ->
                                    OrderItemEntity(
                                        orderOwnerId = orderId.toInt(),
                                        foodItemId = cItem.item.id,
                                        quantityCount = cItem.count.intValue, // Access intValue
                                        itemPriceAtTime = cItem.unitPrice
                                    )
                                }
                                customerDao.insertOrderItems(orderItemsEntities)
                                onOrderSaved()
                            }
                        }
                    },
                    enabled = mobileNo.length == 10 && cartItems.isNotEmpty()
                ) {
                    Text("Save Order")
                }
            }
        }
    }


    // --- VARIANT SELECTION POPUP ---
    if (showVariantDialog && selectedMultiPlateItem != null) {
        val item = selectedMultiPlateItem!!
        AlertDialog(
            onDismissRequest = {
                showVariantDialog = false
                selectedMultiPlateItem = null
            },
            title = { Text("Select Portion for ${item.foodName}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    item.fullPlatePrice?.let { price ->
                        Button(
                            onClick = {
                                addToCart(cartItems, item, "Full", price)
                                showVariantDialog = false
                                selectedMultiPlateItem = null
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Full Plate")
                                Text("₹$price")
                            }
                        }
                    }

                    item.halfPlatePrice?.let { price ->
                        Button(
                            onClick = {
                                addToCart(cartItems, item, "Half", price)
                                showVariantDialog = false
                                selectedMultiPlateItem = null
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                        ) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Half Plate")
                                Text("₹$price")
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = {
                    showVariantDialog = false
                    selectedMultiPlateItem = null
                }) { Text("Cancel") }
            }
        )
    }
}

// --- Helper: Add to Cart Logic ---
fun addToCart(
    cartList: MutableList<CartItem>,
    item: InventoryEntity,
    variant: String,
    price: Double
) {
    val existing = cartList.find { it.item.id == item.id && it.variant == variant }
    if (existing != null) {
        existing.count.intValue++ // Access intValue
    } else {
        cartList.add(CartItem(item, variant, price)) // Init with default 1
    }
}

// --- UI Components for List ---

@Composable
fun InventorySelectionCard(item: InventoryEntity, onAddClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(item.foodName, fontWeight = FontWeight.Bold)
                if (item.isMultiPlate) {
                    Text("Multi-Plate Available", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                } else {
                    Text("₹${item.price}", style = MaterialTheme.typography.bodySmall)
                }
            }
            Button(onClick = onAddClick, modifier = Modifier.height(36.dp)) {
                Icon(Icons.Default.Add, contentDescription = "Add")
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add")
            }
        }
    }
}

@Composable
fun CartItemCard(cartItem: CartItem, onIncrease: () -> Unit, onDecrease: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${cartItem.item.foodName} ${if(cartItem.variant != "Standard") "(${cartItem.variant})" else ""}",
                    fontWeight = FontWeight.SemiBold
                )
                Text("₹${cartItem.unitPrice}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // DELETE / DECREASE BUTTON
                IconButton(onClick = onDecrease, modifier = Modifier.size(28.dp)) {
                    // Visual logic: if count is 1, show delete icon, else show remove (minus) icon
                    if (cartItem.count.intValue == 1) {
                        Icon(Icons.Default.Delete, "Remove", tint = MaterialTheme.colorScheme.error)
                    } else {
                        Icon(Icons.Default.Delete, "Decrease")
                    }
                }

                // COUNT DISPLAY
                Text(
                    text = "${cartItem.count.intValue}", // Access intValue
                    modifier = Modifier.padding(horizontal = 8.dp),
                    fontWeight = FontWeight.Bold
                )

                // ADD / INCREASE BUTTON
                IconButton(onClick = onIncrease, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Add, "Increase")
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = "₹${cartItem.unitPrice * cartItem.count.intValue}", // Access intValue
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
