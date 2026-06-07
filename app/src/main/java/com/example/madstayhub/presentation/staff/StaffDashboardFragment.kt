package com.example.madstayhub.presentation.staff

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
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
    private var sosListener: ListenerRegistration? = null

    // Track simulated email dispatches to prevent duplicate logging on snapshot changes
    private val notifiedEmergencies = mutableSetOf<String>()

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
            findNavController().navigate(R.id.action_to_kyc)
        }

        startLiveStats()
        listenForEmergencies()
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

    private fun listenForEmergencies() {
        sosListener = db.collection("emergencies")
            .whereEqualTo("status", "active")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null || !isAdded) return@addSnapshotListener
                
                val activeAlert = snapshot.documents.firstOrNull()
                if (activeAlert != null) {
                    val id = activeAlert.id
                    val name = activeAlert.getString("userName") ?: "Guest"
                    val room = activeAlert.getString("roomNumber") ?: "N/A"
                    val desc = activeAlert.getString("description") ?: "Emergency Alert Triggered"

                    binding.tvEmergencyText.text = "SOS Alert: Room $room ($name)"
                    binding.cardEmergency.visibility = View.VISIBLE

                    binding.btnRespondSos.setOnClickListener {
                        binding.btnRespondSos.isEnabled = false
                        db.collection("emergencies").document(id)
                            .update("status", "resolved")
                            .addOnSuccessListener {
                                if (isAdded) {
                                    binding.btnRespondSos.isEnabled = true
                                    Toast.makeText(context, "Emergency Resolved", Toast.LENGTH_SHORT).show()
                                }
                            }
                            .addOnFailureListener { e ->
                                if (isAdded) {
                                    binding.btnRespondSos.isEnabled = true
                                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                    }

                    // Trigger simulated email dispatch warning to manager only once
                    if (!notifiedEmergencies.contains(id)) {
                        notifiedEmergencies.add(id)
                        simulateManagerEmailAlert(name, room, desc)
                    }
                } else {
                    binding.cardEmergency.visibility = View.GONE
                }
            }
    }

    private fun simulateManagerEmailAlert(guestName: String, roomNumber: String, desc: String) {
        val emailBody = """
            ================ SIMULATED EMAIL OUTBOX ================
            To: esairfan112@gmail.com
            Subject: CRITICAL SOS ALERT - Room $roomNumber
            
            An emergency alert has been triggered by guest:
            Guest Name: $guestName
            Room Number: $roomNumber
            Details: $desc
            
            Action Required: Please respond immediately.
            =========================================================
        """.trimIndent()
        
        Log.e("SOS_EMAIL_SIMULATOR", emailBody)
        Toast.makeText(context, "Simulated emergency email alert sent to Manager", Toast.LENGTH_LONG).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        guestsListener?.remove()
        ordersListener?.remove()
        sosListener?.remove()
        _binding = null
    }
}
