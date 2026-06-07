package com.example.madstayhub.presentation.staff

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.madstayhub.R
import com.example.madstayhub.databinding.FragmentStaffAmenitiesBinding
import com.example.madstayhub.databinding.ItemStaffAmenityBinding
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class StaffAmenitiesFragment : Fragment() {
    private var _binding: FragmentStaffAmenitiesBinding? = null
    private val binding get() = _binding!!
    private val db = FirebaseFirestore.getInstance()
    private lateinit var adapter: AmenitiesAdapter
    private var listener: ListenerRegistration? = null

    data class AmenityItem(
        val id: String,
        val name: String,
        val hours: String
    )

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentStaffAmenitiesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        adapter = AmenitiesAdapter(
            onItemClick = { item -> showAmenityDialog(item) },
            onDeleteClick = { item -> confirmDelete(item) }
        )
        binding.rvAmenities.adapter = adapter

        binding.fabAddAmenity.setOnClickListener {
            showAmenityDialog(null)
        }

        listenToAmenities()
    }

    private fun listenToAmenities() {
        listener = db.collection("amenities")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null || !isAdded) return@addSnapshotListener
                
                val list = snapshot.documents.mapNotNull { doc ->
                    val name = doc.getString("name") ?: return@mapNotNull null
                    val hours = doc.getString("hours") ?: "00:00 - 00:00"
                    AmenityItem(doc.id, name, hours)
                }

                if (list.isEmpty()) {
                    binding.tvEmptyState.visibility = View.VISIBLE
                    binding.rvAmenities.visibility = View.GONE
                } else {
                    binding.tvEmptyState.visibility = View.GONE
                    binding.rvAmenities.visibility = View.VISIBLE
                }

                adapter.submitList(list)
            }
    }

    private fun showAmenityDialog(item: AmenityItem?) {
        val context = context ?: return
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_add_amenity, null)
        val dialog = AlertDialog.Builder(context)
            .setView(dialogView)
            .create()

        val tilName = dialogView.findViewById<TextInputLayout>(R.id.tilAmenityName)
        val etName = dialogView.findViewById<EditText>(R.id.etAmenityName)
        val tilHours = dialogView.findViewById<TextInputLayout>(R.id.tilAmenityHours)
        val etHours = dialogView.findViewById<EditText>(R.id.etAmenityHours)
        val btnSave = dialogView.findViewById<Button>(R.id.btnSave)
        val btnCancel = dialogView.findViewById<Button>(R.id.btnCancel)

        if (item != null) {
            etName.setText(item.name)
            etHours.setText(item.hours)
        }

        btnSave.setOnClickListener {
            val name = etName.text.toString().trim()
            val hours = etHours.text.toString().trim()

            if (name.isEmpty()) {
                tilName.error = "Name required"
                return@setOnClickListener
            } else {
                tilName.error = null
            }

            if (hours.isEmpty()) {
                tilHours.error = "Timings required"
                return@setOnClickListener
            } else {
                tilHours.error = null
            }

            btnSave.isEnabled = false
            val data = hashMapOf(
                "name" to name,
                "hours" to hours
            )

            val task = if (item == null) {
                db.collection("amenities").document().set(data)
            } else {
                db.collection("amenities").document(item.id).set(data)
            }

            task.addOnSuccessListener {
                Toast.makeText(context, "Amenity Saved Successfully!", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }.addOnFailureListener { e ->
                btnSave.isEnabled = true
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun confirmDelete(item: AmenityItem) {
        val context = context ?: return
        AlertDialog.Builder(context)
            .setTitle("Delete Amenity")
            .setMessage("Are you sure you want to delete ${item.name}?")
            .setPositiveButton("Delete") { _, _ ->
                db.collection("amenities").document(item.id).delete()
                    .addOnSuccessListener {
                        Toast.makeText(context, "Amenity Deleted", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        listener?.remove()
        _binding = null
    }

    private class AmenitiesAdapter(
        private val onItemClick: (AmenityItem) -> Unit,
        private val onDeleteClick: (AmenityItem) -> Unit
    ) : ListAdapter<AmenityItem, AmenitiesAdapter.ViewHolder>(DiffCallback) {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val binding = ItemStaffAmenityBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(getItem(position))
        }

        inner class ViewHolder(private val binding: ItemStaffAmenityBinding) : RecyclerView.ViewHolder(binding.root) {
            fun bind(item: AmenityItem) {
                binding.tvAmenityName.text = item.name
                binding.tvAmenityHours.text = item.hours

                binding.root.setOnClickListener { onItemClick(item) }
                binding.btnDeleteAmenity.setOnClickListener { onDeleteClick(item) }
            }
        }

        object DiffCallback : DiffUtil.ItemCallback<AmenityItem>() {
            override fun areItemsTheSame(oldItem: AmenityItem, newItem: AmenityItem): Boolean = oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: AmenityItem, newItem: AmenityItem): Boolean = oldItem == newItem
        }
    }
}
