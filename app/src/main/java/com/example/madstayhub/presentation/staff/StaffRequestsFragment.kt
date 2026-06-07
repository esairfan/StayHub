package com.example.madstayhub.presentation.staff

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
import com.example.madstayhub.databinding.FragmentStaffRequestsBinding
import com.example.madstayhub.databinding.ItemStaffRequestBinding
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.text.SimpleDateFormat
import java.util.*

class StaffRequestsFragment : Fragment() {
    private var _binding: FragmentStaffRequestsBinding? = null
    private val binding get() = _binding!!
    private val db = FirebaseFirestore.getInstance()
    private lateinit var adapter: RequestsAdapter

    private var laundryListener: ListenerRegistration? = null
    private var housekeepingListener: ListenerRegistration? = null
    private var amenityListener: ListenerRegistration? = null

    private val laundryList = mutableListOf<RequestItem>()
    private val housekeepingList = mutableListOf<RequestItem>()
    private val amenityList = mutableListOf<RequestItem>()

    data class RequestItem(
        val id: String,
        val collectionName: String,
        val type: String,
        val title: String,
        val guestName: String,
        val roomNumber: String,
        val guestUid: String,
        val timestamp: Date?,
        val status: String
    )

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentStaffRequestsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        adapter = RequestsAdapter(
            onAccept = { item -> handleRequestStatus(item, "accepted") },
            onReject = { item -> handleRequestStatus(item, "rejected") }
        )
        binding.rvServiceRequests.adapter = adapter

        startRealTimeListeners()
    }

    private fun startRealTimeListeners() {
        // 1. Laundry Requests (show pickup_requested or pending status requests)
        laundryListener = db.collection("laundry_requests")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                laundryList.clear()
                for (doc in snapshot.documents) {
                    val status = doc.getString("status") ?: "pending"
                    if (status == "pickup_requested" || status == "pending") {
                        val shirts = (doc.get("shirts") as? Number)?.toInt() ?: 0
                        val trousers = (doc.get("trousers") as? Number)?.toInt() ?: 0
                        laundryList.add(
                            RequestItem(
                                id = doc.id,
                                collectionName = "laundry_requests",
                                type = "LAUNDRY",
                                title = "Laundry: $shirts Shirts, $trousers Trousers",
                                guestName = doc.getString("guestName") ?: "Guest",
                                roomNumber = doc.getString("roomNumber") ?: "None",
                                guestUid = doc.getString("guestUid") ?: "",
                                timestamp = doc.getDate("timestamp"),
                                status = status
                            )
                        )
                    }
                }
                combineAndSubmitList()
            }

        // 2. Housekeeping Requests
        housekeepingListener = db.collection("housekeeping_requests")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                housekeepingList.clear()
                for (doc in snapshot.documents) {
                    val status = doc.getString("status") ?: "pending"
                    if (status == "pending") {
                        val cleaningType = doc.getString("type") ?: "General Cleaning"
                        housekeepingList.add(
                            RequestItem(
                                id = doc.id,
                                collectionName = "housekeeping_requests",
                                type = "HOUSEKEEPING",
                                title = "Housekeeping: $cleaningType",
                                guestName = doc.getString("guestName") ?: "Guest",
                                roomNumber = doc.getString("roomNumber") ?: "None",
                                guestUid = doc.getString("guestUid") ?: "",
                                timestamp = doc.getDate("timestamp"),
                                status = status
                            )
                        )
                    }
                }
                combineAndSubmitList()
            }

        // 3. Amenity Bookings
        amenityListener = db.collection("amenity_bookings")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                amenityList.clear()
                for (doc in snapshot.documents) {
                    val status = doc.getString("status") ?: "booked"
                    if (status == "booked" || status == "pending") {
                        val amenityName = doc.getString("amenityName") ?: "Amenity"
                        val timeSlot = doc.getString("timeSlot") ?: "N/A"
                        amenityList.add(
                            RequestItem(
                                id = doc.id,
                                collectionName = "amenity_bookings",
                                type = "AMENITY BOOKING",
                                title = "$amenityName Booking ($timeSlot)",
                                guestName = doc.getString("guestName") ?: "Guest",
                                roomNumber = doc.getString("roomNumber") ?: "None",
                                guestUid = doc.getString("guestUid") ?: "",
                                timestamp = doc.getDate("timestamp"),
                                status = status
                            )
                        )
                    }
                }
                combineAndSubmitList()
            }
    }

    private fun combineAndSubmitList() {
        val allRequests = mutableListOf<RequestItem>()
        allRequests.addAll(laundryList)
        allRequests.addAll(housekeepingList)
        allRequests.addAll(amenityList)
        allRequests.sortByDescending { it.timestamp ?: Date(0) }

        if (allRequests.isEmpty()) {
            binding.tvEmptyState.visibility = View.VISIBLE
            binding.rvServiceRequests.visibility = View.GONE
        } else {
            binding.tvEmptyState.visibility = View.GONE
            binding.rvServiceRequests.visibility = View.VISIBLE
        }
        adapter.submitList(allRequests)
    }

    private fun handleRequestStatus(item: RequestItem, newStatus: String) {
        // Update document status
        db.collection(item.collectionName).document(item.id)
            .update("status", newStatus)
            .addOnSuccessListener {
                val capitalizedStatus = newStatus.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
                Toast.makeText(context, "Request $capitalizedStatus!", Toast.LENGTH_SHORT).show()
                createPrivateNotification(item, newStatus)
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Error updating status: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun createPrivateNotification(item: RequestItem, newStatus: String) {
        val uid = item.guestUid
        if (uid.isEmpty()) return

        // 24 Hour Expiry Timestamp
        val expiryTime = Date(System.currentTimeMillis() + (24 * 60 * 60 * 1000))

        val notification = hashMapOf(
            "guestUid" to uid,
            "title" to "${item.type} Update",
            "body" to "Your request for '${item.title}' has been $newStatus by the staff.",
            "timestamp" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
            "expiryTimestamp" to expiryTime,
            "type" to "private",
            "isRead" to false
        )

        db.collection("announcements").document().set(notification)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        laundryListener?.remove()
        housekeepingListener?.remove()
        amenityListener?.remove()
        _binding = null
    }

    private class RequestsAdapter(
        private val onAccept: (RequestItem) -> Unit,
        private val onReject: (RequestItem) -> Unit
    ) : ListAdapter<RequestItem, RequestsAdapter.ViewHolder>(DiffCallback) {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val binding = ItemStaffRequestBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(getItem(position))
        }

        inner class ViewHolder(private val binding: ItemStaffRequestBinding) : RecyclerView.ViewHolder(binding.root) {
            fun bind(item: RequestItem) {
                binding.tvRequestType.text = item.type
                binding.tvRequestTitle.text = item.title
                binding.tvRequestGuestInfo.text = "Guest: ${item.guestName} (Room ${item.roomNumber})"

                val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
                binding.tvRequestTime.text = item.timestamp?.let { sdf.format(it) } ?: "Just now"

                binding.btnAcceptRequest.setOnClickListener { onAccept(item) }
                binding.btnRejectRequest.setOnClickListener { onReject(item) }
            }
        }

        object DiffCallback : DiffUtil.ItemCallback<RequestItem>() {
            override fun areItemsTheSame(oldItem: RequestItem, newItem: RequestItem): Boolean = oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: RequestItem, newItem: RequestItem): Boolean = oldItem == newItem
        }
    }
}
