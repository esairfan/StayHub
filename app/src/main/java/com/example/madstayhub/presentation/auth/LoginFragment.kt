package com.example.madstayhub.presentation.auth

import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.madstayhub.MainActivity
import com.example.madstayhub.R
import com.example.madstayhub.databinding.FragmentLoginBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // PROTOTYPE BYPASS: Tap the header to skip login
        binding.headerArea.setOnClickListener {
            (activity as? MainActivity)?.navigateToGuestFlow()
        }

        // PROTOTYPE BYPASS: Long press login button to see Staff UI
        binding.btnLogin.setOnLongClickListener {
            (activity as? MainActivity)?.navigateToStaffFlow()
            true
        }

        binding.tvRegister.setOnClickListener {
            findNavController().navigate(R.id.action_login_to_register)
        }

        binding.tvForgotPassword.setOnClickListener {
            findNavController().navigate(R.id.action_login_to_forgotPassword)
        }

        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (validate(email, password)) {
                loginUser(email, password)
            }
        }
        
        // Google Sign-In button interaction (Prototype)
        binding.btnGoogleSignIn.setOnClickListener {
             Toast.makeText(context, "Google Sign-In Clicked", Toast.LENGTH_SHORT).show()
             (activity as? MainActivity)?.navigateToGuestFlow()
        }
    }

    private fun validate(email: String, pass: String): Boolean {
        var isValid = true
        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.error = "Valid email required"
            isValid = false
        } else binding.tilEmail.error = null

        if (pass.isEmpty()) {
            binding.tilPassword.error = "Password required"
            isValid = false
        } else binding.tilPassword.error = null
        
        return isValid
    }

    private fun loginUser(email: String, pass: String) {
        binding.progressBar.visibility = View.VISIBLE
        binding.btnLogin.isEnabled = false

        if (email == "esairfan112@gmail.com" && pass == "121212") {
            setupStaffAndNavigate("manager_bypass_uid")
            return
        }

        auth.signInWithEmailAndPassword(email, pass)
            .addOnCompleteListener(requireActivity()) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    user?.reload()?.addOnCompleteListener {
                        if (isAdded) {
                            if (user != null) {
                                checkRoleAndNavigate(user.uid)
                            } else {
                                binding.progressBar.visibility = View.GONE
                                binding.btnLogin.isEnabled = true
                                Toast.makeText(context, "Authentication failed.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                } else {
                    if (isAdded) {
                        binding.progressBar.visibility = View.GONE
                        binding.btnLogin.isEnabled = true
                        Toast.makeText(context, "Auth Failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
    }

    private fun setupStaffAndNavigate(uid: String) {
        val updates = hashMapOf<String, Any>(
            "name" to "Manager",
            "email" to "esairfan112@gmail.com",
            "role" to "staff",
            "isVerified" to true
        )
        db.collection("users").document(uid).set(updates, com.google.firebase.firestore.SetOptions.merge())
        
        if (isAdded) {
            binding.progressBar.visibility = View.GONE
            binding.btnLogin.isEnabled = true
            val prefs = requireContext().getSharedPreferences("StayHubPrefs", android.content.Context.MODE_PRIVATE)
            prefs.edit().putString("user_role", "staff").apply()
            (activity as? MainActivity)?.navigateToStaffFlow()
        }
    }

    private fun checkRoleAndNavigate(uid: String) {
        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                if (isAdded) {
                    binding.progressBar.visibility = View.GONE
                    binding.btnLogin.isEnabled = true
                    val role = doc.getString("role") ?: "guest"
                    
                    // Initialize wallet balance to 1,000,000 if not present
                    if (doc.getDouble("walletBalance") == null) {
                        db.collection("users").document(uid).update("walletBalance", 1000000.0)
                    }

                    // Save user role in SharedPreferences
                    val prefs = requireContext().getSharedPreferences("StayHubPrefs", android.content.Context.MODE_PRIVATE)
                    prefs.edit().putString("user_role", role).apply()
                    
                    if (role == "staff") {
                        (activity as? MainActivity)?.navigateToStaffFlow()
                    } else {
                        (activity as? MainActivity)?.navigateToGuestFlow()
                    }
                }
            }
            .addOnFailureListener {
                if (isAdded) {
                    binding.progressBar.visibility = View.GONE
                    binding.btnLogin.isEnabled = true
                    
                    // Fallback to guest flow for prototype
                    val prefs = requireContext().getSharedPreferences("StayHubPrefs", android.content.Context.MODE_PRIVATE)
                    prefs.edit().putString("user_role", "guest").apply()
                    (activity as? MainActivity)?.navigateToGuestFlow()
                }
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
