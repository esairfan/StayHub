package com.example.madstayhub.presentation.guest

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.madstayhub.data.repository.GeminiRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import javax.inject.Inject

@HiltViewModel
class MoodRoomControlViewModel @Inject constructor(
    private val repository: GeminiRepository
) : ViewModel() {

    private val _moodConfig = MutableStateFlow<JSONObject?>(null)
    val moodConfig = _moodConfig.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    fun setMood(mood: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val config = repository.getMoodConfig(mood)
            _moodConfig.value = config
            _isLoading.value = false
        }
    }

    fun savePreferences(config: JSONObject) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val prefs = hashMapOf(
            "lightingColor" to config.optString("lightingColor"),
            "tempCelsius" to config.optInt("tempCelsius"),
            "musicGenre" to config.optString("musicGenre"),
            "dndHours" to config.optInt("dndHours")
        )
        FirebaseFirestore.getInstance().collection("users").document(uid)
            .update("roomPreferences", prefs)
    }
}
