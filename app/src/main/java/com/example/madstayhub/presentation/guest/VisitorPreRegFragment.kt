package com.example.madstayhub.presentation.guest

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.madstayhub.R
import com.example.madstayhub.databinding.FragmentVisitorPreRegBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

class VisitorPreRegFragment : Fragment() {
    private var _binding: FragmentVisitorPreRegBinding? = null
    private val binding get() = _binding!!

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private val calendar = Calendar.getInstance()
    private var selectedDateStr = ""
    private var selectedTimeStr = ""

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentVisitorPreRegBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnPickDate.setOnClickListener {
            DatePickerDialog(requireContext(), { _, year, month, day ->
                calendar.set(Calendar.YEAR, year)
                calendar.set(Calendar.MONTH, month)
                calendar.set(Calendar.DAY_OF_MONTH, day)
                val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                selectedDateStr = sdf.format(calendar.time)
                binding.btnPickDate.text = selectedDateStr
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
        }

        binding.btnPickTime.setOnClickListener {
            TimePickerDialog(requireContext(), { _, hour, minute ->
                calendar.set(Calendar.HOUR_OF_DAY, hour)
                calendar.set(Calendar.MINUTE, minute)
                val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
                selectedTimeStr = sdf.format(calendar.time)
                binding.btnPickTime.text = selectedTimeStr
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), false).show()
        }

        binding.btnSubmitVisitor.setOnClickListener {
            val name = binding.etVisitorName.text.toString().trim()
            val phone = binding.etVisitorPhone.text.toString().trim()

            if (name.isEmpty()) {
                Toast.makeText(context, "Please enter visitor name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (phone.isEmpty()) {
                Toast.makeText(context, "Please enter visitor phone", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (selectedDateStr.isEmpty() || selectedTimeStr.isEmpty()) {
                Toast.makeText(context, "Please select date and time", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            submitVisitorRegistration(name, phone)
        }

        setupHistoryRecyclerView()
    }

    private lateinit var adapter: VisitorHistoryAdapter

    data class VisitorRecord(
        val visitorName: String,
        val visitorPhone: String,
        val date: String,
        val time: String,
        val roomNumber: String,
        val hostName: String
    )

    private fun setupHistoryRecyclerView() {
        adapter = VisitorHistoryAdapter { record ->
            val bundle = Bundle().apply {
                putString("visitorName", record.visitorName)
                putString("expectedTime", "${record.date}, ${record.time}")
                putString("roomNumber", record.roomNumber)
                putString("hostName", record.hostName)
            }
            findNavController().navigate(R.id.action_visitor_to_qr, bundle)
        }
        binding.rvVisitorsList.adapter = adapter
        fetchVisitorHistory()
    }

    private fun fetchVisitorHistory() {
        val user = auth.currentUser ?: return
        db.collection("visitor_registrations")
            .whereEqualTo("guestUid", user.uid)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (!isAdded) return@addOnSuccessListener
                val list = querySnapshot.documents.mapNotNull { doc ->
                    val vName = doc.getString("visitorName") ?: return@mapNotNull null
                    val vPhone = doc.getString("visitorPhone") ?: ""
                    val vDate = doc.getString("date") ?: ""
                    val vTime = doc.getString("time") ?: ""
                    val room = doc.getString("roomNumber") ?: "None"
                    val host = doc.getString("hostName") ?: "Guest"
                    VisitorRecord(vName, vPhone, vDate, vTime, room, host)
                }
                adapter.submitList(list)
            }
    }

    private fun submitVisitorRegistration(name: String, phone: String) {
        val user = auth.currentUser ?: return
        binding.btnSubmitVisitor.isEnabled = false

        db.collection("users").document(user.uid).get()
            .addOnSuccessListener { doc ->
                val room = doc?.getString("roomNumber") ?: "None"
                val hostName = doc?.getString("name") ?: "Guest"

                val registrationData = hashMapOf(
                    "guestUid" to user.uid,
                    "hostName" to hostName,
                    "roomNumber" to room,
                    "visitorName" to name,
                    "visitorPhone" to phone,
                    "date" to selectedDateStr,
                    "time" to selectedTimeStr,
                    "timestamp" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                )

                db.collection("visitor_registrations").document().set(registrationData)
                
                if (isAdded) {
                    Toast.makeText(context, "Visitor Pre-registered Successfully!", Toast.LENGTH_SHORT).show()
                    val bundle = Bundle().apply {
                        putString("visitorName", name)
                        putString("expectedTime", "$selectedDateStr, $selectedTimeStr")
                        putString("roomNumber", room)
                        putString("hostName", hostName)
                    }
                    findNavController().navigate(R.id.action_visitor_to_qr, bundle)
                }
            }
            .addOnFailureListener { e ->
                if (isAdded) {
                    binding.btnSubmitVisitor.isEnabled = true
                    Toast.makeText(context, "Failed to fetch details: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private class VisitorHistoryAdapter(private val onClick: (VisitorRecord) -> Unit) :
        ListAdapter<VisitorRecord, VisitorHistoryAdapter.ViewHolder>(DiffCallback) {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val binding = com.example.madstayhub.databinding.ItemInvoiceBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
            return ViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = getItem(position)
            holder.bind(item)
            holder.itemView.setOnClickListener { onClick(item) }
        }

        class ViewHolder(private val binding: com.example.madstayhub.databinding.ItemInvoiceBinding) :
            RecyclerView.ViewHolder(binding.root) {
            fun bind(record: VisitorRecord) {
                binding.tvInvoiceTitle.text = record.visitorName
                binding.tvInvoiceId.text = "Expected: ${record.date} • ${record.time}"
                binding.tvInvoiceAmount.text = "View Pass"
                binding.tvInvoiceAmount.setTextColor(binding.root.context.resources.getColor(com.example.madstayhub.R.color.primary, null))
            }
        }

        object DiffCallback : DiffUtil.ItemCallback<VisitorRecord>() {
            override fun areItemsTheSame(oldItem: VisitorRecord, newItem: VisitorRecord): Boolean =
                oldItem.visitorName == newItem.visitorName && oldItem.date == newItem.date
            override fun areContentsTheSame(oldItem: VisitorRecord, newItem: VisitorRecord): Boolean =
                oldItem == newItem
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}