package com.example.madstayhub.presentation.guest

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.madstayhub.R
import com.example.madstayhub.databinding.FragmentAmenityBookingBinding
import com.example.madstayhub.databinding.ItemAmenityBinding
import com.google.android.material.chip.Chip
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@AndroidEntryPoint
class AmenityBookingFragment : Fragment() {
    private var _binding: FragmentAmenityBookingBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AmenityBookingViewModel by viewModels()
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private lateinit var adapter: AmenityAdapter
    private var selectedDate = ""
    private var roomNumber = "None"
    private var guestName = "Guest"

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAmenityBookingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        fetchUserDetails()
        setupCalendar()

        adapter = AmenityAdapter { amenity ->
            viewModel.selectAmenity(amenity)
        }
        binding.rvAmenities.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.amenities.collect { list ->
                adapter.submitList(list)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.bookingConfirmed.collect { isConfirmed ->
                if (isConfirmed) {
                    Toast.makeText(context, "Amenity Booked Successfully!", Toast.LENGTH_SHORT).show()
                    viewModel.resetBookingState()
                    findNavController().navigateUp()
                }
            }
        }

        binding.btnConfirmBooking.setOnClickListener {
            val amenity = viewModel.selectedAmenity.value
            if (amenity == null) {
                Toast.makeText(context, "Please select an amenity", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val checkedChipId = binding.cgTimeSlots.checkedChipId
            val chip = binding.cgTimeSlots.findViewById<Chip>(checkedChipId)
            val timeSlot = chip?.text?.toString() ?: ""

            if (timeSlot.isEmpty()) {
                Toast.makeText(context, "Please select a time slot", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            viewModel.bookAmenity(selectedDate, timeSlot, roomNumber, guestName)
        }
    }

    private fun setupCalendar() {
        val sdf = SimpleDateFormat("EEE\ndd", Locale.getDefault())
        val dbSdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        
        binding.llCalendar.removeAllViews()
        val calendar = Calendar.getInstance()
        selectedDate = dbSdf.format(calendar.time)

        for (i in 0 until 5) {
            val dateStr = sdf.format(calendar.time)
            val fullDateVal = dbSdf.format(calendar.time)
            
            val dayTextView = TextView(context).apply {
                text = dateStr
                textAlignment = View.TEXT_ALIGNMENT_CENTER
                textSize = 14f
                setPadding(24, 16, 24, 16)
                setTextColor(Color.GRAY)
                setBackgroundResource(android.R.color.transparent)
                
                setOnClickListener {
                    selectedDate = fullDateVal
                    setupCalendarHighlight(this)
                }
            }

            if (i == 0) {
                dayTextView.setTextColor(Color.WHITE)
                dayTextView.setBackgroundColor(resources.getColor(R.color.primary, null))
            }
            binding.llCalendar.addView(dayTextView)
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }
    }

    private fun setupCalendarHighlight(selectedView: TextView) {
        for (i in 0 until binding.llCalendar.childCount) {
            val child = binding.llCalendar.getChildAt(i) as TextView
            child.setTextColor(Color.GRAY)
            child.setBackgroundResource(android.R.color.transparent)
        }
        selectedView.setTextColor(Color.WHITE)
        selectedView.setBackgroundColor(resources.getColor(R.color.primary, null))
    }

    private fun fetchUserDetails() {
        val user = auth.currentUser ?: return
        db.collection("users").document(user.uid).get()
            .addOnSuccessListener { doc ->
                if (isAdded && doc != null) {
                    roomNumber = doc.getString("roomNumber") ?: "None"
                    guestName = doc.getString("name") ?: "Guest"
                }
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private class AmenityAdapter(private val onSelect: (AmenityItem) -> Unit) : 
        ListAdapter<AmenityItem, AmenityAdapter.ViewHolder>(DiffCallback) {

        private var selectedPos = -1

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val binding = ItemAmenityBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = getItem(position)
            holder.bind(item, position == selectedPos)
            holder.itemView.setOnClickListener {
                val oldSelected = selectedPos
                selectedPos = holder.adapterPosition
                notifyItemChanged(oldSelected)
                notifyItemChanged(selectedPos)
                onSelect(item)
            }
        }

        class ViewHolder(private val binding: ItemAmenityBinding) : RecyclerView.ViewHolder(binding.root) {
            val rbSelect = binding.rbSelect
            fun bind(item: AmenityItem, isSelected: Boolean) {
                binding.tvAmenityName.text = item.name
                binding.tvAmenityHours.text = item.hours
                binding.ivAmenityIcon.setImageResource(item.iconResId)
                binding.rbSelect.isChecked = isSelected
            }
        }

        object DiffCallback : DiffUtil.ItemCallback<AmenityItem>() {
            override fun areItemsTheSame(oldItem: AmenityItem, newItem: AmenityItem): Boolean = oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: AmenityItem, newItem: AmenityItem): Boolean = oldItem == newItem
        }
    }
}