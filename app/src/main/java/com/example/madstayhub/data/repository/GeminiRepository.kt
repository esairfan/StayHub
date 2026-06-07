package com.example.madstayhub.data.repository

import com.example.madstayhub.BuildConfig
import com.example.madstayhub.data.remote.api.GeminiApiService
import com.example.madstayhub.data.remote.model.GeminiContent
import com.example.madstayhub.data.remote.model.GeminiPart
import com.example.madstayhub.data.remote.model.GeminiRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import org.json.JSONObject
import retrofit2.awaitResponse
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GeminiRepository @Inject constructor(
    private val apiService: GeminiApiService
) {
    private val apiKey = BuildConfig.GEMINI_API_KEY

    fun chat(history: List<GeminiContent>, message: String): Flow<String> = flow {
        val contents = history + GeminiContent(role = "user", parts = listOf(GeminiPart(message)))
        val systemInstruction = GeminiContent(
            parts = listOf(GeminiPart("You are StayHub's virtual butler, Aria. You are professional, warm, and helpful. Assisting guests with hotel amenities like the pool (6AM-10PM) and room service. You MUST ALWAYS reply in English. Under no circumstances should you reply in any language other than English."))
        )
        val request = GeminiRequest(contents, systemInstruction)

        try {
            val response = apiService.streamGenerateContent(apiKey, request).execute()
            if (response.isSuccessful) {
                response.body()?.charStream()?.use { reader ->
                    val jsonReader = com.google.gson.stream.JsonReader(reader)
                    jsonReader.isLenient = true
                    
                    if (jsonReader.peek() == com.google.gson.stream.JsonToken.BEGIN_ARRAY) {
                        jsonReader.beginArray()
                    }
                    
                    while (jsonReader.hasNext() && jsonReader.peek() != com.google.gson.stream.JsonToken.END_ARRAY) {
                        val candidateObj = com.google.gson.JsonParser.parseReader(jsonReader).asJsonObject
                        try {
                            val text = candidateObj
                                .getAsJsonArray("candidates")
                                .get(0).asJsonObject
                                .getAsJsonObject("content")
                                .getAsJsonArray("parts")
                                .get(0).asJsonObject
                                .get("text").asString
                            emit(text)
                        } catch (_: Exception) {
                        }
                    }
                    
                    if (jsonReader.peek() == com.google.gson.stream.JsonToken.END_ARRAY) {
                        jsonReader.endArray()
                    }
                }
            } else {
                emitMockResponse(message)
            }
        } catch (e: Exception) {
            emitMockResponse(message)
        }
    }.flowOn(Dispatchers.IO)

    private suspend fun kotlinx.coroutines.flow.FlowCollector<String>.emitMockResponse(message: String) {
        val fallback = getMockAriaResponse(message)
        val words = fallback.split(" ")
        for (word in words) {
            emit("$word ")
            kotlinx.coroutines.delay(50) // 50ms typewriter speed
        }
    }

    private fun getMockAriaResponse(message: String): String {
        val msg = message.lowercase()
        return when {
            msg.contains("pool") || msg.contains("swim") -> 
                "The StayHub luxury swimming pool is located on the 5th floor. It is open daily for guests from 6:00 AM to 10:00 PM. Clean towels and refreshments are provided at the deck."
            msg.contains("food") || msg.contains("eat") || msg.contains("burger") || msg.contains("order") || msg.contains("menu") || msg.contains("service") -> 
                "Our Room Service is available 24/7. You can browse our menu on your dashboard and place your order directly through the app. Popular items include our Gourmet Burger and Club Sandwich."
            msg.contains("gym") || msg.contains("fitness") || msg.contains("exercise") -> 
                "Our wellness gym and fitness center is located on the 2nd floor and is open 24 hours a day with card key access."
            msg.contains("laundry") || msg.contains("clean") || msg.contains("shirt") || msg.contains("trouser") -> 
                "You can request laundry collection via the Laundry Tracker on your dashboard. Simply input the item count and select 'Request Pickup'."
            msg.contains("wifi") || msg.contains("internet") || msg.contains("password") -> 
                "Complimentary high-speed WiFi is available throughout the hotel. Network: 'StayHub_Guest', Password: 'luxuryresidency'."
            msg.contains("hello") || msg.contains("hi") || msg.contains("hey") || msg.contains("greet") -> 
                "Hello! I am Aria, your virtual butler. I am here to help you make your residency comfortable. How can I assist you today?"
            else -> 
                "I am happy to assist you! As your virtual butler, I can help you with details about the pool, room service menu, gym times, WiFi access, or laundry pickups. What would you like to know?"
        }
    }

    suspend fun getMoodConfig(mood: String): JSONObject? {
        val prompt = "Based on this mood: '$mood', return ONLY a JSON object with: 'lightingColor' (hex), 'tempCelsius' (int 18-26), 'musicGenre' (string), 'dndHours' (int 0-8). No markdown."
        val request = GeminiRequest(listOf(GeminiContent(parts = listOf(GeminiPart(prompt)))))

        return try {
            val response = apiService.generateContent(apiKey, request).awaitResponse()
            if (response.isSuccessful) {
                val text = response.body()?.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""
                JSONObject(text.trim().removeSurrounding("```json", "```"))
            } else null
        } catch (_: Exception) {
            null
        }
    }

    suspend fun summarizeLease(text: String): String {
        val prompt = "Summarize this lease in 3 short bullet points. Focus on rent, duration, and penalty:\n$text"
        val request = GeminiRequest(listOf(GeminiContent(parts = listOf(GeminiPart(prompt)))))

        return try {
            val response = apiService.generateContent(apiKey, request).awaitResponse()
            if (response.isSuccessful) {
                response.body()?.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: "Could not summarize."
            } else "API Error"
        } catch (_: Exception) {
            "Network Error"
        }
    }
}
