package com.example.madstayhub.presentation.guest

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.madstayhub.R
import com.example.madstayhub.databinding.FragmentCheckoutBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class CheckoutFragment : Fragment() {
    private var _binding: FragmentCheckoutBinding? = null
    private val binding get() = _binding!!

    private val viewModel: RoomServiceViewModel by activityViewModels()
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    
    private var roomNumber = "None"
    private var guestName = "Guest"

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCheckoutBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        fetchUserDetails()

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.cart.collect { cartMap ->
                binding.llOrderItems.removeAllViews()
                
                cartMap.forEach { (item, qty) ->
                    val textView = TextView(context).apply {
                        text = "${item.name} x $qty - Rs. ${(item.price * qty).toInt()}"
                        textSize = 15f
                        setPadding(0, 8, 0, 8)
                        setTextColor(resources.getColor(R.color.text_primary, null))
                    }
                    binding.llOrderItems.addView(textView)
                }

                val total = cartMap.entries.sumOf { it.key.price * it.value }
                binding.tvCheckoutTotal.text = "Rs. ${total.toInt()}"
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.orderPlacedId.collect { orderId ->
                if (orderId != null) {
                    Toast.makeText(context, "Order Placed Successfully!", Toast.LENGTH_SHORT).show()
                    viewModel.clearCart()
                    val bundle = Bundle().apply {
                        putString("orderId", orderId)
                    }
                    findNavController().navigate(R.id.action_checkout_to_orderProgress, bundle)
                    viewModel.resetOrderState() // Reset so subsequent orders trigger collection
                    
                    // Reset button state
                    binding.btnPayNow.isEnabled = true
                    binding.btnPayNow.setBackgroundColor(resources.getColor(R.color.primary, null))
                    binding.btnPayNow.text = "Pay and Confirm Order"
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.orderError.collect { error ->
                if (error != null) {
                    Toast.makeText(context, "Order Failed: $error", Toast.LENGTH_SHORT).show()
                    binding.btnPayNow.isEnabled = true
                    binding.btnPayNow.setBackgroundColor(resources.getColor(R.color.primary, null))
                    binding.btnPayNow.text = "Pay and Confirm Order"
                }
            }
        }

        binding.btnPayNow.setOnClickListener {
            val user = auth.currentUser ?: return@setOnClickListener
            binding.btnPayNow.isEnabled = false
            binding.btnPayNow.setBackgroundColor(android.graphics.Color.GRAY)
            binding.btnPayNow.text = "Processing..."

            val cartMap = viewModel.cart.value
            val total = cartMap.entries.sumOf { it.key.price * it.value }

            db.collection("users").document(user.uid).get()
                .addOnSuccessListener { doc ->
                    if (doc != null) {
                        val balance = doc.getDouble("walletBalance") ?: 1000000.0
                        if (balance >= total) {
                            val newBalance = balance - total
                            db.collection("users").document(user.uid).update("walletBalance", newBalance)
                            // Proceed optimistically without waiting for firestore write network confirmation
                            viewModel.placeOrder(roomNumber, guestName)
                        } else {
                            Toast.makeText(context, "Insufficient wallet balance! Available: Rs. $balance", Toast.LENGTH_LONG).show()
                            binding.btnPayNow.isEnabled = true
                            binding.btnPayNow.setBackgroundColor(resources.getColor(R.color.primary, null))
                            binding.btnPayNow.text = "Pay and Confirm Order"
                        }
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(context, "Error fetching wallet: ${e.message}", Toast.LENGTH_SHORT).show()
                    binding.btnPayNow.isEnabled = true
                    binding.btnPayNow.setBackgroundColor(resources.getColor(R.color.primary, null))
                    binding.btnPayNow.text = "Pay and Confirm Order"
                }
        }
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
}