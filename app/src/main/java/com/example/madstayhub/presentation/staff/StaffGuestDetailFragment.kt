package com.example.madstayhub.presentation.staff

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.madstayhub.databinding.FragmentStaffGuestDetailBinding
import com.example.madstayhub.databinding.ItemInvoiceBinding
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class StaffGuestDetailFragment : Fragment() {

    private var _binding: FragmentStaffGuestDetailBinding? = null
    private val binding get() = _binding!!
    private val db = FirebaseFirestore.getInstance()

    private lateinit var ordersAdapter: OrdersHistoryAdapter
    private lateinit var paymentsAdapter: PaymentsHistoryAdapter
    private lateinit var visitorsAdapter: VisitorsHistoryAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentStaffGuestDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val guestUid = arguments?.getString("guestUid") ?: ""
        val guestName = arguments?.getString("guestName") ?: "Guest"

        binding.toolbar.title = "History: $guestName"
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        setupRecyclerViews()
        loadGuestHistory(guestUid)
    }

    private fun setupRecyclerViews() {
        ordersAdapter = OrdersHistoryAdapter()
        binding.rvDetailOrders.adapter = ordersAdapter

        paymentsAdapter = PaymentsHistoryAdapter()
        binding.rvDetailPayments.adapter = paymentsAdapter

        visitorsAdapter = VisitorsHistoryAdapter()
        binding.rvDetailVisitors.adapter = visitorsAdapter
    }

    private fun loadGuestHistory(uid: String) {
        if (uid.isEmpty()) return

        // 1. Load Orders
        db.collection("room_service_orders")
            .whereEqualTo("guestUid", uid)
            .get()
            .addOnSuccessListener { snapshot ->
                if (!isAdded) return@addOnSuccessListener
                val orders = snapshot.documents.mapNotNull { doc ->
                    val id = doc.id.take(8).uppercase()
                    val total = doc.getDouble("totalAmount") ?: 0.0
                    val date = doc.getDate("timestamp")
                    val items = doc.get("items") as? List<Map<String, Any>>
                    val itemNames = items?.joinToString { itemMap ->
                        val name = itemMap["itemName"] as? String ?: "Item"
                        val qty = (itemMap["quantity"] as? Number)?.toInt() ?: 1
                        "$name x$qty"
                    } ?: "Room Service Order"
                    OrderRecord(id, itemNames, total, date)
                }.sortedByDescending { it.date ?: Date(0) }

                if (orders.isEmpty()) {
                    binding.tvEmptyOrders.visibility = View.VISIBLE
                    binding.rvDetailOrders.visibility = View.GONE
                } else {
                    binding.tvEmptyOrders.visibility = View.GONE
                    binding.rvDetailOrders.visibility = View.VISIBLE
                }
                ordersAdapter.submitList(orders)
            }

        // 2. Load Payments (Room Service Orders + Rent)
        db.collection("users").document(uid).get()
            .addOnSuccessListener { userDoc ->
                if (!isAdded) return@addOnSuccessListener
                val list = mutableListOf<PaymentRecord>()
                
                val rentPaid = userDoc.getBoolean("isRentPaid") ?: false
                if (rentPaid) {
                    list.add(PaymentRecord("RENT", "Monthly Rent Payment", 45000.0, Date()))
                }

                // Query orders for payments too
                db.collection("room_service_orders")
                    .whereEqualTo("guestUid", uid)
                    .get()
                    .addOnSuccessListener { ordersSnapshot ->
                        for (doc in ordersSnapshot.documents) {
                            val id = doc.id.take(8).uppercase()
                            val total = doc.getDouble("totalAmount") ?: 0.0
                            val date = doc.getDate("timestamp")
                            list.add(PaymentRecord("RS-$id", "Room Service payment", total, date))
                        }

                        list.sortByDescending { it.date ?: Date(0) }
                        if (list.isEmpty()) {
                            binding.tvEmptyPayments.visibility = View.VISIBLE
                            binding.rvDetailPayments.visibility = View.GONE
                        } else {
                            binding.tvEmptyPayments.visibility = View.GONE
                            binding.rvDetailPayments.visibility = View.VISIBLE
                        }
                        paymentsAdapter.submitList(list)
                    }
            }

        // 3. Load Visitors
        db.collection("visitor_registrations")
            .whereEqualTo("guestUid", uid)
            .get()
            .addOnSuccessListener { snapshot ->
                if (!isAdded) return@addOnSuccessListener
                val visitors = snapshot.documents.mapNotNull { doc ->
                    val name = doc.getString("visitorName") ?: ""
                    val date = doc.getString("date") ?: ""
                    val time = doc.getString("time") ?: ""
                    val phone = doc.getString("visitorPhone") ?: ""
                    VisitorRecord(name, phone, date, time)
                }

                if (visitors.isEmpty()) {
                    binding.tvEmptyVisitors.visibility = View.VISIBLE
                    binding.rvDetailVisitors.visibility = View.GONE
                } else {
                    binding.tvEmptyVisitors.visibility = View.GONE
                    binding.rvDetailVisitors.visibility = View.VISIBLE
                }
                visitorsAdapter.submitList(visitors)
            }
    }

    data class OrderRecord(val id: String, val title: String, val amount: Double, val date: Date?)
    data class PaymentRecord(val id: String, val title: String, val amount: Double, val date: Date?)
    data class VisitorRecord(val visitorName: String, val visitorPhone: String, val date: String, val time: String)

    private class OrdersHistoryAdapter : ListAdapter<OrderRecord, OrdersHistoryAdapter.ViewHolder>(DiffCallbackOrders) {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val binding = ItemInvoiceBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ViewHolder(binding)
        }
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(getItem(position))
        }
        class ViewHolder(private val binding: ItemInvoiceBinding) : RecyclerView.ViewHolder(binding.root) {
            fun bind(record: OrderRecord) {
                binding.tvInvoiceTitle.text = record.title
                val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
                val formattedDate = record.date?.let { sdf.format(it) } ?: "Date Unknown"
                binding.tvInvoiceId.text = "Order ID: #${record.id} • $formattedDate"
                binding.tvInvoiceAmount.text = "Rs. ${record.amount.toInt()}"
                binding.tvInvoiceAmount.setTextColor(android.graphics.Color.parseColor("#1A237E"))
            }
        }
        object DiffCallbackOrders : DiffUtil.ItemCallback<OrderRecord>() {
            override fun areItemsTheSame(oldItem: OrderRecord, newItem: OrderRecord): Boolean = oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: OrderRecord, newItem: OrderRecord): Boolean = oldItem == newItem
        }
    }

    private class PaymentsHistoryAdapter : ListAdapter<PaymentRecord, PaymentsHistoryAdapter.ViewHolder>(DiffCallbackPayments) {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val binding = ItemInvoiceBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ViewHolder(binding)
        }
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(getItem(position))
        }
        class ViewHolder(private val binding: ItemInvoiceBinding) : RecyclerView.ViewHolder(binding.root) {
            fun bind(record: PaymentRecord) {
                binding.tvInvoiceTitle.text = record.title
                val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
                val formattedDate = record.date?.let { sdf.format(it) } ?: "Date Unknown"
                binding.tvInvoiceId.text = "Transaction: #${record.id} • $formattedDate"
                binding.tvInvoiceAmount.text = "Rs. ${record.amount.toInt()}"
                binding.tvInvoiceAmount.setTextColor(android.graphics.Color.parseColor("#4CAF50"))
            }
        }
        object DiffCallbackPayments : DiffUtil.ItemCallback<PaymentRecord>() {
            override fun areItemsTheSame(oldItem: PaymentRecord, newItem: PaymentRecord): Boolean = oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: PaymentRecord, newItem: PaymentRecord): Boolean = oldItem == newItem
        }
    }

    private class VisitorsHistoryAdapter : ListAdapter<VisitorRecord, VisitorsHistoryAdapter.ViewHolder>(DiffCallbackVisitors) {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val binding = ItemInvoiceBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ViewHolder(binding)
        }
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(getItem(position))
        }
        class ViewHolder(private val binding: ItemInvoiceBinding) : RecyclerView.ViewHolder(binding.root) {
            fun bind(record: VisitorRecord) {
                binding.tvInvoiceTitle.text = "${record.visitorName} (${record.visitorPhone})"
                binding.tvInvoiceId.text = "Expected Arrival: ${record.date} at ${record.time}"
                binding.tvInvoiceAmount.text = "Pre-Reg"
                binding.tvInvoiceAmount.setTextColor(android.graphics.Color.parseColor("#FF9800"))
            }
        }
        object DiffCallbackVisitors : DiffUtil.ItemCallback<VisitorRecord>() {
            override fun areItemsTheSame(oldItem: VisitorRecord, newItem: VisitorRecord): Boolean = oldItem.visitorName == newItem.visitorName
            override fun areContentsTheSame(oldItem: VisitorRecord, newItem: VisitorRecord): Boolean = oldItem == newItem
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
