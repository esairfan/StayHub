package com.example.madstayhub.presentation.guest

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.madstayhub.databinding.FragmentServiceRequestsHistoryBinding
import com.example.madstayhub.databinding.ItemInvoiceBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class ServiceRequestsHistoryFragment : Fragment() {
    private var _binding: FragmentServiceRequestsHistoryBinding? = null
    private val binding get() = _binding!!

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private lateinit var adapter: RequestHistoryAdapter

    data class RequestHistoryItem(
        val id: String,
        val type: String, // "Housekeeping", "Laundry", "Amenity Booking"
        val title: String,
        val status: String,
        val date: Date?
    )

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentServiceRequestsHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        adapter = RequestHistoryAdapter()
        binding.rvRequests.adapter = adapter

        fetchServiceRequests()
    }

    private fun fetchServiceRequests() {
        val user = auth.currentUser ?: return
        val list = mutableListOf<RequestHistoryItem>()
        var remainingQueries = 3

        fun checkFinished() {
            remainingQueries--
            if (remainingQueries == 0) {
                if (isAdded) {
                    list.sortByDescending { it.date ?: Date(0) }
                    adapter.submitList(list.toList())
                }
            }
        }

        // 1. Fetch Housekeeping
        db.collection("housekeeping_requests")
            .whereEqualTo("guestUid", user.uid)
            .get()
            .addOnSuccessListener { hkSnapshot ->
                for (doc in hkSnapshot.documents) {
                    val id = doc.id.take(8).uppercase()
                    val type = doc.getString("type") ?: "Cleaning"
                    val status = doc.getString("status") ?: "pending"
                    val date = doc.getDate("timestamp") ?: doc.getDate("scheduledTime")
                    val schedDate = doc.getDate("scheduledTime")
                    val timeSdf = SimpleDateFormat("dd MMM hh:mm a", Locale.getDefault())
                    val formattedSched = schedDate?.let { timeSdf.format(it) } ?: ""
                    val title = if (formattedSched.isNotEmpty()) "$type (Scheduled: $formattedSched)" else type
                    list.add(RequestHistoryItem("#HK-$id", "Housekeeping", title, status, date ?: Date()))
                }
                checkFinished()
            }
            .addOnFailureListener { e ->
                checkFinished()
            }

        // 2. Fetch Laundry
        db.collection("laundry_requests")
            .whereEqualTo("guestUid", user.uid)
            .get()
            .addOnSuccessListener { laundrySnapshot ->
                for (doc in laundrySnapshot.documents) {
                    val id = doc.id.take(8).uppercase()
                    val shirts = doc.getLong("shirts") ?: 0
                    val trousers = doc.getLong("trousers") ?: 0
                    val status = doc.getString("status") ?: "pickup_requested"
                    val date = doc.getDate("timestamp")
                    list.add(
                        RequestHistoryItem(
                            "#LD-$id",
                            "Laundry",
                            "$shirts Shirts, $trousers Trousers",
                            status,
                            date ?: Date()
                        )
                    )
                }
                checkFinished()
            }
            .addOnFailureListener {
                checkFinished()
            }

        // 3. Fetch Amenity Bookings
        db.collection("amenity_bookings")
            .whereEqualTo("guestUid", user.uid)
            .get()
            .addOnSuccessListener { amenitySnapshot ->
                for (doc in amenitySnapshot.documents) {
                    val id = doc.id.take(8).uppercase()
                    val name = doc.getString("amenityName") ?: "Amenity"
                    val timeSlot = doc.getString("timeSlot") ?: ""
                    val status = doc.getString("status") ?: "booked"
                    val date = doc.getDate("timestamp")
                    list.add(
                        RequestHistoryItem(
                            "#AM-$id",
                            "Amenity",
                            "$name ($timeSlot)",
                            status,
                            date ?: Date()
                        )
                    )
                }
                checkFinished()
            }
            .addOnFailureListener {
                checkFinished()
            }
    }

    private class RequestHistoryAdapter : ListAdapter<RequestHistoryItem, RequestHistoryAdapter.ViewHolder>(DiffCallback) {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val binding = ItemInvoiceBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(getItem(position))
        }

        class ViewHolder(private val binding: ItemInvoiceBinding) : RecyclerView.ViewHolder(binding.root) {
            fun bind(item: RequestHistoryItem) {
                val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
                val formattedDate = item.date?.let { sdf.format(it) } ?: "Date Unknown"
                
                binding.tvInvoiceTitle.text = "[${item.type}] ${item.title}"
                binding.tvInvoiceId.text = "ID: ${item.id} • $formattedDate"
                
                val displayStatus = item.status.replace("_", " ").replaceFirstChar { it.uppercase() }
                binding.tvInvoiceAmount.text = displayStatus
                
                if (item.status.lowercase() == "delivered" || item.status.lowercase() == "ready" || item.status.lowercase() == "done" || item.status.lowercase() == "completed" || item.status.lowercase() == "booked") {
                    binding.tvInvoiceAmount.setTextColor(android.graphics.Color.parseColor("#4CAF50")) // green
                } else {
                    binding.tvInvoiceAmount.setTextColor(android.graphics.Color.parseColor("#FF9800")) // orange/pending
                }
            }
        }

        object DiffCallback : DiffUtil.ItemCallback<RequestHistoryItem>() {
            override fun areItemsTheSame(oldItem: RequestHistoryItem, newItem: RequestHistoryItem): Boolean = oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: RequestHistoryItem, newItem: RequestHistoryItem): Boolean = oldItem == newItem
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
