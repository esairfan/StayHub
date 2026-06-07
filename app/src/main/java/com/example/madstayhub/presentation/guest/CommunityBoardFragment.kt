package com.example.madstayhub.presentation.guest

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.madstayhub.R
import com.example.madstayhub.databinding.FragmentCommunityBoardBinding

class CommunityBoardFragment : Fragment() {
    private var _binding: FragmentCommunityBoardBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCommunityBoardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
        binding.fabAddPost.setOnClickListener {
            findNavController().navigate(R.id.action_community_to_createPost)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}