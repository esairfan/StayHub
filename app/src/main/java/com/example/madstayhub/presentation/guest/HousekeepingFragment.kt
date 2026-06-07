package com.example.madstayhub.presentation.guest

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.madstayhub.databinding.FragmentHousekeepingBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class HousekeepingFragment : Fragment() {
    private var _binding: FragmentHousekeepingBinding? = null
    private val binding get() = _binding!!

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val calendar = Calendar.getInstance()
    private var selectedDateTime: Date? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHousekeepingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnPickTime.setOnClickListener {
            showDatePicker()
        }

        binding.btnSubmitHousekeeping.setOnClickListener {
            submitRequest()
        }
    }

    private fun showDatePicker() {
        context?.let { ctx ->
            DatePickerDialog(ctx, { _, year, month, day ->
                calendar.set(Calendar.YEAR, year)
                calendar.set(Calendar.MONTH, month)
                calendar.set(Calendar.DAY_OF_MONTH, day)
                showTimePicker()
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
        }
    }

    private fun showTimePicker() {
        context?.let { ctx ->
            TimePickerDialog(ctx, { _, hour, minute ->
                calendar.set(Calendar.HOUR_OF_DAY, hour)
                calendar.set(Calendar.MINUTE, minute)
                selectedDateTime = calendar.time
                
                val sdf = SimpleDateFormat("EEE, dd MMM yyyy 'at' hh:mm a", Locale.getDefault())
                binding.btnPickTime.text = sdf.format(calendar.time)
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), false).show()
        }
    }

    private fun submitRequest() {
        val user = auth.currentUser ?: return
        val checkedId = binding.rgHousekeeping.checkedRadioButtonId
        if (checkedId == -1) {
            Toast.makeText(context, "Please select a request type", Toast.LENGTH_SHORT).show()
            return
        }
        val radioButton = view?.findViewById<RadioButton>(checkedId)
        val requestType = radioButton?.text?.toString() ?: "General Cleaning"

        if (selectedDateTime == null) {
            Toast.makeText(context, "Please select a preferred date and time", Toast.LENGTH_SHORT).show()
            return
        }

        binding.btnSubmitHousekeeping.isEnabled = false

        db.collection("users").document(user.uid).get()
            .addOnSuccessListener { doc ->
                val room = doc?.getString("roomNumber") ?: "None"
                val name = doc?.getString("name") ?: "Guest"

                val request = hashMapOf(
                    "guestUid" to user.uid,
                    "guestName" to name,
                    "roomNumber" to room,
                    "type" to requestType,
                    "scheduledTime" to selectedDateTime,
                    "status" to "pending",
                    "timestamp" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                )

                // Write optimistically
                db.collection("housekeeping_requests").document().set(request)
                
                if (isAdded) {
                    Toast.makeText(context, "Cleaning Scheduled Successfully!", Toast.LENGTH_SHORT).show()
                    findNavController().navigateUp()
                }
            }
            .addOnFailureListener { e ->
                if (isAdded) {
                    binding.btnSubmitHousekeeping.isEnabled = true
                    Toast.makeText(context, "Failed to fetch details: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}