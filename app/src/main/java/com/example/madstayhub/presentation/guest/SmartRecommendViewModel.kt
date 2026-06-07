package com.example.madstayhub.presentation.guest

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.madstayhub.data.repository.GeminiRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SmartRecommendViewModel @Inject constructor(
    private val repository: GeminiRepository
) : ViewModel() {

    private val _recommendations = MutableStateFlow<String>("")
    val recommendations = _recommendations.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    fun fetchRecommendations(city: String) {
        viewModelScope.launch {
            _isLoading.value = true
            // Requesting a summary for recommendations
            val prompt = "Give 5 luxury recommendations for a guest staying in $city. Include one restaurant, one attraction, and one shopping spot. Keep it elegant."
            // For simplicity, we use chat without history for a one-off recommendation or generateContent
            // But let's use a simpler prompt logic in the repository if needed or just chat flow
            var result = ""
            repository.chat(emptyList(), prompt).collect { chunk ->
                result += chunk
                _recommendations.value = result
            }
            _isLoading.value = false
        }
    }
}
