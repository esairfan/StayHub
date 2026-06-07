package com.example.madstayhub.presentation.guest

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.madstayhub.R
import com.example.madstayhub.databinding.FragmentGuestDashboardBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class GuestDashboardFragment : Fragment() {

    private var _binding: FragmentGuestDashboardBinding? = null
    private val binding get() = _binding!!
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGuestDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        setupDashboardTiles()
        fetchGuestDetails()

        binding.btnSOS.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_emergencySos)
        }
    }

    private fun setupDashboardTiles() {
        // 1. Room Key
        binding.tileRoomKey.apply {
            tvTileTitle.text = getString(R.string.room_key)
            ivTileIcon.setImageResource(android.R.drawable.ic_lock_idle_lock)
            root.setOnClickListener { findNavController().navigate(R.id.action_home_to_roomKey) }
        }

        // 2. Room Service
        binding.tileService.apply {
            tvTileTitle.text = getString(R.string.room_service)
            ivTileIcon.setImageResource(android.R.drawable.ic_menu_today)
            root.setOnClickListener { findNavController().navigate(R.id.action_home_to_roomService) }
        }

        // 3. AI Concierge
        binding.tileConcierge.apply {
            tvTileTitle.text = getString(R.string.ai_concierge)
            ivTileIcon.setImageResource(android.R.drawable.ic_menu_help)
            root.setOnClickListener { findNavController().navigate(R.id.nav_concierge) }
        }

        // 4. Rent & Utilities
        binding.tileRent.apply {
            tvTileTitle.text = getString(R.string.rent)
            ivTileIcon.setImageResource(android.R.drawable.ic_menu_agenda)
            root.setOnClickListener { findNavController().navigate(R.id.action_home_to_rent) }
        }

        // 5. Housekeeping
        binding.tileHousekeeping.apply {
            tvTileTitle.text = getString(R.string.housekeeping)
            ivTileIcon.setImageResource(android.R.drawable.ic_menu_edit)
            root.setOnClickListener { findNavController().navigate(R.id.action_home_to_housekeeping) }
        }

        // 6. Laundry
        binding.tileLaundry.apply {
            tvTileTitle.text = getString(R.string.laundry)
            ivTileIcon.setImageResource(android.R.drawable.stat_notify_sync)
            root.setOnClickListener { findNavController().navigate(R.id.action_home_to_laundry) }
        }

        // 7. Amenities
        binding.tileAmenities.apply {
            tvTileTitle.text = getString(R.string.amenity_booking)
            ivTileIcon.setImageResource(android.R.drawable.ic_menu_camera)
            root.setOnClickListener { findNavController().navigate(R.id.action_home_to_amenities) }
        }

        // 8. Wallet
        binding.tileWallet.apply {
            tvTileTitle.text = "Wallet"
            ivTileIcon.setImageResource(android.R.drawable.ic_menu_save)
            root.setOnClickListener { findNavController().navigate(R.id.paymentWalletFragment) }
        }

        // 9. Community
        binding.tileCommunity.apply {
            tvTileTitle.text = getString(R.string.community)
            ivTileIcon.setImageResource(android.R.drawable.stat_notify_chat)
            root.setOnClickListener { findNavController().navigate(R.id.action_home_to_community) }
        }

        // 10. Visitors
        binding.tileVisitors.apply {
            tvTileTitle.text = getString(R.string.visitors)
            ivTileIcon.setImageResource(android.R.drawable.ic_menu_myplaces)
            root.setOnClickListener { findNavController().navigate(R.id.action_home_to_visitors) }
        }

        // 11. Mood Control (Emoji)
        binding.tileMood.apply {
            tvTileTitle.text = getString(R.string.mood_control)
            ivTileIcon.visibility = View.GONE
            tvTileEmoji.visibility = View.VISIBLE
            tvTileEmoji.text = "🎭"
            root.setOnClickListener { findNavController().navigate(R.id.action_home_to_mood) }
        }

        // 12. Packages
        binding.tilePackages.apply {
            tvTileTitle.text = getString(R.string.packages)
            ivTileIcon.setImageResource(android.R.drawable.ic_menu_send)
            root.setOnClickListener { findNavController().navigate(R.id.action_home_to_packages) }
        }
    }

    private var userListener: com.google.firebase.firestore.ListenerRegistration? = null

    private fun fetchGuestDetails() {
        val user = auth.currentUser ?: return
        
        userListener = db.collection("users").document(user.uid)
            .addSnapshotListener { document, error ->
                if (error != null || document == null || !isAdded) return@addSnapshotListener
                
                val name = document.getString("name") ?: "Guest"
                binding.tvWelcomeGuest.text = getString(R.string.welcome_guest_param, name)
                
                val roomNumber = document.getString("roomNumber") ?: ""
                val isVerified = document.getBoolean("isVerified") ?: false
                
                if (roomNumber.isEmpty()) {
                    if (!isVerified) {
                        binding.tvRoomNumber.text = "KYC Upload Required"
                        binding.tvResidencyPlan.text = "Please upload ID documents to verify account"
                        binding.cardActiveStatus.setCardBackgroundColor(android.graphics.Color.parseColor("#D32F2F"))
                        binding.btnDetails.visibility = View.VISIBLE
                        binding.btnDetails.text = "Verify"
                        binding.btnDetails.setOnClickListener {
                            findNavController().navigate(R.id.kycUploadFragment)
                        }
                    } else {
                        binding.tvRoomNumber.text = "Pending Assignment"
                        binding.tvResidencyPlan.text = "Verification approved. Awaiting room allocation."
                        binding.cardActiveStatus.setCardBackgroundColor(android.graphics.Color.parseColor("#F57C00"))
                        binding.btnDetails.visibility = View.GONE
                    }
                } else {
                    binding.tvRoomNumber.text = "Room $roomNumber - Active"
                    binding.tvResidencyPlan.text = "Premium Residency Plan"
                    binding.cardActiveStatus.setCardBackgroundColor(android.graphics.Color.parseColor("#1A237E"))
                    binding.btnDetails.visibility = View.VISIBLE
                    binding.btnDetails.text = "View Key"
                    binding.btnDetails.setOnClickListener {
                        findNavController().navigate(R.id.digitalRoomKeyFragment)
                    }
                }
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        userListener?.remove()
        _binding = null
    }
}
