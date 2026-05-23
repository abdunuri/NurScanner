package com.example.data.remote

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SyncRow(
    val itemName: String,
    val quantity: Double,
    val unitPrice: Double,
    val tot2Percent: Double,
    val totalPrice: Double,
    val fsNo: String,
    val date: String,
    val rawOcrText: String,
    val validationStatus: String,
    val savedTimestamp: String
)

@JsonClass(generateAdapter = true)
data class SyncRequest(
    val rows: List<SyncRow>
)

@JsonClass(generateAdapter = true)
data class SyncResponse(
    val success: Boolean,
    val message: String? = null
)
