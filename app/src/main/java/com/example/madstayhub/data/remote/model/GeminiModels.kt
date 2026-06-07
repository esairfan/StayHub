package com.example.madstayhub.data.remote.model

import com.google.gson.annotations.SerializedName

// Request Models
data class GeminiRequest(
    @SerializedName("contents") val contents: List<GeminiContent>,
    @SerializedName("systemInstruction") val systemInstruction: GeminiContent? = null
)

data class GeminiContent(
    @SerializedName("role") val role: String? = null,
    @SerializedName("parts") val parts: List<GeminiPart>
)

data class GeminiPart(
    @SerializedName("text") val text: String
)

// Response Models
data class GeminiResponse(
    @SerializedName("candidates") val candidates: List<GeminiCandidate>? = null,
    @SerializedName("error") val error: GeminiError? = null
)

data class GeminiCandidate(
    @SerializedName("content") val content: GeminiContent
)

data class GeminiError(
    @SerializedName("message") val message: String,
    @SerializedName("code") val code: Int
)
