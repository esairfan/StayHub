package com.example.madstayhub.data.remote.api

import com.example.madstayhub.data.remote.model.GeminiRequest
import com.example.madstayhub.data.remote.model.GeminiResponse
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import retrofit2.http.Streaming

interface GeminiApiService {
    
    @POST("v1beta/models/gemini-2.5-flash:generateContent")
    fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): Call<GeminiResponse>

    @Streaming
    @POST("v1beta/models/gemini-2.5-flash:streamGenerateContent")
    fun streamGenerateContent(
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): Call<ResponseBody>
}
