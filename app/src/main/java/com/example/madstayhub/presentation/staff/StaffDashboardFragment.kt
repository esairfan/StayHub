package com.example.madstayhub.presentation.staff

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.madstayhub.R
import com.example.madstayhub.databinding.FragmentStaffDashboardBinding
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.util.*

class StaffDashboardFragment : Fragment() {

    private var _binding: FragmentStaffDashboardBinding? = null
    private val binding get() = _binding!!
    private val db = FirebaseFirestore.getInstance()

    private var guestsListener: ListenerRegistration? = null
    private var ordersListener: ListenerRegistration? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStaffDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Set up click listeners for staff terminal tiles
        binding.cardKyc.setOnClickListener {
            findNavController().navigate(R.id.action_to_kyc)
        }
        binding.cardGuests.setOnClickListener {
            findNavController().navigate(R.id.action_to_guests)
        }
        binding.cardRequests.setOnClickListener {
            findNavController().navigate(R.id.action_to_requests)
        }
        binding.cardMessages.setOnClickListener {
            findNavController().navigate(R.id.action_to_chatHub)
        }
        binding.cardAmenities.setOnClickListener {
            findNavController().navigate(R.id.action_to_amenities)
        }
        binding.cardHKQueue.setOnClickListener {
            findNavController().navigate(R.id.action_to_hkQueue)
        }
        binding.cardAnalytics.setOnClickListener {
            findNavController().navigate(R.id.action_to_analytics)
        }
        binding.cardAnnouncements.setOnClickListener {
            findNavController().navigate(R.id.action_to_announcements)
        }
        
        binding.fabScanner.setOnClickListener {
            // Direct to KYC screen for convenience in prototype
            findNavController().navigate(R.id.action_to_kyc)
        }

        startLiveStats()
    }

    private fun startLiveStats() {
        // Count verified guests (Guests Arrived)
        guestsListener = db.collection("users")
            .whereEqualTo("role", "guest")
            .whereEqualTo("isVerified", true)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null && isAdded) {
                    val count = snapshot.documents.size
                    binding.tvArrivedGuests.text = count.toString()
                }
            }

        // Sum today's revenue from orders
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOfToday = calendar.time

        ordersListener = db.collection("room_service_orders")
            .whereGreaterThanOrEqualTo("timestamp", startOfToday)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null && isAdded) {
                    var total = 0.0
                    for (doc in snapshot.documents) {
                        total += doc.getDouble("totalAmount") ?: 0.0
                    }
                    binding.tvRevenueToday.text = "Rs. ${total.toInt()}"
                }
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        guestsListener?.remove()
        ordersListener?.remove()
        _binding = null
    }
}
