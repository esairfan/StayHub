package com.example.madstayhub.presentation.auth

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.madstayhub.MainActivity
import com.example.madstayhub.R
import com.example.madstayhub.databinding.FragmentOtpBinding
import com.google.firebase.auth.FirebaseAuth

class OtpFragment : Fragment() {

    private var _binding: FragmentOtpBinding? = null
    private val binding get() = _binding!!
    private lateinit var auth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOtpBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        auth = FirebaseAuth.getInstance()

        val phoneNumber = arguments?.getString("phoneNumber") ?: "+92 300 1234567"
        binding.tvPhoneNumber.text = getString(R.string.otp_sent_to, phoneNumber)

        binding.btnVerify.setOnClickListener {
            val code = binding.etOTP.text.toString().trim()
            if (code.length == 6) {
                Toast.makeText(context, "Verification Successful", Toast.LENGTH_SHORT).show()
                (activity as? MainActivity)?.navigateToGuestFlow()
            } else {
                binding.tilOTP.error = getString(R.string.verify_otp)
            }
        }
        
        binding.tvResend.setOnClickListener {
            Toast.makeText(context, "OTP Resent", Toast.LENGTH_SHORT).show()
            sendVerificationCode(phoneNumber)
        }
        
        sendVerificationCode(phoneNumber)
    }

    private fun sendVerificationCode(phone: String) {
        binding.progressBar.visibility = View.VISIBLE
        Handler(Looper.getMainLooper()).postDelayed({
            if (isAdded) {
                binding.progressBar.visibility = View.GONE
                Toast.makeText(context, "OTP Sent to $phone", Toast.LENGTH_SHORT).show()
            }
        }, 1000)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
