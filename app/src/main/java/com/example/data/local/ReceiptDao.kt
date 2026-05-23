package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ReceiptDao {
    @Query("SELECT * FROM receipt_items ORDER BY savedTimestamp DESC")
    fun getAllItemsFlow(): Flow<List<ReceiptItem>>

    @Query("SELECT * FROM receipt_items WHERE synced = 0 ORDER BY savedTimestamp ASC")
    suspend fun getUnsyncedItems(): List<ReceiptItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: ReceiptItem): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<ReceiptItem>)

    @Query("UPDATE receipt_items SET synced = 1 WHERE id = :id")
    suspend fun markSynced(id: Int)

    @Delete
    suspend fun deleteItem(item: ReceiptItem)

    @Query("SELECT * FROM receipt_items WHERE fsNo = :fsNo AND date = :date AND itemName = :itemName AND ABS(totalPrice - :totalPrice) < 0.05 LIMIT 1")
    suspend fun findDuplicate(fsNo: String, date: String, itemName: String, totalPrice: Double): ReceiptItem?

    @Query("SELECT COUNT(*) FROM receipt_items WHERE synced = 0")
    fun getUnsyncedCount(): Flow<Int>
}
