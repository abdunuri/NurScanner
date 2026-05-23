package com.example.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Url

interface SyncApiService {
    @POST
    suspend fun syncReceipts(
        @Url url: String,
        @Body request: SyncRequest
    ): Response<SyncResponse>
}
