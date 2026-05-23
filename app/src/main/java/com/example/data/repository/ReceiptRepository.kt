package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.data.local.ReceiptDao
import com.example.data.local.ReceiptItem
import com.example.data.remote.SyncApiService
import com.example.data.remote.SyncRequest
import com.example.data.remote.SyncRow
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class ReceiptRepository(
    private val receiptDao: ReceiptDao,
    context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences("scanner_prefs", Context.MODE_PRIVATE)

    val allItems: Flow<List<ReceiptItem>> = receiptDao.getAllItemsFlow()
    val unsyncedCount: Flow<Int> = receiptDao.getUnsyncedCount()

    // Get Web App URL
    fun getWebAppUrl(): String {
        return prefs.getString("web_app_url", "") ?: ""
    }

    // Save Web App URL
    fun saveWebAppUrl(url: String) {
        prefs.edit().putString("web_app_url", url).apply()
    }

    suspend fun insertItem(item: ReceiptItem): Long = withContext(Dispatchers.IO) {
        receiptDao.insertItem(item)
    }

    suspend fun insertItems(items: List<ReceiptItem>) = withContext(Dispatchers.IO) {
        receiptDao.insertItems(items)
    }

    suspend fun deleteItem(item: ReceiptItem) = withContext(Dispatchers.IO) {
        receiptDao.deleteItem(item)
    }

    suspend fun findDuplicate(fsNo: String, date: String, itemName: String, totalPrice: Double): ReceiptItem? = withContext(Dispatchers.IO) {
        receiptDao.findDuplicate(fsNo, date, itemName, totalPrice)
    }

    // Sync all pending (unsynced) items
    suspend fun syncPendingItems(): Result<String> = withContext(Dispatchers.IO) {
        val url = getWebAppUrl()
        if (url.isBlank()) {
            return@withContext Result.failure(Exception("Google Apps Script URL is empty. Please configure it in Settings."))
        }

        val unsyncedList = receiptDao.getUnsyncedItems()
        if (unsyncedList.isEmpty()) {
            return@withContext Result.success("All items are already synced.")
        }

        try {
            val rows = unsyncedList.map { item ->
                SyncRow(
                    itemName = item.itemName,
                    quantity = item.quantity,
                    unitPrice = item.unitPrice,
                    tot2Percent = item.tot2Percent,
                    totalPrice = item.totalPrice,
                    fsNo = item.fsNo,
                    date = item.date,
                    rawOcrText = item.rawOcrText,
                    validationStatus = item.validationStatus,
                    savedTimestamp = formatTimestamp(item.savedTimestamp)
                )
            }

            // In-place initialization of Retrofit and OkHttp with generous timeouts
            val okHttpClient = OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build()

            val moshi = Moshi.Builder()
                .add(KotlinJsonAdapterFactory())
                .build()

            val retrofit = Retrofit.Builder()
                .baseUrl("https://dummy.com/") // Ignored as Dynamic @Url is used
                .client(okHttpClient)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()

            val apiService = retrofit.create(SyncApiService::class.java)
            val response = apiService.syncReceipts(url, SyncRequest(rows))

            if (response.isSuccessful) {
                val body = response.body()
                if (body != null && body.success) {
                    // Mark as synced locally
                    unsyncedList.forEach { item ->
                        receiptDao.markSynced(item.id)
                    }
                    Result.success("Successfully synced ${unsyncedList.size} items to Google Sheets.")
                } else {
                    val errMsg = body?.message ?: "Unknown server error from Apps Script"
                    Result.failure(Exception(errMsg))
                }
            } else {
                val errorBody = response.errorBody()?.string() ?: "Response code ${response.code()}"
                Result.failure(Exception("Sync failed: $errorBody"))
            }

        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    // Test URL Connection by sending a dummy row
    suspend fun testConnection(url: String): Result<String> = withContext(Dispatchers.IO) {
        if (url.isBlank()) {
            return@withContext Result.failure(Exception("URL is blank."))
        }
        try {
            val dummyRow = SyncRow(
                itemName = "TEST_CONNECTION",
                quantity = 1.0,
                unitPrice = 0.0,
                tot2Percent = 0.0,
                totalPrice = 0.0,
                fsNo = "TEST",
                date = "01/01/2026",
                rawOcrText = "TEST",
                validationStatus = "VALID",
                savedTimestamp = formatTimestamp(System.currentTimeMillis())
            )

            val okHttpClient = OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .build()

            val moshi = Moshi.Builder()
                .add(KotlinJsonAdapterFactory())
                .build()

            val retrofit = Retrofit.Builder()
                .baseUrl("https://dummy.com/")
                .client(okHttpClient)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()

            val apiService = retrofit.create(SyncApiService::class.java)
            val response = apiService.syncReceipts(url, SyncRequest(listOf(dummyRow)))

            if (response.isSuccessful && response.body()?.success == true) {
                Result.success("Connection test successful! Google Sheet responded successfully.")
            } else {
                val msg = response.body()?.message ?: "HTTP ${response.code()}"
                Result.failure(Exception(msg))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    private fun formatTimestamp(timestamp: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
}
