package com.example.madstayhub.presentation.staff

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.madstayhub.R
import com.example.madstayhub.databinding.FragmentStaffKycBinding
import com.example.madstayhub.databinding.ItemInvoiceBinding
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class StaffKycFragment : Fragment() {

    private var _binding: FragmentStaffKycBinding? = null
    private val binding get() = _binding!!
    private val db = FirebaseFirestore.getInstance()
    private lateinit var adapter: KycAdapter
    private var listener: ListenerRegistration? = null

    data class KycRequest(
        val uid: String,
        val name: String,
        val email: String,
        val kycDocUrl: String
    )

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentStaffKycBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        adapter = KycAdapter { request ->
            showKycReviewDialog(request)
        }
        binding.rvKycRequests.adapter = adapter

        listenForKycRequests()
    }

    private fun listenForKycRequests() {
        listener = db.collection("users")
            .whereEqualTo("isKycPending", true)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null || !isAdded) return@addSnapshotListener
                
                val list = mutableListOf<KycRequest>()
                for (doc in snapshot.documents) {
                    val uid = doc.id
                    val name = doc.getString("name") ?: "Guest"
                    val email = doc.getString("email") ?: ""
                    val url = doc.getString("kycDocUrl") ?: ""
                    list.add(KycRequest(uid, name, email, url))
                }

                if (list.isEmpty()) {
                    binding.tvEmptyState.visibility = View.VISIBLE
                    binding.rvKycRequests.visibility = View.GONE
                } else {
                    binding.tvEmptyState.visibility = View.GONE
                    binding.rvKycRequests.visibility = View.VISIBLE
                }

                adapter.submitList(list)
            }
    }

    private fun showKycReviewDialog(request: KycRequest) {
        val context = context ?: return
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_kyc_review, null)
        val dialog = AlertDialog.Builder(context)
            .setView(dialogView)
            .create()

        val tvName = dialogView.findViewById<TextView>(R.id.tvKycGuestName)
        val tvEmail = dialogView.findViewById<TextView>(R.id.tvKycGuestEmail)
        val ivDoc = dialogView.findViewById<ImageView>(R.id.ivKycDocImage)
        val etRoom = dialogView.findViewById<EditText>(R.id.etAllotRoomNumber)
        val btnApprove = dialogView.findViewById<Button>(R.id.btnApproveKyc)
        val btnCancel = dialogView.findViewById<Button>(R.id.btnCancelKyc)

        tvName.text = request.name
        tvEmail.text = request.email

        if (request.kycDocUrl.isNotEmpty()) {
            ivDoc.loadImageFromUrl(request.kycDocUrl)
        }

        btnApprove.setOnClickListener {
            val roomNumber = etRoom.text.toString().trim()
            if (roomNumber.isEmpty()) {
                Toast.makeText(context, "Please enter a room number", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            btnApprove.isEnabled = false
            val updates = hashMapOf<String, Any>(
                "isVerified" to true,
                "isKycPending" to false,
                "roomNumber" to roomNumber
            )

            db.collection("users").document(request.uid)
                .update(updates)
                .addOnSuccessListener {
                    Toast.makeText(context, "Guest Verified & Room Allotted!", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                }
                .addOnFailureListener { e ->
                    btnApprove.isEnabled = true
                    Toast.makeText(context, "Failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun ImageView.loadImageFromUrl(url: String) {
        if (url.startsWith("data:image/")) {
            try {
                val base64Data = url.substringAfter(",")
                val decodedBytes = android.util.Base64.decode(base64Data, android.util.Base64.DEFAULT)
                val bitmap = android.graphics.BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                this.setImageBitmap(bitmap)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return
        }
        val executor = java.util.concurrent.Executors.newSingleThreadExecutor()
        val handler = android.os.Handler(android.os.Looper.getMainLooper())
        executor.execute {
            try {
                val inStream = java.net.URL(url).openStream()
                val bitmap = android.graphics.BitmapFactory.decodeStream(inStream)
                handler.post {
                    this.setImageBitmap(bitmap)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private class KycAdapter(private val onClick: (KycRequest) -> Unit) : ListAdapter<KycRequest, KycAdapter.ViewHolder>(DiffCallback) {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val binding = ItemInvoiceBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(getItem(position))
        }

        inner class ViewHolder(private val binding: ItemInvoiceBinding) : RecyclerView.ViewHolder(binding.root) {
            fun bind(item: KycRequest) {
                binding.tvInvoiceTitle.text = item.name
                binding.tvInvoiceId.text = item.email
                binding.tvInvoiceAmount.text = "Review ID"
                binding.tvInvoiceAmount.setTextColor(android.graphics.Color.parseColor("#1A237E")) // primary blue
                binding.root.setOnClickListener {
                    onClick(item)
                }
            }
        }

        object DiffCallback : DiffUtil.ItemCallback<KycRequest>() {
            override fun areItemsTheSame(oldItem: KycRequest, newItem: KycRequest): Boolean = oldItem.uid == newItem.uid
            override fun areContentsTheSame(oldItem: KycRequest, newItem: KycRequest): Boolean = oldItem == newItem
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        listener?.remove()
        _binding = null
    }
}
