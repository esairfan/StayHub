package com.example.madstayhub.presentation.guest

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.madstayhub.MainActivity
import com.example.madstayhub.databinding.FragmentAppSettingsBinding
import com.example.madstayhub.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import androidx.navigation.fragment.findNavController

class AppSettingsFragment : Fragment() {
    private var _binding: FragmentAppSettingsBinding? = null
    private val binding get() = _binding!!
    
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private var userListener: ListenerRegistration? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAppSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnLogout.setOnClickListener {
            auth.signOut()
            (activity as? MainActivity)?.logout()
        }

        binding.btnPaymentHistory.setOnClickListener {
            findNavController().navigate(R.id.action_settings_to_paymentHistory)
        }

        binding.btnServiceRequests.setOnClickListener {
            findNavController().navigate(R.id.action_settings_to_serviceRequestsHistory)
        }

        binding.btnAmenitiesHistory.setOnClickListener {
            findNavController().navigate(R.id.action_settings_to_amenitiesHistory)
        }

        // Set initial Dark Mode switch state
        val isNightMode = (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES
        binding.switchDarkMode.setOnCheckedChangeListener(null)
        binding.switchDarkMode.isChecked = isNightMode
        binding.switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES)
            } else {
                androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO)
            }
        }

        fetchProfileDetails()
    }

    private fun fetchProfileDetails() {
        val user = auth.currentUser ?: return
        binding.tvProfileEmail.text = user.email ?: ""

        userListener = db.collection("users").document(user.uid)
            .addSnapshotListener { document, error ->
                if (error != null || document == null || !isAdded) return@addSnapshotListener
                
                val name = document.getString("name") ?: "Guest"
                binding.tvProfileName.text = name

                val isVerified = document.getBoolean("isVerified") ?: false
                if (isVerified) {
                    binding.tvVerificationStatus.text = "Status: Verified"
                    binding.tvVerificationStatus.setTextColor(Color.parseColor("#4CAF50"))
                } else {
                    binding.tvVerificationStatus.text = "Status: Unverified"
                    binding.tvVerificationStatus.setTextColor(Color.parseColor("#D32F2F"))
                }
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        userListener?.remove()
        _binding = null
    }
}