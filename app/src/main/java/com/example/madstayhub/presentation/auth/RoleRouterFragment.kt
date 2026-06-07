package com.example.madstayhub.presentation.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.madstayhub.MainActivity
import com.example.madstayhub.databinding.FragmentRoleRouterBinding

class RoleRouterFragment : Fragment() {

    private var _binding: FragmentRoleRouterBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRoleRouterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.cardGuest.setOnClickListener {
            (activity as? MainActivity)?.navigateToGuestFlow()
        }

        binding.cardStaff.setOnClickListener {
            (activity as? MainActivity)?.navigateToStaffFlow()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
