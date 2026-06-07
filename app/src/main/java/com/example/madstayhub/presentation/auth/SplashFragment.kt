package com.example.madstayhub.presentation.auth

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.madstayhub.MainActivity
import com.example.madstayhub.R
import com.example.madstayhub.databinding.FragmentSplashBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SplashFragment : Fragment() {

    private var _binding: FragmentSplashBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSplashBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        Handler(Looper.getMainLooper()).postDelayed({
            if (isAdded) {
                val currentUser = FirebaseAuth.getInstance().currentUser
                if (currentUser != null) {
                    currentUser.reload().addOnCompleteListener {
                        if (isAdded) {
                            checkUserRoleAndNavigate(currentUser.uid)
                        }
                    }
                } else {
                    findNavController().navigate(R.id.action_splash_to_login)
                }
            }
        }, 2000)
    }

    private fun checkUserRoleAndNavigate(uid: String) {
        FirebaseFirestore.getInstance().collection("users").document(uid).get()
            .addOnSuccessListener { document ->
                if (isAdded) {
                    val role = document.getString("role") ?: "guest"
                    if (role == "staff") {
                        (activity as? MainActivity)?.navigateToStaffFlow()
                    } else {
                        (activity as? MainActivity)?.navigateToGuestFlow()
                    }
                }
            }
            .addOnFailureListener {
                if (isAdded) {
                    findNavController().navigate(R.id.action_splash_to_login)
                }
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
