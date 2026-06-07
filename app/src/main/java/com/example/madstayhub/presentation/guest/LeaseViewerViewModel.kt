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
class LeaseViewerViewModel @Inject constructor(
    private val repository: GeminiRepository
) : ViewModel() {

    private val _summary = MutableStateFlow<String>("")
    val summary = _summary.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    fun summarizeLease(fullText: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.summarizeLease(fullText)
            _summary.value = result
            _isLoading.value = false
        }
    }
}
