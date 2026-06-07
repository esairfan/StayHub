package com.example.madstayhub.presentation.guest

import android.graphics.Color
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
import com.example.madstayhub.R
import com.example.madstayhub.databinding.FragmentRentUtilitiesBinding
import com.example.madstayhub.databinding.ItemInvoiceBinding

class RentUtilitiesFragment : Fragment() {
    private var _binding: FragmentRentUtilitiesBinding? = null
    private val binding get() = _binding!!

    private val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
    private val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
    private var rentListener: com.google.firebase.firestore.ListenerRegistration? = null

    data class Invoice(val id: String, val title: String, val amount: String)

    private lateinit var adapter: InvoiceAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentRentUtilitiesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        adapter = InvoiceAdapter()
        binding.rvPaymentHistory.adapter = adapter

        val list = listOf(
            Invoice("#SH-2018", "September Rent", "Rs. 45,000"),
            Invoice("#SH-2019", "Electricity Bill - Sept", "Rs. 4,200"),
            Invoice("#SH-2020", "Water Bill - Sept", "Rs. 850")
        )
        adapter.submitList(list)

        checkRentStatus()
    }

    private var rentPayInProgress = false

    private fun checkRentStatus() {
        val user = auth.currentUser ?: return
        rentListener = db.collection("users").document(user.uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null || !isAdded) return@addSnapshotListener
                val isPaid = snapshot.getBoolean("isRentPaid") ?: false
                val balance = snapshot.getDouble("walletBalance") ?: 1000000.0

                if (isPaid) {
                    binding.btnPayRent.isEnabled = false
                    binding.btnPayRent.setBackgroundColor(Color.GRAY)
                    binding.btnPayRent.text = "Paid"
                    binding.btnPayRent.setOnClickListener(null)
                } else {
                    if (!rentPayInProgress) {
                        binding.btnPayRent.isEnabled = true
                        binding.btnPayRent.setBackgroundColor(resources.getColor(R.color.primary, null))
                        binding.btnPayRent.text = "Pay Now"
                    }
                    
                    binding.btnPayRent.setOnClickListener {
                        if (rentPayInProgress) return@setOnClickListener
                        rentPayInProgress = true
                        val rentAmount = 45000.0
                        if (balance >= rentAmount) {
                            binding.btnPayRent.isEnabled = false
                            binding.btnPayRent.setBackgroundColor(Color.GRAY)
                            binding.btnPayRent.text = "Processing..."
                            
                            val updates = hashMapOf<String, Any>(
                                "walletBalance" to (balance - rentAmount),
                                "isRentPaid" to true
                            )
                            db.collection("users").document(user.uid).update(updates)
                                .addOnSuccessListener {
                                    rentPayInProgress = false
                                    if (isAdded) {
                                        Toast.makeText(context, "Rent of Rs. 45,000 Paid Successfully!", Toast.LENGTH_LONG).show()
                                    }
                                }
                                .addOnFailureListener { e ->
                                    rentPayInProgress = false
                                    if (isAdded) {
                                        binding.btnPayRent.isEnabled = true
                                        binding.btnPayRent.setBackgroundColor(resources.getColor(R.color.primary, null))
                                        binding.btnPayRent.text = "Pay Now"
                                        Toast.makeText(context, "Payment Failed: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                        } else {
                            rentPayInProgress = false
                            Toast.makeText(context, "Insufficient wallet balance! Available: Rs. $balance", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
    }

    private class InvoiceAdapter : ListAdapter<Invoice, InvoiceAdapter.ViewHolder>(DiffCallback) {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val binding = ItemInvoiceBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(getItem(position))
        }

        class ViewHolder(private val binding: ItemInvoiceBinding) : RecyclerView.ViewHolder(binding.root) {
            fun bind(invoice: Invoice) {
                binding.tvInvoiceTitle.text = invoice.title
                binding.tvInvoiceId.text = "ID: ${invoice.id}"
                binding.tvInvoiceAmount.text = invoice.amount
            }
        }

        object DiffCallback : DiffUtil.ItemCallback<Invoice>() {
            override fun areItemsTheSame(oldItem: Invoice, newItem: Invoice): Boolean = oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: Invoice, newItem: Invoice): Boolean = oldItem == newItem
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        rentListener?.remove()
        _binding = null
    }
}