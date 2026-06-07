package com.example.madstayhub.presentation.guest

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.madstayhub.R
import com.example.madstayhub.databinding.FragmentLaundryTrackerBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class LaundryTrackerFragment : Fragment() {
    private var _binding: FragmentLaundryTrackerBinding? = null
    private val binding get() = _binding!!

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private var shirtsCount = 0
    private var trousersCount = 0
    private var registration: ListenerRegistration? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentLaundryTrackerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnPlusShirts.setOnClickListener {
            shirtsCount++
            binding.tvShirtsCount.text = shirtsCount.toString()
        }

        binding.btnMinusShirts.setOnClickListener {
            if (shirtsCount > 0) {
                shirtsCount--
                binding.tvShirtsCount.text = shirtsCount.toString()
            }
        }

        binding.btnPlusTrousers.setOnClickListener {
            trousersCount++
            binding.tvTrousersCount.text = trousersCount.toString()
        }

        binding.btnMinusTrousers.setOnClickListener {
            if (trousersCount > 0) {
                trousersCount--
                binding.tvTrousersCount.text = trousersCount.toString()
            }
        }

        binding.btnSubmitLaundry.setOnClickListener {
            if (shirtsCount == 0 && trousersCount == 0) {
                Toast.makeText(context, "Please select at least 1 item", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            submitLaundryRequest()
        }

        listenToLaundryStatus()
    }

    private fun submitLaundryRequest() {
        val user = auth.currentUser ?: return
        binding.btnSubmitLaundry.isEnabled = false

        db.collection("users").document(user.uid).get()
            .addOnSuccessListener { doc ->
                val room = doc?.getString("roomNumber") ?: "None"
                val name = doc?.getString("name") ?: "Guest"

                val request = hashMapOf(
                    "guestUid" to user.uid,
                    "guestName" to name,
                    "roomNumber" to room,
                    "shirts" to shirtsCount,
                    "trousers" to trousersCount,
                    "status" to "pickup_requested",
                    "timestamp" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                )

                // Write optimistically
                db.collection("laundry_requests").document().set(request)
                
                if (isAdded) {
                    Toast.makeText(context, "Laundry Pickup Requested!", Toast.LENGTH_SHORT).show()
                    shirtsCount = 0
                    trousersCount = 0
                    binding.tvShirtsCount.text = "0"
                    binding.tvTrousersCount.text = "0"
                    binding.btnSubmitLaundry.isEnabled = true
                }
            }
            .addOnFailureListener { e ->
                if (isAdded) {
                    binding.btnSubmitLaundry.isEnabled = true
                    Toast.makeText(context, "Failed to fetch details: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun listenToLaundryStatus() {
        val user = auth.currentUser ?: return
        registration = db.collection("laundry_requests")
            .whereEqualTo("guestUid", user.uid)
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(1)
            .addSnapshotListener { snapshots, error ->
                if (error != null || snapshots == null || !isAdded) return@addSnapshotListener
                val doc = snapshots.documents.firstOrNull()
                if (doc != null) {
                    val status = doc.getString("status") ?: "pickup_requested"
                    val formattedStatus = when (status) {
                        "pickup_requested" -> "Pickup Requested"
                        "picked_up" -> "Picked Up / In Laundry"
                        "washing" -> "Washing / Cleaning"
                        "ready" -> "Ready for Delivery"
                        "delivered" -> "Delivered"
                        else -> "Pending"
                    }
                    binding.tvLaundryStatus.text = formattedStatus
                } else {
                    binding.tvLaundryStatus.text = "No active requests"
                }
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        registration?.remove()
        _binding = null
    }
}