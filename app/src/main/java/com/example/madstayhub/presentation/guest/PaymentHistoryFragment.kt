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
import com.example.madstayhub.databinding.FragmentPaymentHistoryBinding
import com.example.madstayhub.databinding.ItemInvoiceBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.*

class PaymentHistoryFragment : Fragment() {
    private var _binding: FragmentPaymentHistoryBinding? = null
    private val binding get() = _binding!!

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private lateinit var adapter: PaymentAdapter

    data class PaymentRecord(
        val id: String,
        val title: String,
        val amount: Double,
        val date: Date?
    )

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPaymentHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        adapter = PaymentAdapter()
        binding.rvPayments.adapter = adapter

        fetchPayments()
    }

    private fun fetchPayments() {
        val user = auth.currentUser ?: return
        val list = mutableListOf<PaymentRecord>()

        // 1. Fetch Room Service Orders
        db.collection("room_service_orders")
            .whereEqualTo("guestUid", user.uid)
            .get()
            .addOnSuccessListener { querySnapshot ->
                for (doc in querySnapshot.documents) {
                    val id = doc.id.take(8).uppercase()
                    val total = doc.getDouble("totalAmount") ?: 0.0
                    val date = doc.getDate("timestamp")
                    
                    val items = doc.get("items") as? List<Map<String, Any>>
                    val itemNames = items?.joinToString { itemMap ->
                        val name = itemMap["itemName"] as? String ?: "Item"
                        val qty = (itemMap["quantity"] as? Number)?.toInt() ?: 1
                        "$name x$qty"
                    } ?: "Room Service Order"
                    
                    list.add(PaymentRecord("#RS-$id", itemNames, total, date))
                }
                
                // 2. Fetch User Rent Info to show if rent paid
                db.collection("users").document(user.uid).get()
                    .addOnSuccessListener { userDoc ->
                        val rentPaid = userDoc.getBoolean("isRentPaid") ?: false
                        if (rentPaid) {
                            list.add(PaymentRecord("#RT-RENT", "Monthly Rent Payment", 45000.0, Date()))
                        }
                        
                        // Sort by date descending
                        list.sortByDescending { it.date ?: Date(0) }
                        adapter.submitList(list.toList())
                    }
                    .addOnFailureListener {
                        list.sortByDescending { it.date ?: Date(0) }
                        adapter.submitList(list.toList())
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Error fetching payments: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private class PaymentAdapter : ListAdapter<PaymentRecord, PaymentAdapter.ViewHolder>(DiffCallback) {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val binding = ItemInvoiceBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(getItem(position))
        }

        class ViewHolder(private val binding: ItemInvoiceBinding) : RecyclerView.ViewHolder(binding.root) {
            fun bind(record: PaymentRecord) {
                binding.tvInvoiceTitle.text = record.title
                
                val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
                val formattedDate = record.date?.let { sdf.format(it) } ?: "Date Unknown"
                binding.tvInvoiceId.text = "ID: ${record.id} • $formattedDate"
                binding.tvInvoiceAmount.text = "Rs. ${record.amount.toInt()}"
            }
        }

        object DiffCallback : DiffUtil.ItemCallback<PaymentRecord>() {
            override fun areItemsTheSame(oldItem: PaymentRecord, newItem: PaymentRecord): Boolean = oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: PaymentRecord, newItem: PaymentRecord): Boolean = oldItem == newItem
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
