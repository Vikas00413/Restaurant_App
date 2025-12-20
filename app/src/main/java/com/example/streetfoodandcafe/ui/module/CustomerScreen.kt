package com.example.streetfoodandcafe.ui.module

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.streetfoodandcafe.db.AppDatabase
import com.example.streetfoodandcafe.db.InventoryEntity
import com.example.streetfoodandcafe.db.OrderWithItems
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // --- DATABASE SETUP ---
    val db = remember { AppDatabase.getDatabase(context) }
    val customerDao = remember { db.customerDao() }
    val inventoryDao = remember { db.inventoryDao() }

    // --- STATE ---
    val ordersWithItemsState = customerDao.getAllOrdersWithItems().collectAsState(initial = emptyList())
    // Observe inventory items to map IDs to Names
    val allInventoryItemsState = inventoryDao.getAllInventoryItems().collectAsState(initial = emptyList())

    // Create a Map for quick lookup: ID -> InventoryEntity
    val inventoryMap = remember(allInventoryItemsState.value) {
        allInventoryItemsState.value.associateBy { it.id }
    }

    var showAddOrderSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val orders = ordersWithItemsState.value
    val totalEarnings = orders.sumOf { it.order.totalAmount }

    val groupedOrders = orders.groupBy {
        SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(it.order.orderDate))
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddOrderSheet = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "New Order")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // --- TOTAL EARNINGS HEADER ---
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Total Earnings",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "₹${"%.2f".format(totalEarnings)}",
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            // --- ORDERS LIST ---
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                groupedOrders.forEach { (dateString, ordersForDate) ->
                    item {
                        Text(
                            text = dateString,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                        Divider(modifier = Modifier.padding(vertical = 4.dp))
                    }

                    items(ordersForDate) { orderWithItems ->
                        OrderCard(orderWithItems, inventoryMap)
                    }

                    item {
                        val dailyTotal = ordersForDate.sumOf { it.order.totalAmount }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Text(
                                text = "Daily Total: ₹${"%.2f".format(dailyTotal)}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }

    if (showAddOrderSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAddOrderSheet = false },
            sheetState = sheetState
        ) {
            AddOrderSheet (
                inventoryList = allInventoryItemsState.value,
                onOrderSaved = {
                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                        showAddOrderSheet = false
                    }
                },
                onCancel = {
                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                        showAddOrderSheet = false
                    }
                }
            )
        }
    }
}

@Composable
fun OrderCard(
    orderData: OrderWithItems,
    inventoryMap: Map<Int, InventoryEntity>
) {
    Card(
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Customer Mobile + Name
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = orderData.order.mobileNo,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )

                    if (orderData.order.customerName.isNotBlank() &&
                        orderData.order.customerName != orderData.order.mobileNo &&
                        orderData.order.customerName != "Guest"
                    ) {
                        Text(
                            text = " (${orderData.order.customerName})",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.Normal
                        )
                    }
                }

                // Order Amount
                Text(
                    text = "₹${orderData.order.totalAmount}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // List of items in this order
            Column {
                orderData.items.forEach { item ->
                    val inventoryItem = inventoryMap[item.foodItemId]
                    val foodName = inventoryItem?.foodName ?: "Item #${item.foodItemId}"

                    // Determine variant string (Full/Half)
                    val variantText = if (inventoryItem != null && inventoryItem.isMultiPlate) {
                        // Infer variant based on price stored in order vs inventory price
                        when (item.itemPriceAtTime) {
                            inventoryItem.fullPlatePrice -> "(Full)"
                            inventoryItem.halfPlatePrice -> "(Half)"
                            else -> "" // Price changed or custom
                        }
                    } else {
                        ""
                    }

                    Text(
                        text = "• $foodName $variantText x ${item.quantityCount} (₹${item.itemPriceAtTime})",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}
