package com.example.madstayhub.presentation.guest

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.madstayhub.databinding.FragmentMoodRoomControlBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.json.JSONObject

import androidx.navigation.fragment.findNavController

@AndroidEntryPoint
class MoodRoomControlFragment : Fragment() {

    private var _binding: FragmentMoodRoomControlBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MoodRoomControlViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMoodRoomControlBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnSetVibe.setOnClickListener {
            val mood = binding.etMood.text.toString().trim()
            if (mood.isNotEmpty()) {
                viewModel.setMood(mood)
            }
        }

        binding.btnSavePrefs.setOnClickListener {
            viewModel.moodConfig.value?.let { config ->
                viewModel.savePreferences(config)
                Toast.makeText(context, "Preferences Saved!", Toast.LENGTH_SHORT).show()
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isLoading.collect { isLoading ->
                binding.btnSetVibe.isEnabled = !isLoading
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.moodConfig.collect { config ->
                if (config != null) {
                    updateUI(config)
                }
            }
        }
    }

    private fun updateUI(config: JSONObject) {
        binding.cardResult.visibility = View.VISIBLE
        
        val colorHex = config.optString("lightingColor", "#1A237E")
        val temp = config.optInt("tempCelsius", 22)
        val music = config.optString("musicGenre", "Acoustic")
        
        try {
            val color = Color.parseColor(colorHex)
            binding.viewColorPreview.backgroundTintList = ColorStateList.valueOf(color)
            binding.tvColorName.text = "Theme: $colorHex"
        } catch (e: Exception) {
            // Fallback for invalid hex
        }

        binding.chipTemp.text = "${temp}°C"
        binding.chipMusic.text = music
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
