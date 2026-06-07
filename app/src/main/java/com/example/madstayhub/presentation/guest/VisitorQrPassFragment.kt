package com.example.madstayhub.presentation.guest

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.madstayhub.R
import com.example.madstayhub.databinding.FragmentVisitorQrPassBinding

class VisitorQrPassFragment : Fragment() {
    private var _binding: FragmentVisitorQrPassBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentVisitorQrPassBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        val visitorName = arguments?.getString("visitorName") ?: "Visitor"
        val expectedTime = arguments?.getString("expectedTime") ?: ""
        val roomNumber = arguments?.getString("roomNumber") ?: ""
        val hostName = arguments?.getString("hostName") ?: ""

        binding.tvVisitorNameHeader.text = "Guest Pass: $visitorName"
        binding.tvVisitorTime.text = "Expected: $expectedTime"
        
        val qrData = "VISITOR:${visitorName}_HOST:${hostName}_ROOM:${roomNumber}_TIME:${expectedTime}"
        val qrBitmap = generateMockQrCode(qrData)
        binding.ivVisitorQr.setImageBitmap(qrBitmap)

        binding.btnSharePass.setOnClickListener {
            try {
                val cachePath = java.io.File(requireContext().cacheDir, "images")
                cachePath.mkdirs()
                val stream = java.io.FileOutputStream("$cachePath/visitor_qr.png")
                qrBitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, stream)
                stream.close()

                val imageFile = java.io.File(cachePath, "visitor_qr.png")
                val contentUri = androidx.core.content.FileProvider.getUriForFile(
                    requireContext(),
                    "${requireContext().packageName}.fileprovider",
                    imageFile
                )

                if (contentUri != null) {
                    val shareIntent = android.content.Intent().apply {
                        action = android.content.Intent.ACTION_SEND
                        addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        setDataAndType(contentUri, requireContext().contentResolver.getType(contentUri))
                        putExtra(android.content.Intent.EXTRA_STREAM, contentUri)
                    }
                    startActivity(android.content.Intent.createChooser(shareIntent, "Share Guest Pass"))
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Sharing failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnBackToDashboard.setOnClickListener {
            findNavController().popBackStack(R.id.nav_home, false)
        }
    }

    private fun generateMockQrCode(data: String): android.graphics.Bitmap {
        val size = 512
        val bitmap = android.graphics.Bitmap.createBitmap(size, size, android.graphics.Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        val paint = android.graphics.Paint()
        
        paint.color = android.graphics.Color.WHITE
        canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), paint)
        
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
        
        val random = java.util.Random(data.hashCode().toLong())
        val cellSize = 20
        paint.color = android.graphics.Color.BLACK
        for (x in 40 until size - 40 step cellSize) {
            for (y in 40 until size - 40 step cellSize) {
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