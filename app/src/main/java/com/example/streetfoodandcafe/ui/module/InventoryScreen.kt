package com.example.streetfoodandcafe.ui.module

import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext

import androidx.compose.ui.unit.dp
import com.example.streetfoodandcafe.db.AppDatabase

import com.example.streetfoodandcafe.db.InventoryEntity
import com.example.streetfoodandcafe.ui.module.data.InventoryItem
import kotlinx.coroutines.launch

// Helper extension to convert Entity to UI Model
fun InventoryEntity.toUiModel(): InventoryItem {
    return InventoryItem(
        id = this.id,
        foodName = this.foodName,
        price = this.price,
        quantityType = this.quantityType,
        customImageUri = this.imageUriString?.let { Uri.parse(it) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // --- DATABASE SETUP ---
    val db = remember { AppDatabase.getDatabase(context) }
    val dao = remember { db.inventoryDao() }

    // State to hold list from DB
    val allItemsState = dao.getAllInventoryItems().collectAsState(initial = emptyList())
    val allItems = allItemsState.value.map { it.toUiModel() }

    var searchQuery by remember { mutableStateOf("") }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var itemToDelete by remember { mutableStateOf<InventoryItem?>(null) }

    // Sheet State
    var showSheet by remember { mutableStateOf(false) }
    var itemToEdit by remember { mutableStateOf<InventoryItem?>(null) }
    val sheetState = rememberModalBottomSheetState()

    // Filter Logic
    val filteredItems = if (searchQuery.isEmpty()) {
        allItems
    } else {
        allItems.filter { it.foodName.contains(searchQuery, ignoreCase = true) }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    itemToEdit = null
                    showSheet = true
                },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Item")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 16.dp)
        ) {
            // --- SEARCH BAR ---
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search food...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // --- INVENTORY LIST ---
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(filteredItems, key = { it.id }) { item ->
                    Column(modifier = Modifier.animateItem()) {
                        InventoryItemCard(
                            item = item,
                            onEditClick = {
                                itemToEdit = item
                                showSheet = true
                            },
                            onDeleteClick = {
                                itemToDelete = item
                                showDeleteDialog = true
                            }
                        )
                    }
                }
            }
        }
    }

    // --- ADD / EDIT ITEM SHEET ---
    if (showSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSheet = false },
            sheetState = sheetState
        ) {
            AddItemSheet(
                existingItem = itemToEdit,
                onSaveItem = { name, price, qty, imageUri ->
                    scope.launch {
                        if (itemToEdit == null) {
                            // --- DB ADD LOGIC ---
                            val newEntity = InventoryEntity(
                                foodName = name,
                                price = price,
                                quantityType = qty,
                                imageUriString = imageUri?.toString()
                            )
                            dao.insertItem(newEntity)
                        } else {
                            // --- DB UPDATE LOGIC ---
                            val updatedEntity = InventoryEntity(
                                id = itemToEdit!!.id, // Use existing ID
                                foodName = name,
                                price = price,
                                quantityType = qty,
                                imageUriString = imageUri?.toString()
                            )
                            dao.updateItem(updatedEntity)
                        }

                        // Close sheet
                        sheetState.hide()
                        showSheet = false
                        itemToEdit = null
                    }
                },
                onCancel = {
                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                        showSheet = false
                        itemToEdit = null
                    }
                }
            )
        }
    }

    // --- DELETE DIALOG ---
    if (showDeleteDialog && itemToDelete != null) {
        DeleteConfirmationDialog(
            itemName = itemToDelete!!.foodName,
            onConfirm = {
                // --- DB DELETE LOGIC ---
                val entityToDelete = InventoryEntity(
                    id = itemToDelete!!.id,
                    foodName = itemToDelete!!.foodName,
                    price = itemToDelete!!.price,
                    quantityType = itemToDelete!!.quantityType,
                    imageUriString = itemToDelete!!.customImageUri?.toString()
                )

                scope.launch {
                    dao.deleteItem(entityToDelete)
                    showDeleteDialog = false
                    itemToDelete = null
                }
            },
            onDismiss = {
                showDeleteDialog = false
                itemToDelete = null
            }
        )
    }
}
