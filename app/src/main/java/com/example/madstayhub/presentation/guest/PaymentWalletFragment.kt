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
import com.example.madstayhub.databinding.FragmentPaymentWalletBinding
import com.example.madstayhub.databinding.ItemInvoiceBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class PaymentWalletFragment : Fragment() {
    private var _binding: FragmentPaymentWalletBinding? = null
    private val binding get() = _binding!!

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private var listenerRegistration: com.google.firebase.firestore.ListenerRegistration? = null

    data class SavedCard(val title: String, val lastFour: String, val expiry: String)

    private lateinit var adapter: SavedCardAdapter
    private val cardList = mutableListOf(
        SavedCard("Visa Primary", "**** **** **** 4242", "Exp: 12/28"),
        SavedCard("Mastercard Backup", "**** **** **** 8899", "Exp: 08/27")
    )

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPaymentWalletBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        adapter = SavedCardAdapter()
        binding.rvSavedCards.adapter = adapter
        adapter.submitList(cardList.toList())

        binding.btnAddCard.setOnClickListener {
            showAddCardDialog()
        }

        listenToWalletBalance()
    }

    private fun listenToWalletBalance() {
        val user = auth.currentUser ?: return
        listenerRegistration = db.collection("users").document(user.uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null || !isAdded) return@addSnapshotListener
                val balance = snapshot.getDouble("walletBalance") ?: 1000000.0
                binding.tvWalletBalance.text = "Rs. " + String.format("%,.2f", balance)
            }
    }

    private fun showAddCardDialog() {
        val dialogView = LayoutInflater.from(context).inflate(com.example.madstayhub.R.layout.fragment_visitor_pre_reg, null)
        // Let's build a simple custom dialog layout programmatically to be safe and robust
        val builder = MaterialAlertDialogBuilder(requireContext())
        builder.setTitle("Add New Card")
        
        val layout = android.widget.LinearLayout(context).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(48, 24, 48, 24)
        }
        
        val etCardHolder = TextInputEditText(requireContext()).apply {
            hint = "Card Holder Name"
        }
        val etCardNum = TextInputEditText(requireContext()).apply {
            hint = "Card Number"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
        }
        val etExpiry = TextInputEditText(requireContext()).apply {
            hint = "MM/YY"
        }
        
        layout.addView(etCardHolder)
        layout.addView(etCardNum)
        layout.addView(etExpiry)
        
        builder.setView(layout)
        builder.setPositiveButton("Add") { dialog, _ ->
            val holder = etCardHolder.text.toString().trim()
            val num = etCardNum.text.toString().trim()
            val exp = etExpiry.text.toString().trim()
            
            if (holder.isEmpty() || num.length < 12 || exp.isEmpty()) {
                Toast.makeText(context, "Please enter valid card details", Toast.LENGTH_SHORT).show()
                return@setPositiveButton
            }
            
            val lastFour = "**** **** **** " + num.takeLast(4)
            cardList.add(SavedCard(holder, lastFour, "Exp: $exp"))
            adapter.submitList(cardList.toList())
            Toast.makeText(context, "Card added successfully!", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }
        builder.setNegativeButton("Cancel") { dialog, _ ->
            dialog.dismiss()
        }
        builder.show()
    }

    private class SavedCardAdapter : ListAdapter<SavedCard, SavedCardAdapter.ViewHolder>(DiffCallback) {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val binding = ItemInvoiceBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(getItem(position))
        }

        class ViewHolder(private val binding: ItemInvoiceBinding) : RecyclerView.ViewHolder(binding.root) {
            fun bind(card: SavedCard) {
                binding.tvInvoiceTitle.text = card.title
                binding.tvInvoiceId.text = card.lastFour
                binding.tvInvoiceAmount.text = card.expiry
            }
        }

        object DiffCallback : DiffUtil.ItemCallback<SavedCard>() {
            override fun areItemsTheSame(oldItem: SavedCard, newItem: SavedCard): Boolean = oldItem.lastFour == newItem.lastFour
            override fun areContentsTheSame(oldItem: SavedCard, newItem: SavedCard): Boolean = oldItem == newItem
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        listenerRegistration?.remove()
        _binding = null
    }
}