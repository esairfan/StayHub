package com.example.madstayhub.presentation.staff

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.madstayhub.databinding.FragmentStaffAnnouncementsBinding
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Date

class StaffAnnouncementsFragment : Fragment() {
    private var _binding: FragmentStaffAnnouncementsBinding? = null
    private val binding get() = _binding!!
    private val db = FirebaseFirestore.getInstance()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentStaffAnnouncementsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnBroadcast.setOnClickListener {
            submitAnnouncement()
        }
    }

    private fun submitAnnouncement() {
        val title = binding.etAnnounceTitle.text.toString().trim()
        val body = binding.etAnnounceBody.text.toString().trim()

        if (title.isEmpty()) {
            binding.tilAnnounceTitle.error = "Title required"
            return
        } else {
            binding.tilAnnounceTitle.error = null
        }

        if (body.isEmpty()) {
            binding.tilAnnounceBody.error = "Details required"
            return
        } else {
            binding.tilAnnounceBody.error = null
        }

        binding.btnBroadcast.isEnabled = false

        // Expiry timestamp set to 7 days for general broadcasts to give guests enough time to view it
        val expiryTime = Date(System.currentTimeMillis() + (7L * 24 * 60 * 60 * 1000))

        val announcement = hashMapOf(
            "guestUid" to "all",
            "title" to title,
            "body" to body,
            "timestamp" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
            "expiryTimestamp" to expiryTime,
            "type" to "broadcast"
        )

        db.collection("announcements").document()
            .set(announcement)
            .addOnSuccessListener {
                if (isAdded) {
                    Toast.makeText(context, "Announcement Broadcasted!", Toast.LENGTH_SHORT).show()
                    findNavController().navigateUp()
                }
            }
            .addOnFailureListener { e ->
                if (isAdded) {
                    binding.btnBroadcast.isEnabled = true
                    Toast.makeText(context, "Failed to broadcast: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
