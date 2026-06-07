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
import com.example.madstayhub.databinding.FragmentAmenitiesHistoryBinding
import com.example.madstayhub.databinding.ItemInvoiceBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class AmenitiesHistoryFragment : Fragment() {
    private var _binding: FragmentAmenitiesHistoryBinding? = null
    private val binding get() = _binding!!

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private lateinit var adapter: AmenitiesHistoryAdapter

    data class AmenityBookingRecord(
        val id: String,
        val amenityName: String,
        val timeSlot: String,
        val date: String,
        val status: String,
        val timestamp: Date?
    )

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAmenitiesHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        adapter = AmenitiesHistoryAdapter()
        binding.rvAmenitiesHistory.adapter = adapter

        fetchAmenityBookings()
    }

    private fun fetchAmenityBookings() {
        val user = auth.currentUser ?: return
        
        db.collection("amenity_bookings")
            .whereEqualTo("guestUid", user.uid)
            .get()
            .addOnSuccessListener { snapshot ->
                val list = mutableListOf<AmenityBookingRecord>()
                for (doc in snapshot.documents) {
                    val id = doc.id.take(8).uppercase()
                    val name = doc.getString("amenityName") ?: "Amenity"
                    val slot = doc.getString("timeSlot") ?: ""
                    val date = doc.getString("date") ?: ""
                    val status = doc.getString("status") ?: "booked"
                    val timestamp = doc.getDate("timestamp")
                    
                    list.add(AmenityBookingRecord("#AM-$id", name, slot, date, status, timestamp))
                }
                
                // Sort by timestamp descending, fallback to epoch if null
                list.sortByDescending { it.timestamp ?: Date(0) }
                adapter.submitList(list)
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Error fetching bookings: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private class AmenitiesHistoryAdapter : ListAdapter<AmenityBookingRecord, AmenitiesHistoryAdapter.ViewHolder>(DiffCallback) {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val binding = ItemInvoiceBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(getItem(position))
        }

        class ViewHolder(private val binding: ItemInvoiceBinding) : RecyclerView.ViewHolder(binding.root) {
            fun bind(record: AmenityBookingRecord) {
                // Display: [Amenity] Pool
                binding.tvInvoiceTitle.text = "[Amenity] ${record.amenityName}"
                
                // Subtext: Date & Slot
                val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
                val formattedRequestedDate = record.timestamp?.let { sdf.format(it) } ?: "Date Unknown"
                binding.tvInvoiceId.text = "Slot: ${record.date} (${record.timeSlot})\nRequested: $formattedRequestedDate"
                
                // Status
                val displayStatus = record.status.replace("_", " ").replaceFirstChar { it.uppercase() }
                binding.tvInvoiceAmount.text = displayStatus
                
                if (record.status.lowercase() == "booked" || record.status.lowercase() == "completed" || record.status.lowercase() == "approved") {
                    binding.tvInvoiceAmount.setTextColor(android.graphics.Color.parseColor("#4CAF50")) // green
                } else {
                    binding.tvInvoiceAmount.setTextColor(android.graphics.Color.parseColor("#FF9800")) // orange/pending
                }
            }
        }

        object DiffCallback : DiffUtil.ItemCallback<AmenityBookingRecord>() {
            override fun areItemsTheSame(oldItem: AmenityBookingRecord, newItem: AmenityBookingRecord): Boolean = oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: AmenityBookingRecord, newItem: AmenityBookingRecord): Boolean = oldItem == newItem
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
