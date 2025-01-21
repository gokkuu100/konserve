package com.example.konserve

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

class AICameraFragment : Fragment() {

    private lateinit var tflite: Interpreter
    private lateinit var resultTextView: TextView
    private lateinit var previewView: PreviewView
    private lateinit var captureButton: Button
    private lateinit var loadingSpinner: ProgressBar

    private val classNames = listOf(
        "aerosol_cans", "aluminum_food_cans", "aluminum_soda_cans", "cardboard_boxes", "cardboard_packaging", "clothing",
        "coffee_grounds", "disposable_plastic_cutlery", "eggshells", "food_waste", "glass_beverage_bottles", "glass_cosmetic_containers",
        "glass_food_jars", "magazines", "newspaper", "office_paper", "paper_cups", "plastic_cup_lids",
        "plastic_detergent_bottles", "plastic_food_containers", "plastic_shopping_bags", "plastic_soda_bottles", "plastic_straws", "plastic_trash_bags",
        "plastic_water_bottles", "shoes", "steel_food_cans", "styrofoam_cups", "styrofoam_food_containers", "tea_bags",

    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tflite = Interpreter(loadModelFile(""))
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.camera_fragment, container, false)

        previewView = view.findViewById(R.id.previewView)
        captureButton = view.findViewById(R.id.captureButton)
        loadingSpinner = view.findViewById(R.id.loadingSpinner)
        resultTextView = view.findViewById(R.id.resultTextView)

        captureButton.setOnClickListener {
            takePicture()
        }

        startCamera()
        return view
    }

    private fun loadModelFile(modelFile: String): MappedByteBuffer {
        val assetFileDescriptor = requireActivity().assets.openFd(modelFile)
        val inputStream = FileInputStream(assetFileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = assetFileDescriptor.startOffset
        val declaredLength = assetFileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            cameraProvider.bindToLifecycle(this, cameraSelector, preview)
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun takePicture() {
        captureButton.isEnabled = false
        loadingSpinner.visibility = View.VISIBLE

        // Capture the image
        val imageCapture = ImageCapture.Builder().build()
        imageCapture.takePicture(ContextCompat.getMainExecutor(requireContext()), object : ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(imageProxy: ImageProxy) {
                val bitmap = imageProxy.toBitmap()
                imageProxy.close()

                GlobalScope.launch(Dispatchers.Main) {
                    classifyImage(bitmap)
                }
            }

            override fun onError(exception: ImageCaptureException) {
                Log.e("AICameraFragment", "Photo capture failed: ${exception.message}", exception)
                captureButton.isEnabled = true
                loadingSpinner.visibility = View.GONE
            }
        })
    }

    private suspend fun classifyImage(bitmap: Bitmap) {
        withContext(Dispatchers.Default) {
            val input = preprocessImage(bitmap)
            val output = Array(1) { FloatArray(classNames.size) } // Model output is a single integer

            tflite.run(input, output)
            val classIndex = output[0].indices.maxByOrNull { output[0][it] } ?: -1

            withContext(Dispatchers.Main) {
                displayResult(classNames[classIndex])
            }
        }
    }

    private fun displayResult(className: String) {
        loadingSpinner.visibility = View.GONE
        resultTextView.visibility = View.VISIBLE
        resultTextView.text = "Class: $className"
        captureButton.isEnabled = true
    }

    private fun preprocessImage(bitmap: Bitmap): ByteBuffer {
        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, 224, 224, true)
        val byteBuffer = ByteBuffer.allocateDirect(4 * 224 * 224 * 3) // Float buffer for RGB
        byteBuffer.order(ByteOrder.nativeOrder())

        for (y in 0 until 224) {
            for (x in 0 until 224) {
                val pixel = scaledBitmap.getPixel(x, y)

                // Normalize pixel values to [0, 1]
                byteBuffer.putFloat((pixel shr 16 and 0xFF) / 255f)
                byteBuffer.putFloat((pixel shr 8 and 0xFF) / 255f)
                byteBuffer.putFloat((pixel and 0xFF) / 255f)
            }
        }
        return byteBuffer
    }

    private fun ImageProxy.toBitmap(): Bitmap {
        val buffer = planes[0].buffer
        val bytes = ByteArray(buffer.capacity())
        buffer.get(bytes)
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, null)
    }
}
