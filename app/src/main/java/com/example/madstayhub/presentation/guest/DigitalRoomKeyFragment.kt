package com.example.madstayhub.presentation.guest

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.madstayhub.databinding.FragmentDigitalRoomKeyBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class DigitalRoomKeyFragment : Fragment() {
    private var _binding: FragmentDigitalRoomKeyBinding? = null
    private val binding get() = _binding!!
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentDigitalRoomKeyBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        val user = auth.currentUser
        if (user != null) {
            db.collection("users").document(user.uid).get()
                .addOnSuccessListener { doc ->
                    if (isAdded && doc != null) {
                        val room = doc.getString("roomNumber") ?: "None"
                        val name = doc.getString("name") ?: "Guest"
                        
                        if (room == "None" || room.isBlank()) {
                            binding.toolbar.title = "Digital Room Key"
                            binding.tvRoomNumber.text = "Pending Assignment"
                            binding.tvGuestName.text = "Guest: $name"
                            binding.ivQrKey.setImageDrawable(resources.getDrawable(android.R.drawable.ic_dialog_alert, null))
                            binding.ivQrKey.setPadding(64, 64, 64, 64)
                        } else {
                            binding.toolbar.title = "Room $room Key"
                            binding.tvRoomNumber.text = "Room $room"
                            binding.tvGuestName.text = "Guest: $name"
                            
                            val qrData = "STAYHUB_KEY_UID:${user.uid}_ROOM:$room"
                            val qrBitmap = generateMockQrCode(qrData)
                            binding.ivQrKey.setImageBitmap(qrBitmap)
                            binding.ivQrKey.setPadding(0, 0, 0, 0)
                        }
                    }
                }
        }
    }

    private fun generateMockQrCode(data: String): android.graphics.Bitmap {
        val size = 512
        val bitmap = android.graphics.Bitmap.createBitmap(size, size, android.graphics.Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        val paint = android.graphics.Paint()
        
        // Background white
        paint.color = android.graphics.Color.WHITE
        canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), paint)
        
        // Draw standard QR-like corner position patterns (3 big squares)
        paint.color = android.graphics.Color.BLACK
        
        // Top-Left
        canvas.drawRect(40f, 40f, 160f, 160f, paint)
        paint.color = android.graphics.Color.WHITE
        canvas.drawRect(60f, 60f, 140f, 140f, paint)
        paint.color = android.graphics.Color.BLACK
        canvas.drawRect(80f, 80f, 120f, 120f, paint)
        
        // Top-Right
        canvas.drawRect(size - 160f, 40f, size - 40f, 160f, paint)
        paint.color = android.graphics.Color.WHITE
        canvas.drawRect(size - 140f, 60f, size - 60f, 140f, paint)
        paint.color = android.graphics.Color.BLACK
        canvas.drawRect(size - 120f, 80f, size - 80f, 120f, paint)
        
        // Bottom-Left
        canvas.drawRect(40f, size - 160f, 160f, size - 40f, paint)
        paint.color = android.graphics.Color.WHITE
        canvas.drawRect(60f, size - 140f, 140f, size - 60f, paint)
        paint.color = android.graphics.Color.BLACK
        canvas.drawRect(80f, size - 120f, 120f, size - 80f, paint)
        
        // Draw random QR noise
        val random = java.util.Random(data.hashCode().toLong())
        val cellSize = 20
        paint.color = android.graphics.Color.BLACK
        for (x in 40 until size - 40 step cellSize) {
            for (y in 40 until size - 40 step cellSize) {
                // Skip corner areas
                if (x < 180 && y < 180) continue
                if (x > size - 180 && y < 180) continue
                if (x < 180 && y > size - 180) continue
                
                if (random.nextBoolean()) {
                    canvas.drawRect(x.toFloat(), y.toFloat(), (x + cellSize).toFloat(), (y + cellSize).toFloat(), paint)
                }
            }
        }
        return bitmap
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}