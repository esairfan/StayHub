package com.example.madstayhub.presentation.guest

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.madstayhub.databinding.FragmentSmartRecommendBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SmartRecommendFragment : Fragment() {
    private var _binding: FragmentSmartRecommendBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SmartRecommendViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSmartRecommendBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Mock city for prototype - usually would come from location or profile
        viewModel.fetchRecommendations("Lahore")

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.recommendations.collect { text ->
                // In a real app, we'd parse this into a list. 
                // For the logic integration, we show the AI response in a text view or simple list.
                // binding.rvRecommendations logic would go here
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}