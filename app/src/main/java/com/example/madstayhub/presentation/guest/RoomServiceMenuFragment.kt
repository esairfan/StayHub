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
import com.example.madstayhub.databinding.FragmentRoomServiceMenuBinding
import com.example.madstayhub.databinding.ItemMenuFoodBinding
import com.google.android.material.chip.Chip
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class RoomServiceMenuFragment : Fragment() {
    private var _binding: FragmentRoomServiceMenuBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: RoomServiceViewModel by activityViewModels()
    private lateinit var adapter: MenuAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentRoomServiceMenuBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        adapter = MenuAdapter { item ->
            viewModel.addToCart(item)
        }
        binding.rvMenu.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.menuItems.collect { items ->
                filterItems(items)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.cart.collect { cart ->
                val totalItems = cart.values.sum()
                binding.fabCart.text = "View Cart ($totalItems)"
            }
        }

        binding.chipGroupCategories.setOnCheckedStateChangeListener { _, checkedIds ->
            val checkedId = checkedIds.firstOrNull()
            val checkedChip = checkedId?.let { view?.findViewById<Chip>(it) }
            val category = checkedChip?.text?.toString() ?: "All"
            val allItems = viewModel.menuItems.value
            if (category == "All") {
                adapter.submitList(allItems)
            } else {
                adapter.submitList(allItems.filter { it.category.equals(category, ignoreCase = true) })
            }
        }

        binding.fabCart.setOnClickListener {
            findNavController().navigate(R.id.action_menu_to_cart)
        }
    }

    private fun filterItems(items: List<FoodItem>) {
        val checkedId = binding.chipGroupCategories.checkedChipId
        val checkedChip = checkedId.let { view?.findViewById<Chip>(it) }
        val category = checkedChip?.text?.toString() ?: "All"
        if (category == "All") {
            adapter.submitList(items)
        } else {
            adapter.submitList(items.filter { it.category.equals(category, ignoreCase = true) })
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private class MenuAdapter(private val onAddClick: (FoodItem) -> Unit) : 
        ListAdapter<FoodItem, MenuAdapter.ViewHolder>(DiffCallback) {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val binding = ItemMenuFoodBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ViewHolder(binding, onAddClick)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(getItem(position))
        }

        class ViewHolder(private val binding: ItemMenuFoodBinding, private val onAddClick: (FoodItem) -> Unit) : 
            RecyclerView.ViewHolder(binding.root) {
            fun bind(item: FoodItem) {
                binding.tvFoodName.text = item.name
                binding.tvFoodDesc.text = item.description
                binding.tvFoodPrice.text = "Rs. ${item.price.toInt()}"
                binding.ivFoodImage.setImageResource(item.imageResId)
                binding.btnAddToCart.setOnClickListener { onAddClick(item) }
            }
        }

        object DiffCallback : DiffUtil.ItemCallback<FoodItem>() {
            override fun areItemsTheSame(oldItem: FoodItem, newItem: FoodItem): Boolean = oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: FoodItem, newItem: FoodItem): Boolean = oldItem == newItem
        }
    }
}