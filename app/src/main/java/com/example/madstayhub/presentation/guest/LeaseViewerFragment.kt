package com.example.madstayhub.presentation.guest

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.madstayhub.databinding.FragmentLeaseViewerBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class LeaseViewerFragment : Fragment() {
    private var _binding: FragmentLeaseViewerBinding? = null
    private val binding get() = _binding!!
    private val viewModel: LeaseViewerViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentLeaseViewerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // For the prototype, we simulate having extracted text from a PDF
        val mockLeaseText = "This lease agreement is for 12 months. Monthly rent is Rs. 45,000. Late payment after 5 days results in a 5% penalty."
        viewModel.summarizeLease(mockLeaseText)

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.summary.collect { summary ->
                if (summary.isNotEmpty()) {
                    binding.tvLeaseSummary.text = summary
                }
            }
        }
        
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isLoading.collect { isLoading ->
                // Show/hide loading indicator if available in layout
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}