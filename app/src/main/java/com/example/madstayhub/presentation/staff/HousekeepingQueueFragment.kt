package com.example.madstayhub.presentation.staff

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.madstayhub.R
import com.example.madstayhub.databinding.FragmentHousekeepingQueueBinding
import com.example.madstayhub.databinding.ItemInvoiceBinding
import com.example.madstayhub.databinding.ItemChecklistStepBinding
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class HousekeepingQueueFragment : Fragment() {
    private var _binding: FragmentHousekeepingQueueBinding? = null
    private val binding get() = _binding!!
    private val db = FirebaseFirestore.getInstance()
    private lateinit var adapter: HousekeepingAdapter
    private var listener: ListenerRegistration? = null

    data class HousekeepingTask(
        val id: String,
        val guestName: String,
        val roomNumber: String,
        val type: String,
        val status: String
    )

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHousekeepingQueueBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        adapter = HousekeepingAdapter { task ->
            showChecklistDialog(task)
        }
        binding.rvHousekeepingQueue.adapter = adapter

        listenToHousekeepingRequests()
    }

    private fun listenToHousekeepingRequests() {
        listener = db.collection("housekeeping_requests")
            .whereEqualTo("status", "pending")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null || !isAdded) return@addSnapshotListener
                
                val list = snapshot.documents.mapNotNull { doc ->
                    val room = doc.getString("roomNumber") ?: "N/A"
                    val name = doc.getString("guestName") ?: "Guest"
                    val type = doc.getString("type") ?: "General Cleaning"
                    val status = doc.getString("status") ?: "pending"
                    HousekeepingTask(doc.id, name, room, type, status)
                }
                
                adapter.submitList(list)
            }
    }

    private fun showChecklistDialog(task: HousekeepingTask) {
        val context = context ?: return
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_housekeeping_checklist, null)
        val dialog = AlertDialog.Builder(context)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        val tvTitle = dialogView.findViewById<TextView>(R.id.tvChecklistTitle)
        val pbProgress = dialogView.findViewById<ProgressBar>(R.id.pbChecklistProgress)
        val tvProgressText = dialogView.findViewById<TextView>(R.id.tvProgressText)
        val rvChecklist = dialogView.findViewById<RecyclerView>(R.id.rvChecklistItems)
        val btnCancel = dialogView.findViewById<Button>(R.id.btnCancelChecklist)
        val btnFinish = dialogView.findViewById<Button>(R.id.btnCompleteHK)

        tvTitle.text = "Room ${task.roomNumber} - ${task.type}"

        val steps = getChecklistSteps()
        val checkedState = BooleanArray(20) { false }

        val stepAdapter = ChecklistAdapter(steps, checkedState) { index, isChecked ->
            checkedState[index] = isChecked
            val count = checkedState.count { it }
            pbProgress.progress = count
            tvProgressText.text = "$count/20"
            btnFinish.isEnabled = (count == 20)
        }
        rvChecklist.adapter = stepAdapter

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        btnFinish.setOnClickListener {
            btnFinish.isEnabled = false
            db.collection("housekeeping_requests").document(task.id)
                .update("status", "completed")
                .addOnSuccessListener {
                    Toast.makeText(context, "Room ${task.roomNumber} marked clean!", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                }
                .addOnFailureListener { e ->
                    btnFinish.isEnabled = true
                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }

        dialog.show()
    }

    private fun getChecklistSteps(): List<String> {
        return listOf(
            "Strip all bed linens & pillowcases",
            "Remove dirty towels from bathroom",
            "Inspect room for guest personal items",
            "Empty trash cans and replace liners",
            "Dust all hard surfaces & furniture",
            "Sanitize telephone & TV remote control",
            "Wipe down mirrors and glass surfaces",
            "Vacuum carpets and sweep/mop floors",
            "Sanitize toilet bowl, seat, and handle",
            "Clean and scrub shower/bathtub area",
            "Clean bathroom sink and vanity",
            "Restock fresh bath towels & hand towels",
            "Replenish soap, shampoo, and lotion",
            "Put fresh sheets and pillowcases on bed",
            "Inspect and dust window blinds and sills",
            "Restock coffee maker, tea bags, and mugs",
            "Verify TV, AC, and lights work properly",
            "Check and restock water bottles",
            "Wipe down closet shelves",
            "Spray deodorizer and lock door"
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        listener?.remove()
        _binding = null
    }

    private class HousekeepingAdapter(
        private val onItemClick: (HousekeepingTask) -> Unit
    ) : ListAdapter<HousekeepingTask, HousekeepingAdapter.ViewHolder>(DiffCallback) {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val binding = ItemInvoiceBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(getItem(position))
        }

        inner class ViewHolder(private val binding: ItemInvoiceBinding) : RecyclerView.ViewHolder(binding.root) {
            fun bind(task: HousekeepingTask) {
                binding.tvInvoiceTitle.text = "Room ${task.roomNumber} (${task.guestName})"
                binding.tvInvoiceId.text = task.type
                binding.tvInvoiceAmount.text = "Start"
                binding.tvInvoiceAmount.setTextColor(android.graphics.Color.parseColor("#4CAF50"))
                binding.root.setOnClickListener { onItemClick(task) }
            }
        }

        object DiffCallback : DiffUtil.ItemCallback<HousekeepingTask>() {
            override fun areItemsTheSame(oldItem: HousekeepingTask, newItem: HousekeepingTask): Boolean = oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: HousekeepingTask, newItem: HousekeepingTask): Boolean = oldItem == newItem
        }
    }

    private class ChecklistAdapter(
        private val steps: List<String>,
        private val checkedState: BooleanArray,
        private val onCheckChanged: (Int, Boolean) -> Unit
    ) : RecyclerView.Adapter<ChecklistAdapter.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val binding = ItemChecklistStepBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(position, steps[position])
        }

        override fun getItemCount(): Int = steps.size

        inner class ViewHolder(private val binding: ItemChecklistStepBinding) : RecyclerView.ViewHolder(binding.root) {
            fun bind(position: Int, step: String) {
                binding.tvStepName.text = step
                binding.cbStep.setOnCheckedChangeListener(null)
                binding.cbStep.isChecked = checkedState[position]
                binding.cbStep.setOnCheckedChangeListener { _, isChecked ->
                    onCheckChanged(position, isChecked)
                }
            }
        }
    }
}