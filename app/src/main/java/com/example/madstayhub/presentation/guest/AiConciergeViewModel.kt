package com.example.madstayhub.presentation.guest

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.madstayhub.data.remote.model.GeminiContent
import com.example.madstayhub.data.remote.model.GeminiPart
import com.example.madstayhub.data.repository.GeminiRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AiConciergeViewModel @Inject constructor(
    private val repository: GeminiRepository
) : ViewModel() {

    private val _chatHistory = MutableStateFlow<List<GeminiContent>>(emptyList())
    val chatHistory = _chatHistory.asStateFlow()

    private val _streamingResponse = MutableStateFlow("")
    val streamingResponse = _streamingResponse.asStateFlow()

    private val initialChatHistory = listOf(
        GeminiContent(
            role = "user",
            parts = listOf(GeminiPart("Hello"))
        ),
        GeminiContent(
            role = "model",
            parts = listOf(GeminiPart("Hello! I am Aria, your virtual butler. How may I assist you today?"))
        )
    )

    fun sendMessage(userMessage: String) {
        viewModelScope.launch {
            try {
                val currentHistory = _chatHistory.value.toMutableList()
                
                if (currentHistory.isEmpty()) {
                    currentHistory.addAll(initialChatHistory)
                }

                val userContent = GeminiContent(role = "user", parts = listOf(GeminiPart(userMessage)))
                currentHistory.add(userContent)
                _chatHistory.value = currentHistory

                _streamingResponse.value = ""
                var fullResponse = ""
                
                repository.chat(currentHistory.dropLast(1), userMessage).collect { chunk ->
                    fullResponse += chunk
                    _streamingResponse.value = fullResponse
                }

                currentHistory.add(GeminiContent(role = "model", parts = listOf(GeminiPart(fullResponse))))
                _chatHistory.value = currentHistory
                _streamingResponse.value = ""
                
            } catch (e: Exception) {
                _streamingResponse.value = "Sorry, I'm having trouble connecting to my servers right now. Please check your internet."
            }
        }
    }
}
