package com.example.madstayhub.presentation.staff

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.madstayhub.R
import com.example.madstayhub.databinding.FragmentStaffGuestsBinding
import com.example.madstayhub.databinding.ItemInvoiceBinding
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class StaffGuestsFragment : Fragment() {

    private var _binding: FragmentStaffGuestsBinding? = null
    private val binding get() = _binding!!
    private val db = FirebaseFirestore.getInstance()
    private lateinit var adapter: GuestAdapter
    private var listener: ListenerRegistration? = null

    data class GuestRecord(
        val uid: String,
        val name: String,
        val email: String,
        val roomNumber: String,
        val isVerified: Boolean,
        val isKycPending: Boolean
    )

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentStaffGuestsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        adapter = GuestAdapter { guest ->
            val bundle = Bundle().apply {
                putString("guestUid", guest.uid)
                putString("guestName", guest.name)
            }
            findNavController().navigate(R.id.action_guests_to_detail, bundle)
        }
        binding.rvGuests.adapter = adapter

        listenForGuests()
    }

    private fun listenForGuests() {
        listener = db.collection("users")
            .whereEqualTo("role", "guest")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null || !isAdded) return@addSnapshotListener
                
                val list = mutableListOf<GuestRecord>()
                for (doc in snapshot.documents) {
                    val uid = doc.id
                    val name = doc.getString("name") ?: "Guest"
                    val email = doc.getString("email") ?: ""
                    val room = doc.getString("roomNumber") ?: "None"
                    val verified = doc.getBoolean("isVerified") ?: false
                    val pending = doc.getBoolean("isKycPending") ?: false
                    
                    list.add(GuestRecord(uid, name, email, room, verified, pending))
                }

                if (list.isEmpty()) {
                    binding.tvEmptyGuests.visibility = View.VISIBLE
                    binding.rvGuests.visibility = View.GONE
                } else {
                    binding.tvEmptyGuests.visibility = View.GONE
                    binding.rvGuests.visibility = View.VISIBLE
                }

                adapter.submitList(list)
            }
    }

    private class GuestAdapter(private val onClick: (GuestRecord) -> Unit) : ListAdapter<GuestRecord, GuestAdapter.ViewHolder>(DiffCallback) {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val binding = ItemInvoiceBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(getItem(position))
        }

        inner class ViewHolder(private val binding: ItemInvoiceBinding) : RecyclerView.ViewHolder(binding.root) {
            fun bind(item: GuestRecord) {
                binding.tvInvoiceTitle.text = "${item.name} (Room: ${item.roomNumber})"
                binding.tvInvoiceId.text = item.email
                
                val statusText = when {
                    item.isVerified -> "Verified"
                    item.isKycPending -> "KYC Pending"
                    else -> "No KYC"
                }
                
                binding.tvInvoiceAmount.text = statusText
                
                val statusColor = when {
                    item.isVerified -> "#4CAF50" // green
                    item.isKycPending -> "#FF9800" // orange
                    else -> "#F44336" // red
                }
                binding.tvInvoiceAmount.setTextColor(android.graphics.Color.parseColor(statusColor))
                
                binding.root.setOnClickListener {
                    onClick(item)
                }
            }
        }

        object DiffCallback : DiffUtil.ItemCallback<GuestRecord>() {
            override fun areItemsTheSame(oldItem: GuestRecord, newItem: GuestRecord): Boolean = oldItem.uid == newItem.uid
            override fun areContentsTheSame(oldItem: GuestRecord, newItem: GuestRecord): Boolean = oldItem == newItem
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        listener?.remove()
        _binding = null
    }
}
