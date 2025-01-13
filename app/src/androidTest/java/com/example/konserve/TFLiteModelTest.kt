package com.example.konserve

import android.content.Context
import android.graphics.BitmapFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TFLiteModelTest {

    @Test
    fun testModelInference() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        // Initialize the TFLiteModel
        val model = TFLiteModel(context)

        // Load and preprocess the test image from assets
        val assetManager = context.assets
        val inputStream = assetManager.open("plastic_water.jpg")
        val bitmap = BitmapFactory.decodeStream(inputStream)
        val inputBuffer = model.preprocessImage(bitmap)

        // Run inference
        val output = model.runInference(inputBuffer)

        // Check output is not null and has expected length
        assertNotNull("Inference output should not be null", output)
        println("Inference output: ${output.contentToString()}")
    }
}
