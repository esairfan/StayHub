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
import com.example.madstayhub.databinding.FragmentPackageTrackerBinding
import com.example.madstayhub.databinding.ItemPackageBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class PackageTrackerFragment : Fragment() {
    private var _binding: FragmentPackageTrackerBinding? = null
    private val binding get() = _binding!!

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    data class PackageItem(val id: String, val trackingNumber: String, val arrivalDate: String, val locker: String, val pin: String)

    private lateinit var adapter: PackageAdapter
    private val mockPackages = listOf(
        PackageItem("1", "TRK-98234710", "Arrived: Oct 11, 2026", "Locker 12", "5824"),
        PackageItem("2", "TRK-30294812", "Arrived: Oct 12, 2026", "Locker 05", "1983")
    )

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPackageTrackerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        adapter = PackageAdapter { pkg ->
            Toast.makeText(context, "Locker ${pkg.locker} PIN: ${pkg.pin}", Toast.LENGTH_LONG).show()
        }
        binding.rvPackages.adapter = adapter

        fetchPackages()
    }

    private fun fetchPackages() {
        val user = auth.currentUser
        if (user == null) {
            showList(mockPackages)
            return
        }

        db.collection("packages")
            .whereEqualTo("guestUid", user.uid)
            .get()
            .addOnSuccessListener { snapshots ->
                if (snapshots != null && !snapshots.isEmpty) {
                    val list = snapshots.documents.mapIndexed { idx, doc ->
                        PackageItem(
                            id = doc.id,
                            trackingNumber = doc.getString("trackingNumber") ?: "TRK-UNKNOWN",
                            arrivalDate = "Arrived: " + (doc.getString("arrivalDate") ?: "Recently"),
                            locker = doc.getString("locker") ?: "Locker --",
                            pin = doc.getString("pin") ?: "0000"
                        )
                    }
                    showList(list)
                } else {
                    showList(mockPackages)
                }
            }
            .addOnFailureListener {
                showList(mockPackages)
            }
    }

    private fun showList(list: List<PackageItem>) {
        if (!isAdded) return
        if (list.isEmpty()) {
            binding.rvPackages.visibility = View.GONE
            binding.emptyState.visibility = View.VISIBLE
        } else {
            binding.rvPackages.visibility = View.VISIBLE
            binding.emptyState.visibility = View.GONE
            adapter.submitList(list)
        }
    }

    private class PackageAdapter(private val onRevealPin: (PackageItem) -> Unit) : ListAdapter<PackageItem, PackageAdapter.ViewHolder>(DiffCallback) {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val binding = ItemPackageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ViewHolder(binding, onRevealPin)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(getItem(position))
        }

        class ViewHolder(private val binding: ItemPackageBinding, private val onRevealPin: (PackageItem) -> Unit) : RecyclerView.ViewHolder(binding.root) {
            fun bind(item: PackageItem) {
                binding.tvTrackingNumber.text = item.trackingNumber
                binding.tvArrivalDate.text = item.arrivalDate
                binding.chipLocker.text = item.locker
                binding.btnRevealPin.setOnClickListener { onRevealPin(item) }
            }
        }

        object DiffCallback : DiffUtil.ItemCallback<PackageItem>() {
            override fun areItemsTheSame(oldItem: PackageItem, newItem: PackageItem): Boolean = oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: PackageItem, newItem: PackageItem): Boolean = oldItem == newItem
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}