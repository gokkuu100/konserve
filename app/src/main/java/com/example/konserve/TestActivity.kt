package com.example.konserve

import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity

class TestActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        // Initialize the model
        val model = TFLiteModel(this)

        // Load and preprocess the test image from assets
        val assetManager = assets
        val inputStream = assetManager.open("test_image.jpg")
        val bitmap = BitmapFactory.decodeStream(inputStream)
        val inputBuffer = model.preprocessImage(bitmap)

        // Run inference
        val output = model.runInference(inputBuffer)

        // Log or display the results
        Log.d("TFLiteModel", "Inference output: ${output.contentToString()}")
    }
}
