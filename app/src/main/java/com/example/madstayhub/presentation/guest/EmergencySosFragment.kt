package com.example.madstayhub.presentation.guest

import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.madstayhub.databinding.FragmentEmergencySosBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class EmergencySosFragment : Fragment() {
    private var _binding: FragmentEmergencySosBinding? = null
    private val binding get() = _binding!!
    
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private var countDownTimer: CountDownTimer? = null
    private var isSending = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentEmergencySosBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnSosBig.setOnClickListener {
            if (!isSending) {
                startSosCountdown()
            }
        }

        binding.btnCancelSos.setOnClickListener {
            cancelSosCountdown()
        }
    }

    private fun startSosCountdown() {
        isSending = true
        binding.tvCountdown.visibility = View.VISIBLE
        binding.btnCancelSos.visibility = View.VISIBLE
        binding.tvWarning.text = "SOS Triggered. Keep calm."

        countDownTimer = object : CountDownTimer(5000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val secondsRemaining = (millisUntilFinished / 1000) + 1
                binding.tvCountdown.text = "Sending alert in $secondsRemaining..."
            }

            override fun onFinish() {
                sendSosAlert()
            }
        }.start()
    }

    private fun cancelSosCountdown() {
        countDownTimer?.cancel()
        isSending = false
        binding.tvCountdown.visibility = View.GONE
        binding.btnCancelSos.visibility = View.GONE
        binding.tvWarning.text = "Press and hold the button below to send an emergency alert to the front desk and security."
        Toast.makeText(context, "SOS Alert Cancelled", Toast.LENGTH_SHORT).show()
    }

    private fun sendSosAlert() {
        val user = auth.currentUser ?: return
        
        db.collection("users").document(user.uid).get()
            .addOnSuccessListener { doc ->
                val roomNumber = doc?.getString("roomNumber") ?: "None"
                val name = doc?.getString("name") ?: "Guest"
                
                val sosAlert = hashMapOf(
                    "guestUid" to user.uid,
                    "guestName" to name,
                    "roomNumber" to roomNumber,
                    "timestamp" to FieldValue.serverTimestamp(),
                    "status" to "active"
                )

                db.collection("emergencies").add(sosAlert)
                    .addOnSuccessListener {
                        if (isAdded) {
                            binding.tvCountdown.text = "ALERT SENT!"
                            binding.btnCancelSos.visibility = View.GONE
                            Toast.makeText(context, "Emergency SOS sent to front desk!", Toast.LENGTH_LONG).show()
                        }
                    }
                    .addOnFailureListener { e ->
                        if (isAdded) {
                            Toast.makeText(context, "Failed to send alert: ${e.message}", Toast.LENGTH_SHORT).show()
                            cancelSosCountdown()
                        }
                    }
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        countDownTimer?.cancel()
        _binding = null
    }
}