package com.example.madstayhub.presentation.guest

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.madstayhub.R
import com.example.madstayhub.databinding.FragmentCartBinding
import com.example.madstayhub.databinding.ItemCartBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class CartFragment : Fragment() {
    private var _binding: FragmentCartBinding? = null
    private val binding get() = _binding!!

    private val viewModel: RoomServiceViewModel by activityViewModels()
    private lateinit var adapter: CartAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCartBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        adapter = CartAdapter(
            onIncreaseClick = { item -> viewModel.addToCart(item) },
            onDecreaseClick = { item -> viewModel.decreaseQuantity(item) }
        )
        binding.rvCartItems.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.cart.collect { cartMap ->
                val list = cartMap.entries.map { CartItem(it.key, it.value) }
                adapter.submitList(list)
                
                val totalAmount = cartMap.entries.sumOf { it.key.price * it.value }
                binding.tvTotalAmount.text = "Rs. ${totalAmount.toInt()}"
                
                binding.btnCheckout.isEnabled = list.isNotEmpty()
            }
        }

        binding.btnCheckout.setOnClickListener {
            findNavController().navigate(R.id.action_cart_to_checkout)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    data class CartItem(val foodItem: FoodItem, val quantity: Int)

    private class CartAdapter(
        private val onIncreaseClick: (FoodItem) -> Unit,
        private val onDecreaseClick: (FoodItem) -> Unit
    ) : ListAdapter<CartItem, CartAdapter.ViewHolder>(DiffCallback) {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val binding = ItemCartBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ViewHolder(binding, onIncreaseClick, onDecreaseClick)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(getItem(position))
        }

        class ViewHolder(
            private val binding: ItemCartBinding,
            private val onIncreaseClick: (FoodItem) -> Unit,
            private val onDecreaseClick: (FoodItem) -> Unit
        ) : RecyclerView.ViewHolder(binding.root) {
            fun bind(item: CartItem) {
                binding.tvItemName.text = item.foodItem.name
                binding.tvItemPrice.text = "Rs. ${(item.foodItem.price * item.quantity).toInt()}"
                binding.tvQuantity.text = item.quantity.toString()
                binding.ivItemImage.setImageResource(item.foodItem.imageResId)
                binding.btnIncrease.setOnClickListener { onIncreaseClick(item.foodItem) }
                binding.btnDecrease.setOnClickListener { onDecreaseClick(item.foodItem) }
            }
        }

        object DiffCallback : DiffUtil.ItemCallback<CartItem>() {
            override fun areItemsTheSame(oldItem: CartItem, newItem: CartItem): Boolean = 
                oldItem.foodItem.id == newItem.foodItem.id
            override fun areContentsTheSame(oldItem: CartItem, newItem: CartItem): Boolean = 
                oldItem == newItem
        }
    }
}