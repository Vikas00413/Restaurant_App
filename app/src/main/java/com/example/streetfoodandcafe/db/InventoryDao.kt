package com.example.streetfoodandcafe.db


import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface InventoryDao {

    // Get all items as a stream of data (Flow updates UI automatically)
    @Query("SELECT * FROM inventory_table ORDER BY inventory_id DESC")
    fun getAllInventoryItems(): Flow<List<InventoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: InventoryEntity)

    @Update
    suspend fun updateItem(item: InventoryEntity)

    @Delete
    suspend fun deleteItem(item: InventoryEntity)
}
