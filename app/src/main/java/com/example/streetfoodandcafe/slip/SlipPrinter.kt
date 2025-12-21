package com.example.streetfoodandcafe.slip

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.ui.text.intl.Locale
import com.dantsu.escposprinter.*
import com.dantsu.escposprinter.connection.bluetooth.BluetoothPrintersConnections
import com.example.streetfoodandcafe.ui.module.data.CartItem
import java.text.SimpleDateFormat
import java.util.Date
import kotlin.text.format

class SlipPrinter(var context: Context) {
    private lateinit var printer: EscPosPrinter

    fun printBill(
        customerName: String,
        mobileNo: String,
        items: List<CartItem>,
        totalAmount: Double,
        orderId: Long
    ) {
        // Step 1: Scan & Connect Bluetooth
        val bluetoothPrinters = BluetoothPrintersConnections().list
        if (bluetoothPrinters == null || bluetoothPrinters.isEmpty()) {
            Toast.makeText(context, "No Bluetooth printers found", Toast.LENGTH_SHORT).show()
            return
        }

        val printerConnection = bluetoothPrinters[0] // First paired printer
        // Note: 203dpi is standard, 48f is width in mm (58mm printer usually prints ~48mm width), 32 is char limit per line
        val printer = EscPosPrinter(printerConnection, 203, 48f, 32)

        try {
            // Step 2: Build Bill Receipt using Actual Data
            val dateFormat = SimpleDateFormat(
                "dd-MMM-yyyy HH:mm",
                java.util.Locale.getDefault()
            )
            val dateStr = dateFormat.format(Date())

            val sb = StringBuilder()

            // -- Header --
            sb.append("[C]<u><b>STREET FOOD & CAFE</b></u>\n")
            sb.append("[C]Fresh & Tasty\n")
            sb.append("[L]\n") // Empty line

            // -- Order Info --
            sb.append("[L]<b>Order No:</b> #$orderId\n")
            sb.append("[L]<b>Date:</b> $dateStr\n")
            sb.append("[L]<b>Name:</b> $customerName\n")
            if (mobileNo.isNotEmpty()) {
                sb.append("[L]<b>Mobile:</b> $mobileNo\n")
            }

            sb.append("[C]--------------------------------\n")
            sb.append("[L]<b>Item</b>[R]<b>Qty</b>[R]<b>Price</b>\n")
            sb.append("[C]--------------------------------\n")

            // -- Items Loop --
            for (item in items) {
                // Determine item name (append variant if not Standard)
                // e.g. "Burger (Full)" or "Pizza"
                val variantSuffix = if (item.variant != "Standard") "(${item.variant})" else ""
                val itemName = "${item.item.foodName} $variantSuffix"

                // Get values
                // Note: CartItem uses MutableIntState for count in your UI, accessing .intValue here
                val qty = item.count.intValue
                val price = item.unitPrice * qty

                // Format: Name on Left, Qty on Right, Price on Right
                // Using [L] for name and [R] for columns is standard in this library
                sb.append("[L]$itemName[R]x$qty[R]${"%.2f".format(price)}\n")
            }

            sb.append("[C]--------------------------------\n")

            // -- Total --
            sb.append("[R]TOTAL: <b>Rs.${"%.2f".format(totalAmount)}</b>\n")
            sb.append("[C]================================\n")

            // -- Footer --
            sb.append("[L]\n")
            sb.append("[C]Thank you! Visit Again\n")

            // Optional: QR Code (if you have payment info)
            // sb.append("[C]<qrcode size='20'>upi://pay?pa=yourupi@bank&pn=StreetFood</qrcode>\n")

            // Step 3: Print
            printer.printFormattedText(sb.toString())
            Log.e("Print_text", sb.toString())
            Toast.makeText(context, "✅ Bill Printed Successfully!", Toast.LENGTH_SHORT).show()

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "❌ Print Error: ${e.message}", Toast.LENGTH_LONG).show()
        } finally {
            // Usually good to disconnect, but if printing frequently, keeping it open might be faster.
            // For this library, usually disconnect is safer.
            // printer.disconnectPrinter()
        }
    }

}


