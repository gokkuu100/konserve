package com.example.konserve

import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.konserve.databinding.FragmentAiCameraBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.*
import retrofit2.converter.gson.GsonConverterFactory
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.*

class AICameraFragment : Fragment() {

    private var _binding: FragmentAiCameraBinding? = null
    private val binding get() = _binding!!
    private var imageCapture: ImageCapture? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAiCameraBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        startCamera()

        binding.captureButton.setOnClickListener {
            takePhoto()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.viewFinder.surfaceProvider)
            }

            imageCapture = ImageCapture.Builder().build()

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)
            } catch (exc: Exception) {
                Log.e("CameraFragment", "Use case binding failed", exc)
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun takePhoto() {
        val imageCapture = imageCapture ?: return

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(System.currentTimeMillis()))
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
        }

        val outputOptions = ImageCapture.OutputFileOptions.Builder(
            requireContext().contentResolver,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues
        ).build()

        imageCapture.takePicture(outputOptions, ContextCompat.getMainExecutor(requireContext()),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    val savedUri = outputFileResults.savedUri
                    savedUri?.let { sendImageToAI(it) }
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e("CameraFragment", "Photo capture failed: ${exception.message}", exception)
                }
            })
    }

    private fun sendImageToAI(imageUri: Uri) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val bitmap = getResizedBitmap(imageUri)
                val base64Image = encodeImageToBase64(bitmap)

                val requestBody = mapOf(
                    "requests" to listOf(
                        mapOf(
                            "image" to mapOf("content" to base64Image),
                            "features" to listOf(mapOf("type" to "LABEL_DETECTION"))
                        )
                    )
                )

                val retrofit = Retrofit.Builder()
                    .baseUrl("https://vision.googleapis.com/")
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()

                val service = retrofit.create(GoogleVisionAPI::class.java)
                val call = service.analyzeImage(requestBody, getString(R.string.google_cloud_api_key))

                call.enqueue(object : Callback<VisionResponse> {
                    override fun onResponse(call: Call<VisionResponse>, response: Response<VisionResponse>) {
                        if (response.isSuccessful) {
                            val labels = response.body()?.responses?.firstOrNull()?.labelAnnotations ?: emptyList()
                            classifyWaste(labels)
                        } else {
                            Log.e("AIModel", "API Response Error: ${response.errorBody()?.string()}")
                        }
                    }

                    override fun onFailure(call: Call<VisionResponse>, t: Throwable) {
                        Log.e("AIModel", "Error: ${t.message}")
                    }
                })
            } catch (e: Exception) {
                Log.e("AIModel", "Error processing image: ${e.message}")
            }
        }
    }

    private fun getResizedBitmap(imageUri: Uri): Bitmap {
        val source = ImageDecoder.createSource(requireContext().contentResolver, imageUri)
        val bitmap = ImageDecoder.decodeBitmap(source)
        return Bitmap.createScaledBitmap(bitmap, 640, 640, true) // Reduce size for performance
    }

    private fun encodeImageToBase64(bitmap: Bitmap): String {
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream) // Optimize compression
        return Base64.encodeToString(byteArrayOutputStream.toByteArray(), Base64.NO_WRAP)
    }

    private fun classifyWaste(labels: List<LabelAnnotation>) {
        val categories = mapOf(
            "food-waste" to listOf("food", "fruit", "vegetable", "bread", "meat"),
            "paper-cardboard" to listOf("paper", "cardboard", "book", "newspaper"),
            "glass" to listOf("glass", "bottle", "jar"),
            "metal" to listOf("metal", "can", "aluminum", "steel"),
            "plastic" to listOf("plastic", "bottle", "bag"),
            "textiles" to listOf("clothing", "fabric", "cloth", "textile")
        )

        var detectedWaste = "Unknown"
        for (label in labels) {
            label.description?.let { description ->  // Ensure description is not null
                for ((category, keywords) in categories) {
                    if (keywords.any { description.contains(it, ignoreCase = true) }) {
                        detectedWaste = category
                        break
                    }
                }
            }
        }

        val disposalInfo = getDisposalInstructions(detectedWaste)

        requireActivity().runOnUiThread {
            AlertDialog.Builder(requireContext())
                .setTitle("Waste Identified: $detectedWaste")
                .setMessage("Disposal Instructions:\n$disposalInfo")
                .setPositiveButton("OK", null)
                .show()
        }
    }

    private fun getDisposalInstructions(wasteType: String): String {
        return when (wasteType) {
            "food-waste" -> "Compost or use organic waste bins."
            "paper-cardboard" -> "Recycle in a dry paper bin."
            "glass" -> "Recycle in a glass bin. Avoid breaking it."
            "metal" -> "Recycle in a metal bin or scrap yard."
            "plastic" -> "Recycle in a plastic bin. Avoid single-use plastics."
            "textiles" -> "Donate or recycle at textile collection centers."
            else -> "No specific disposal instructions found."
        }
    }
}
