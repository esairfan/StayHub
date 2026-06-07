package com.example.madstayhub.presentation.auth

import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.madstayhub.R
import com.example.madstayhub.databinding.FragmentRegisterBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class RegisterFragment : Fragment() {

    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        binding.btnRegister.setOnClickListener {
            if (validateInputs()) {
                registerUser()
            }
        }

        binding.tvLogin.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun validateInputs(): Boolean {
        val name = binding.etName.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val phone = binding.etPhone.text.toString().trim()
        val nationality = binding.etNationality.text.toString().trim()
        val idNumber = binding.etIdNumber.text.toString().trim()
        val address = binding.etAddress.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()
        val confirmPassword = binding.etConfirmPassword.text.toString().trim()

        var isValid = true

        if (name.isEmpty()) {
            binding.tilName.error = "Name is required"
            isValid = false
        } else binding.tilName.error = null

        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.error = "Valid email is required"
            isValid = false
        } else binding.tilEmail.error = null

        if (phone.length < 10) {
            binding.tilPhone.error = "Valid phone is required"
            isValid = false
        } else binding.tilPhone.error = null

        if (nationality.isEmpty()) {
            binding.tilNationality.error = "Nationality is required"
            isValid = false
        } else binding.tilNationality.error = null

        if (idNumber.isEmpty()) {
            binding.tilIdNumber.error = "ID Proof is required"
            isValid = false
        } else binding.tilIdNumber.error = null

        if (address.isEmpty()) {
            binding.tilAddress.error = "Address is required"
            isValid = false
        } else binding.tilAddress.error = null

        if (password.length < 6) {
            binding.tilPassword.error = "Min 6 characters"
            isValid = false
        } else binding.tilPassword.error = null

        if (password != confirmPassword) {
            binding.tilConfirmPassword.error = "Passwords do not match"
            isValid = false
        } else binding.tilConfirmPassword.error = null

        return isValid
    }

    private fun registerUser() {
        val name = binding.etName.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val phone = binding.etPhone.text.toString().trim()
        val nationality = binding.etNationality.text.toString().trim()
        val idNumber = binding.etIdNumber.text.toString().trim()
        val address = binding.etAddress.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()

        binding.progressBar.visibility = View.VISIBLE
        binding.btnRegister.isEnabled = false

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(requireActivity()) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    
                    // Send Email Verification
                    // Line 108: Send Email Verification with listener
                    user?.sendEmailVerification()?.addOnCompleteListener { emailTask ->
                        if (emailTask.isSuccessful) {
                            Toast.makeText(context, "Verification email sent to $email", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(context, "Email failed: ${emailTask.exception?.message}", Toast.LENGTH_SHORT).show()
                        }
                    }

                    val uid = user?.uid ?: ""
                    val userMap = hashMapOf(
                        "uid" to uid,
                        "name" to name,
                        "email" to email,
                        "phone" to phone,
                        "nationality" to nationality,
                        "idNumber" to idNumber,
                        "address" to address,
                        "role" to "guest",
                        "roomNumber" to "",
                        "fcmToken" to "",
                        "isVerified" to false,
                        "kycDocUrl" to "",
                        "walletBalance" to 1000000.0
                    )

                    db.collection("users").document(uid).set(userMap)
                        .addOnSuccessListener {
                            if (isAdded) {
                                binding.progressBar.visibility = View.GONE
                                binding.btnRegister.isEnabled = true
                                Toast.makeText(requireContext(), "Account created! Please verify your email.", Toast.LENGTH_LONG).show()
                                auth.signOut()
                                findNavController().popBackStack()
                            }
                        }
                        .addOnFailureListener { e ->
                            if (isAdded) {
                                binding.progressBar.visibility = View.GONE
                                binding.btnRegister.isEnabled = true
                                Toast.makeText(requireContext(), "Profile Error: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                } else {
                    if (isAdded) {
                        binding.progressBar.visibility = View.GONE
                        binding.btnRegister.isEnabled = true
                        Toast.makeText(requireContext(), "Auth Error: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
