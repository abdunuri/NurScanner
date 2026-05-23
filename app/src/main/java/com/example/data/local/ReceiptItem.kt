package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "receipt_items")
data class ReceiptItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val itemName: String,
    val quantity: Double,
    val unitPrice: Double,
    val baseAmount: Double,
    val tot2Percent: Double,
    val totalPrice: Double,
    val fsNo: String,
    val date: String,
    val rawOcrText: String,
    val validationStatus: String,
    val savedTimestamp: Long,
    val synced: Boolean = false
)
