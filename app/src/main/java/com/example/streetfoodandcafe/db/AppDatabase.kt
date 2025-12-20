package com.example.streetfoodandcafe.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase


// Updated version to 4 (Assuming we are accounting for all changes)
@Database(
    entities = [InventoryEntity::class, CustomerOrderEntity::class, OrderItemEntity::class],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun inventoryDao(): InventoryDao
    abstract fun customerDao(): CustomerDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // --- MIGRATIONS ---

        // Migration from Version 2 to 3: Adding 'customer_name' to 'customer_orders'
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Add the new column with a default value (e.g., empty string or 'Guest')
                db.execSQL("ALTER TABLE customer_orders ADD COLUMN customer_name TEXT NOT NULL DEFAULT ''")
            }
        }

        // Migration from Version 3 to 4: Adding multi-plate fields to 'inventory_table'
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Add is_multi_plate (INTEGER for Boolean, default 0/false)
                db.execSQL("ALTER TABLE inventory_table ADD COLUMN is_multi_plate INTEGER NOT NULL DEFAULT 0")

                // 2. Add full_plate_price (REAL/Double, nullable)
                db.execSQL("ALTER TABLE inventory_table ADD COLUMN full_plate_price REAL DEFAULT NULL")

                // 3. Add half_plate_price (REAL/Double, nullable)
                db.execSQL("ALTER TABLE inventory_table ADD COLUMN half_plate_price REAL DEFAULT NULL")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "street_food_database"
                )
                    // Register the migrations
                    .addMigrations(MIGRATION_2_3, MIGRATION_3_4)

                    // Optional: Keep this for safety in dev if a migration path is missing
                    // .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}