package com.example.madstayhub.presentation.guest

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.example.madstayhub.MainActivity
import com.example.madstayhub.databinding.FragmentKycUploadBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.UploadTask
import androidx.navigation.fragment.findNavController

class KycUploadFragment : Fragment() {

    private var _binding: FragmentKycUploadBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var storage: FirebaseStorage
    
    private var selectedImageUri: Uri? = null

    private val getContent = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            binding.ivKycPreview.setImageURI(it)
            binding.ivKycPreview.setPadding(0, 0, 0, 0)
            binding.ivKycPreview.imageTintList = null // Clear XML tint so actual image shows
            binding.btnSubmitKyc.isEnabled = true
        }
    }

    private val requestCameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            try {
                takePicturePreview.launch(null)
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to launch camera: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "Camera permission is required to take photos", Toast.LENGTH_SHORT).show()
        }
    }

    private val takePicturePreview = registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        if (bitmap != null) {
            binding.ivKycPreview.setImageBitmap(bitmap)
            binding.ivKycPreview.setPadding(0, 0, 0, 0)
            binding.ivKycPreview.imageTintList = null // Clear XML tint so actual image shows
            binding.btnSubmitKyc.isEnabled = true
            
            try {
                // Save bitmap to a temporary local file so we have a Uri to upload
                val tempFile = java.io.File.createTempFile("kyc_temp", ".jpg", requireContext().cacheDir)
                tempFile.outputStream().use { out ->
                    bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, out)
                }
                selectedImageUri = Uri.fromFile(tempFile)
            } catch (e: Exception) {
                Toast.makeText(context, "Error saving photo: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "No image captured from camera", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentKycUploadBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()

        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnChooseGallery.setOnClickListener {
            getContent.launch("image/*")
        }

        binding.btnTakePhoto.setOnClickListener {
            if (androidx.core.content.ContextCompat.checkSelfPermission(
                    requireContext(),
                    android.Manifest.permission.CAMERA
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                try {
                    takePicturePreview.launch(null)
                } catch (e: Exception) {
                    Toast.makeText(context, "Failed to launch camera: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            } else {
                requestCameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
            }
        }

        binding.btnSubmitKyc.setOnClickListener {
            uploadKycImage()
        }
    }

    private fun uploadKycImage() {
        val uri = selectedImageUri
        if (uri == null) {
            Toast.makeText(context, "Please select or take an image first", Toast.LENGTH_SHORT).show()
            return
        }
        val uid = auth.currentUser?.uid
        if (uid == null) {
            Toast.makeText(context, "User session not found. Please log in again.", Toast.LENGTH_SHORT).show()
            return
        }

        binding.pbUpload.visibility = View.VISIBLE
        binding.btnSubmitKyc.isEnabled = false

        try {
            val bytes = requireContext().contentResolver.openInputStream(uri)?.use { it.readBytes() }
            if (bytes == null) {
                handleFailure(Exception("Could not read image bytes"))
                return
            }
            
            // Compress and scale the image to fit under Firestore document limits
            val bitmap = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            if (bitmap == null) {
                handleFailure(Exception("Could not decode image files. Try another image."))
                return
            }
            val outputStream = java.io.ByteArrayOutputStream()
            val scaledBitmap = if (bitmap.width > 600) {
                val aspectRatio = bitmap.height.toDouble() / bitmap.width.toDouble()
                android.graphics.Bitmap.createScaledBitmap(bitmap, 600, (600 * aspectRatio).toInt(), true)
            } else {
                bitmap
            }
            scaledBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 70, outputStream)
            val compressedBytes = outputStream.toByteArray()
            
            // Convert to Base64 data URL
            val base64String = android.util.Base64.encodeToString(compressedBytes, android.util.Base64.DEFAULT)
            val dataUrl = "data:image/jpeg;base64,$base64String"
            
            updateUserVerification(dataUrl)
        } catch (e: Exception) {
            handleFailure(e)
        }
    }

    private fun updateUserVerification(url: String) {
        val uid = auth.currentUser?.uid ?: return
        val updates = hashMapOf<String, Any>(
            "kycDocUrl" to url,
            "isVerified" to false,
            "isKycPending" to true
        )

        db.collection("users").document(uid)
            .set(updates, com.google.firebase.firestore.SetOptions.merge())
            .addOnSuccessListener {
                if (isAdded) {
                    binding.pbUpload.visibility = View.GONE
                    Toast.makeText(context, "Verification Submitted!", Toast.LENGTH_SHORT).show()
                    (activity as? MainActivity)?.navigateToGuestFlow()
                }
            }
            .addOnFailureListener { e ->
                handleFailure(e)
            }
    }

    private fun handleFailure(e: Exception) {
        if (isAdded) {
            binding.pbUpload.visibility = View.GONE
            binding.btnSubmitKyc.isEnabled = true
            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
