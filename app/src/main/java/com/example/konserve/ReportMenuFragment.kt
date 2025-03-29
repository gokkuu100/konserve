package com.example.konserve

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.util.*

class ReportMenuFragment : Fragment() {
    private lateinit var backButton: ImageView
    private lateinit var imageView: ImageView
    private lateinit var descriptionEditText: EditText
    private lateinit var locationEditText: EditText
    private lateinit var submitButton: Button
    private lateinit var captureImageButton: Button
    private var selectedImageUri: Uri? = null

    private lateinit var supabaseManager: SupabaseManager

    private val CAMERA_PERMISSION_CODE = 1000
    private val GALLERY_PERMISSION_CODE = 1001
    private val CAMERA_REQUEST_CODE = 1002
    private val GALLERY_REQUEST_CODE = 1003

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_report_menu, container, false)
        supabaseManager = SupabaseManager(requireContext())

        backButton = view.findViewById(R.id.backButton)
        imageView = view.findViewById(R.id.reportImageView)
        descriptionEditText = view.findViewById(R.id.descriptionEditText)
        locationEditText = view.findViewById(R.id.locationEditText)
        submitButton = view.findViewById(R.id.submitButton)
        captureImageButton = view.findViewById(R.id.captureImageButton)

        setupClickListeners()

        return view
    }

    private fun setupClickListeners() {
        backButton.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        captureImageButton.setOnClickListener {
            if (checkCameraPermission()) {
                openCamera()
            } else {
                requestPermissions(arrayOf(Manifest.permission.CAMERA), CAMERA_PERMISSION_CODE)
            }
        }

        imageView.setOnClickListener {
            if (checkGalleryPermission()) {
                openGallery()
            } else {
                requestPermissions(arrayOf(Manifest.permission.READ_MEDIA_IMAGES), GALLERY_PERMISSION_CODE)
            }
        }

        submitButton.setOnClickListener {
            submitReport()
        }
    }

    private fun checkCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun checkGalleryPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.READ_MEDIA_IMAGES
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun openCamera() {
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { intent ->
            startActivityForResult(intent, CAMERA_REQUEST_CODE)
        }
    }

    private fun openGallery() {
        Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI).also { intent ->
            startActivityForResult(intent, GALLERY_REQUEST_CODE)
        }
    }

    private fun submitReport() {
        val description = descriptionEditText.text.toString()
        val location = locationEditText.text.toString()

        if (description.isEmpty()) {
            descriptionEditText.error = "Description is required"
            return
        }
        if (location.isEmpty()) {
            locationEditText.error = "Location is required"
            return
        }
        if (selectedImageUri == null) {
            Toast.makeText(context, "Please select an image", Toast.LENGTH_SHORT).show()
            return
        }

        submitButton.isEnabled = false
        CoroutineScope(Dispatchers.IO).launch {
            val context = requireContext()
            val fileName = "report_${System.currentTimeMillis()}.jpg" // Generate a unique file name
            val imageBytes = supabaseManager.uriToByteArray(context, selectedImageUri!!)

            if (imageBytes != null) {
                val imageUrl = supabaseManager.uploadImage(
                    context,
                    fileName,
                    imageBytes
                ) // Pass file name and byte array
                if (imageUrl != null) {
                    val userId = supabaseManager.getCurrentUser()
                    val reportData = mapOf(
                        "description" to description,
                        "location" to location,
                        "imageUrl" to imageUrl,
                        "userId" to (userId ?: "Unknown"),
                        "timestamp" to System.currentTimeMillis()
                    )
                    val success = supabaseManager.saveReport(reportData)

                    withContext(Dispatchers.Main) {
                        submitButton.isEnabled = true
                        if (success) {
                            Toast.makeText(
                                context,
                                "Report submitted successfully",
                                Toast.LENGTH_SHORT
                            ).show()
                            parentFragmentManager.popBackStack()
                        } else {
                            Toast.makeText(context, "Failed to submit report", Toast.LENGTH_SHORT)
                                .show()
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        submitButton.isEnabled = true
                        Toast.makeText(context, "Failed to upload image", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                withContext(Dispatchers.Main) {
                    submitButton.isEnabled = true
                    Toast.makeText(context, "Failed to process image", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                CAMERA_REQUEST_CODE -> {
                    val imageBitmap = data?.extras?.get("data") as? Bitmap
                    imageView.setImageBitmap(imageBitmap)
                    selectedImageUri = getImageUri(imageBitmap)
                }
                GALLERY_REQUEST_CODE -> {
                    selectedImageUri = data?.data
                    imageView.setImageURI(selectedImageUri)
                }
            }
        }
    }

    private fun getImageUri(bitmap: Bitmap?): Uri? {
        val bytes = ByteArrayOutputStream()
        bitmap?.compress(Bitmap.CompressFormat.JPEG, 100, bytes)
        val path = MediaStore.Images.Media.insertImage(
            requireContext().contentResolver, bitmap, "Title", null
        )
        return Uri.parse(path)
    }
}
