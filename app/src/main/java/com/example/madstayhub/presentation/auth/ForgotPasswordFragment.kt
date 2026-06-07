package com.example.madstayhub.presentation.auth

import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.madstayhub.databinding.FragmentForgotPasswordBinding
import com.google.firebase.auth.FirebaseAuth

class ForgotPasswordFragment : Fragment() {

    private var _binding: FragmentForgotPasswordBinding? = null
    private val binding get() = _binding!!
    private lateinit var auth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentForgotPasswordBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        auth = FirebaseAuth.getInstance()

        binding.btnReset.setOnClickListener {
            val email = binding.etResetEmail.text.toString().trim()
            if (validateEmail(email)) {
                sendResetLink(email)
            }
        }

        binding.tvBackToLogin.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun validateEmail(email: String): Boolean {
        return if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilResetEmail.error = "Enter a valid registered email"
            false
        } else {
            binding.tilResetEmail.error = null
            true
        }
    }

    private fun sendResetLink(email: String) {
        binding.progressBar.visibility = View.VISIBLE
        binding.btnReset.isEnabled = false
        
        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                if (isAdded) {
                    binding.progressBar.visibility = View.GONE
                    binding.btnReset.isEnabled = true
                    if (task.isSuccessful) {
                        Toast.makeText(context, "Reset link sent to $email", Toast.LENGTH_LONG).show()
                        findNavController().popBackStack()
                    } else {
                        Toast.makeText(context, "Error: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
