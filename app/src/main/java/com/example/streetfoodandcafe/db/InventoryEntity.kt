package com.example.streetfoodandcafe.db



import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "inventory_table")
data class InventoryEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "inventory_id")
    val id: Int = 0, // 0 triggers auto-generation

    @ColumnInfo(name = "food_name")
    val foodName: String,

    // This is the default price (used if multi-plate is disabled)
    @ColumnInfo(name = "price")
    val price: Double,

    // Flag to check if this item has Full/Half variants
    @ColumnInfo(name = "is_multi_plate")
    val isMultiPlate: Boolean = false,

    @ColumnInfo(name = "full_plate_price")
    val fullPlatePrice: Double? = null,

    @ColumnInfo(name = "half_plate_price")
    val halfPlatePrice: Double? = null,

    // We keep quantityType for legacy data or simple labeling if needed,
    // but the new logic relies on the boolean above.
    @ColumnInfo(name = "quantity_type")
    val quantityType: String = "Standard",

    @ColumnInfo(name = "image_uri_string")
    val imageUriString: String? = null
)
