package com.example.streetfoodandcafe.ui.module

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import coil.compose.rememberAsyncImagePainter
import com.example.streetfoodandcafe.ui.module.data.InventoryItem
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AddItemSheet(
    existingItem: InventoryItem? = null,
    onSaveItem: (name: String, price: Double, isMulti: Boolean, fullPrice: Double?, halfPrice: Double?, imageUri: Uri?) -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current

    // --- State Variables ---
    var name by remember { mutableStateOf(existingItem?.foodName ?: "") }

    // Standard Price (used if Multi-Plate is unchecked)
    var standardPrice by remember { mutableStateOf(existingItem?.price?.toString() ?: "") }

    // Multi-Plate Toggle & Prices
    var isMultiPlate by remember { mutableStateOf(existingItem?.isMultiPlate ?: false) }
    var fullPrice by remember { mutableStateOf(existingItem?.fullPlatePrice?.toString() ?: "") }
    var halfPrice by remember { mutableStateOf(existingItem?.halfPlatePrice?.toString() ?: "") }

    var selectedImageUri by remember { mutableStateOf(existingItem?.customImageUri) }

    // State for the temporary camera file URI
    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }
    var showImageSourceDialog by remember { mutableStateOf(false) }

    // --- Launchers ---
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) selectedImageUri = uri
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            selectedImageUri = tempCameraUri
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            tempCameraUri = createImageFileUri(context)
            tempCameraUri?.let { cameraLauncher.launch(it) }
        } else {
            Toast.makeText(context, "Camera permission required", Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- Dynamic Title ---
        Text(
            text = if (existingItem == null) "Add New Item" else "Edit Item",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        // --- Image Selection Area ---
        Box(
            modifier = Modifier
                .size(150.dp)
                .align(Alignment.CenterHorizontally)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .border(1.dp, Color.Gray, RoundedCornerShape(12.dp))
                .clickable { showImageSourceDialog = true },
            contentAlignment = Alignment.Center
        ) {
            if (selectedImageUri != null) {
                Image(
                    painter = rememberAsyncImagePainter(selectedImageUri),
                    contentDescription = "Selected Image",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Photo",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Tap to add photo",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // --- Food Name (Optional) ---
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Food Name (Optional)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        // --- Multi-Plate Toggle ---
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Checkbox(
                checked = isMultiPlate,
                onCheckedChange = { isMultiPlate = it }
            )
            Text(text = "Enable Full / Half Plate Pricing")
        }

        if (isMultiPlate) {
            // --- FULL PLATE PRICE ---
            OutlinedTextField(
                value = fullPrice,
                onValueChange = { if (it.all { char -> char.isDigit() || char == '.' }) fullPrice = it },
                label = { Text("Full Plate Price") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                prefix = { Text("₹") }
            )

            // --- HALF PLATE PRICE (Optional) ---
            OutlinedTextField(
                value = halfPrice,
                onValueChange = { if (it.all { char -> char.isDigit() || char == '.' }) halfPrice = it },
                label = { Text("Half Plate Price (Optional)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                prefix = { Text("₹") }
            )
        } else {
            // --- STANDARD PRICE ---
            OutlinedTextField(
                value = standardPrice,
                onValueChange = { if (it.all { char -> char.isDigit() || char == '.' }) standardPrice = it },
                label = { Text("Price") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                prefix = { Text("₹") }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // --- Action Buttons ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onCancel) {
                Text("Cancel")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = {
                    val pStandard = standardPrice.toDoubleOrNull() ?: 0.0
                    val pFull = fullPrice.toDoubleOrNull()
                    val pHalf = halfPrice.toDoubleOrNull()

                    // Validation:
                    // 1. If MultiPlate is ON: Full Price must be valid (> 0)
                    // 2. If MultiPlate is OFF: Standard Price must be valid (> 0)
                    val isValid = if (isMultiPlate) (pFull != null && pFull > 0) else (pStandard > 0)

                    if (isValid) {
                        val finalName = if (name.isBlank()) "Unnamed Item" else name

                        // If multiplate is active, we pass 0.0 as standard price fallback
                        val saveStandard = if (isMultiPlate) (pFull ?: 0.0) else pStandard

                        onSaveItem(finalName, saveStandard, isMultiPlate, pFull, pHalf, selectedImageUri)
                    }
                },
                // Basic UI enable check (doesn't block validation inside onClick, but gives visual cue)
                enabled = true
            ) {
                Text(if (existingItem == null) "Add Item" else "Update Item")
            }
        }
    }

    // --- Source Selection Dialog ---
    if (showImageSourceDialog) {
        AlertDialog(
            onDismissRequest = { showImageSourceDialog = false },
            title = { Text("Select Image Source") },
            text = { Text("Choose where to get the food image from.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showImageSourceDialog = false
                        val permissionCheck = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
                            tempCameraUri = createImageFileUri(context)
                            tempCameraUri?.let { cameraLauncher.launch(it) }
                        } else {
                            permissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    }
                ) { Text("Camera") }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showImageSourceDialog = false
                        galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }
                ) { Text("Gallery") }
            }
        )
    }
}

// --- Helper Function ---
fun createImageFileUri(context: Context): Uri {
    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
    val file = File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir)
    val authority = "${context.packageName}.fileprovider"
    return try {
        FileProvider.getUriForFile(context, authority, file)
    } catch (e: IllegalArgumentException) {
        Uri.fromFile(file)
    }
}
