package com.example.madstayhub.presentation.guest

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.madstayhub.MainActivity
import com.example.madstayhub.databinding.FragmentOrderProgressBinding
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class OrderProgressFragment : Fragment() {
    private var _binding: FragmentOrderProgressBinding? = null
    private val binding get() = _binding!!

    private val db = FirebaseFirestore.getInstance()
    private var orderListener: ListenerRegistration? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentOrderProgressBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        val orderId = arguments?.getString("orderId") ?: ""
        binding.tvOrderId.text = "Order ID: #$orderId"

        if (orderId.isNotEmpty()) {
            orderListener = db.collection("room_service_orders").document(orderId)
                .addSnapshotListener { document, error ->
                    if (error != null || document == null || !isAdded) return@addSnapshotListener
                    updateOrderStatus(document)
                }
        }

        binding.btnBackToHome.setOnClickListener {
            (activity as? MainActivity)?.navigateToGuestFlow()
        }
    }

    private fun updateOrderStatus(document: DocumentSnapshot) {
        val status = document.getString("status") ?: "ordered"
        
        when (status) {
            "ordered" -> {
                binding.tvStatusTitle.text = "Order Received"
                binding.tvStatusDesc.text = "Awaiting confirmation from staff."
                binding.ivStatusAnimation.setImageResource(android.R.drawable.ic_menu_today)
            }
            "preparing" -> {
                binding.tvStatusTitle.text = "Preparing Meal"
                binding.tvStatusDesc.text = "Chef is preparing your gourmet order."
                binding.ivStatusAnimation.setImageResource(android.R.drawable.ic_popup_sync)
            }
            "delivering" -> {
                binding.tvStatusTitle.text = "Out for Delivery"
                binding.tvStatusDesc.text = "Your food is on its way to your room!"
                binding.ivStatusAnimation.setImageResource(android.R.drawable.ic_menu_directions)
            }
            "completed" -> {
                binding.tvStatusTitle.text = "Order Delivered"
                binding.tvStatusDesc.text = "Enjoy your meal!"
                binding.ivStatusAnimation.setImageResource(android.R.drawable.stat_sys_phone_call)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        orderListener?.remove()
        _binding = null
    }
}