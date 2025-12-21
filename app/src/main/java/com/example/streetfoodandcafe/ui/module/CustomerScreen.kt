package com.example.streetfoodandcafe.ui.module

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.streetfoodandcafe.R
import com.example.streetfoodandcafe.db.AppDatabase
import com.example.streetfoodandcafe.db.InventoryEntity
import com.example.streetfoodandcafe.db.OrderWithItems
import com.example.streetfoodandcafe.slip.SlipPrinter
import com.example.streetfoodandcafe.ui.module.data.CartItem // Import CartItem wrapper
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import androidx.core.net.toUri

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

    // --- BLUETOOTH PERMISSION LAUNCHER ---
    // This state holds the action to perform after permissions are granted
    var pendingPrintAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    val bluetoothPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // Check if all required permissions are granted
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            pendingPrintAction?.invoke() // Run the print action
        } else {
            Toast.makeText(context, "Bluetooth permissions are required to print.", Toast.LENGTH_SHORT).show()
        }
        pendingPrintAction = null // Reset
    }
    // Helper function to check and request permissions
    fun checkAndPrint(action: () -> Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val permissionsToRequest = arrayOf(
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN
            )
            // Launch permission request
            pendingPrintAction = action
            bluetoothPermissionLauncher.launch(permissionsToRequest)
        } else {
            // For Android 11 and below, just run the action (basic bluetooth doesn't need runtime permission usually, or location is needed)
            action()
        }
    }
    // --- NEW METHOD: Preview Bill in Browser/Viewer ---
    fun previewBill(
        customerName: String,
        mobileNo: String,
        items: List<CartItem>,
        totalAmount: Double,
        orderId: Long
    ) {
        try {
            val dateFormat = SimpleDateFormat("dd-MMM-yyyy HH:mm", Locale.getDefault())
            val dateStr = dateFormat.format(Date())

            // 1. Build HTML String
            val sb = StringBuilder()
            sb.append("<html><body>")
            sb.append("<div style='text-align:center; font-family: monospace;'>")

            // Header
            sb.append("<h2>STREET FOOD & CAFE</h2>")
            sb.append("<p>Fresh & Tasty</p>")
            sb.append("<hr>")

            // Info
            sb.append("<div style='text-align:left;'>")
            sb.append("<p><b>Order No:</b> #$orderId<br>")
            sb.append("<b>Date:</b> $dateStr<br>")
            sb.append("<b>Name:</b> $customerName<br>")
            if (mobileNo.isNotEmpty()) {
                sb.append("<b>Mobile:</b> $mobileNo</p>")
            }
            sb.append("</div>")

            sb.append("<hr>")

            // Items Table
            sb.append("<table style='width:100%; border-collapse:collapse;'>")
            sb.append("<tr><th style='text-align:left;'>Item</th><th>Qty</th><th style='text-align:right;'>Price</th></tr>")

            for (item in items) {
                val variantSuffix = if (item.variant != "Standard") "(${item.variant})" else ""
                val itemName = "${item.item.foodName} $variantSuffix"
                val qty = item.count.intValue
                val price = item.unitPrice * qty

                sb.append("<tr>")
                sb.append("<td style='text-align:left;'>$itemName</td>")
                sb.append("<td style='text-align:center;'>x$qty</td>")
                sb.append("<td style='text-align:right;'>${"%.2f".format(price)}</td>")
                sb.append("</tr>")
            }
            sb.append("</table>")

            sb.append("<hr>")

            // Total
            sb.append("<h3 style='text-align:right;'>TOTAL: Rs.${"%.2f".format(totalAmount)}</h3>")
            sb.append("<hr>")

            // Footer
            sb.append("<p>Thank you! Visit Again</p>")
            sb.append("</div></body></html>")
            Log.e("Print_text", sb.toString())
            val htmlContent = sb.toString()

            // 2. Encode HTML for Data URI
            val encodedHtml = Uri.encode(htmlContent)

            // 3. Launch Intent
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = "data:text/html;charset=utf-8,$encodedHtml".toUri()
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }

            context.startActivity(intent)

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Error opening preview: ${e.message}", Toast.LENGTH_SHORT).show()
        }
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
            if (orders.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No orders yet. Add one!", color = Color.Gray)
                }
            } else {
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
                            OrderCard(
                                orderData = orderWithItems,
                                inventoryMap = inventoryMap,
                                onPrintClick = {
                                    // --- PRINT LOGIC ---
                                    val cartItemsForPrint = orderWithItems.items.mapNotNull { orderItem ->
                                        val inventoryEntity = inventoryMap[orderItem.foodItemId]
                                        if (inventoryEntity != null) {
                                            val variantName = if (inventoryEntity.isMultiPlate) {
                                                when (orderItem.itemPriceAtTime) {
                                                    inventoryEntity.fullPlatePrice -> "Full"
                                                    inventoryEntity.halfPlatePrice -> "Half"
                                                    else -> "Custom"
                                                }
                                            } else {
                                                "Standard"
                                            }

                                            CartItem(
                                                item = inventoryEntity,
                                                variant = variantName,
                                                unitPrice = orderItem.itemPriceAtTime,
                                                count = mutableIntStateOf(orderItem.quantityCount)
                                            )
                                        } else {
                                            null
                                        }
                                    }
                                    previewBill(
                                        customerName = orderWithItems.order.customerName,
                                        mobileNo = orderWithItems.order.mobileNo,
                                        items = cartItemsForPrint,
                                        totalAmount = orderWithItems.order.totalAmount,
                                        orderId = orderWithItems.order.orderId.toLong()
                                    )

                                  /*  checkAndPrint {
                                        // --- PRINT LOGIC ---
                                        val printerHelper = SlipPrinter(context)

                                        // 1. Reconstruct CartItems from DB Entities
                                        val cartItemsForPrint = orderWithItems.items.mapNotNull { orderItem ->
                                            val inventoryEntity = inventoryMap[orderItem.foodItemId]
                                            if (inventoryEntity != null) {
                                                val variantName = if (inventoryEntity.isMultiPlate) {
                                                    when (orderItem.itemPriceAtTime) {
                                                        inventoryEntity.fullPlatePrice -> "Full"
                                                        inventoryEntity.halfPlatePrice -> "Half"
                                                        else -> "Custom"
                                                    }
                                                } else {
                                                    "Standard"
                                                }

                                                CartItem(
                                                    item = inventoryEntity,
                                                    variant = variantName,
                                                    unitPrice = orderItem.itemPriceAtTime,
                                                    count = mutableIntStateOf(orderItem.quantityCount)
                                                )
                                            } else {
                                                null
                                            }
                                        }

                                        // 2. Call Printer Helper
                                        printerHelper.printBill(
                                            customerName = orderWithItems.order.customerName,
                                            mobileNo = orderWithItems.order.mobileNo,
                                            items = cartItemsForPrint,
                                            totalAmount = orderWithItems.order.totalAmount,
                                            orderId = orderWithItems.order.orderId.toLong()
                                        )
                                    }*/

                                    Toast.makeText(context, "Slip Generated for Order #${orderWithItems.order.orderId}", Toast.LENGTH_SHORT).show()
                                }
                            )
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
    }

    // --- ADD ORDER SHEET ---
    if (showAddOrderSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAddOrderSheet = false },
            sheetState = sheetState
        ) {
            AddOrderSheet(
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
    inventoryMap: Map<Int, InventoryEntity>,
    onPrintClick: () -> Unit // Callback for printing
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f) // Let text take available space
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.width(4.dp))

                    Column {
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
                }

                // Amount + Print Button
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "₹${orderData.order.totalAmount}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    // --- PRINT BUTTON ---
                    IconButton(
                        onClick = onPrintClick,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_print),
                            contentDescription = "Print Slip",
                            tint = Color.DarkGray
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Divider(color = Color.LightGray.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(8.dp))

            // List of items in this order
            Column {
                orderData.items.forEach { item ->
                    val inventoryItem = inventoryMap[item.foodItemId]
                    val foodName = inventoryItem?.foodName ?: "Item #${item.foodItemId}"

                    val variantText = if (inventoryItem != null && inventoryItem.isMultiPlate) {
                        when (item.itemPriceAtTime) {
                            inventoryItem.fullPlatePrice -> "(Full)"
                            inventoryItem.halfPlatePrice -> "(Half)"
                            else -> ""
                        }
                    } else {
                        ""
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "• $foodName $variantText x ${item.quantityCount}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.DarkGray
                        )
                        Text(
                            text = "₹${item.itemPriceAtTime * item.quantityCount}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                }
            }
        }
    }

}
