package com.example.konserve

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.vision.v1.AnnotateImageRequest
import com.google.cloud.vision.v1.Feature
import com.google.cloud.vision.v1.Feature.Type
import com.google.cloud.vision.v1.Image
import com.google.cloud.vision.v1.ImageAnnotatorClient
import com.google.cloud.vision.v1.ImageAnnotatorSettings
import com.google.protobuf.ByteString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.IOException

import android.os.Build
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder

class VisionService(private val context: Context) {

    private val wasteCategories = mapOf(
        "glass" to listOf("glass", "bottle", "jar", "window", "mirror"),
        "metal" to listOf("metal", "can", "aluminum", "steel", "tin", "foil"),
        "plastic" to listOf("plastic", "bottle", "container", "packaging", "bag", "wrapper"),
        "paper/cardboard" to listOf("paper", "cardboard", "box", "newspaper", "magazine", "carton"),
        "textiles" to listOf("textile", "clothing", "fabric", "cloth", "garment", "shoe", "hat"),
        "food waste" to listOf("food", "fruit", "vegetable", "meat", "leftover", "organic")
    )

    suspend fun analyzeImage(imageUri: Uri): WasteAnalysisResult = withContext(Dispatchers.IO) {
        try {
            // Load image from URI and convert to bitmap
            val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(context.contentResolver, imageUri)
                ImageDecoder.decodeBitmap(source)
            } else {
                context.contentResolver.openInputStream(imageUri)?.use {
                    BitmapFactory.decodeStream(it)
                } ?: throw IOException("Failed to decode bitmap")
            }

            // Prepare image for Google Cloud Vision
            val byteArrayOutputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, byteArrayOutputStream)
            val imageBytes = byteArrayOutputStream.toByteArray()

            // Set up the Cloud Vision client
            val credentials = context.assets.open("credential.json").use {
                GoogleCredentials.fromStream(it)
            }

            val settings = ImageAnnotatorSettings.newBuilder()
                .setCredentialsProvider { credentials }
                .build()

            ImageAnnotatorClient.create(settings).use { client ->
                // Create the image annotation request
                val image = Image.newBuilder()
                    .setContent(ByteString.copyFrom(imageBytes))
                    .build()

                val features = listOf(
                    Feature.newBuilder().setType(Type.LABEL_DETECTION).setMaxResults(15).build(),
                    Feature.newBuilder().setType(Type.OBJECT_LOCALIZATION).setMaxResults(5).build()
                )

                val request = AnnotateImageRequest.newBuilder()
                    .setImage(image)
                    .addAllFeatures(features)
                    .build()

                val response = client.batchAnnotateImages(listOf(request))

                // Process annotation results
                val result = response.responsesList[0]

                // Extract labels from response
                val labels = result.labelAnnotationsList.map { it.description.lowercase() }
                val objects = result.localizedObjectAnnotationsList.map { it.name.lowercase() }

                // Combine all detected terms
                val allTerms = (labels + objects).distinct()

                // Classify waste based on detected terms
                val wasteType = classifyWaste(allTerms)

                // Create result object with waste type and disposal information
                WasteAnalysisResult(
                    wasteType = wasteType,
                    confidence = calculateConfidence(allTerms, wasteType),
                    detectedObjects = allTerms,
                    disposalInfo = getDisposalInfo(wasteType)
                )
            }
        } catch (e: IOException) {
            Log.e(TAG, "Error analyzing image", e)
            WasteAnalysisResult(
                wasteType = "Unknown",
                confidence = 0.0,
                detectedObjects = emptyList(),
                disposalInfo = "Unable to analyze image. Please try again."
            )
        }
    }

    private fun classifyWaste(detectedTerms: List<String>): String {
        val categoryScores = wasteCategories.mapValues { (_, keywords) ->
            keywords.count { keyword ->
                detectedTerms.any { it.contains(keyword) }
            }
        }

        return categoryScores.maxByOrNull { it.value }?.key ?: "Unknown"
    }

    private fun calculateConfidence(detectedTerms: List<String>, wasteType: String): Double {
        if (wasteType == "Unknown") return 0.0

        val relevantKeywords = wasteCategories[wasteType] ?: return 0.0
        val matchCount = relevantKeywords.count { keyword ->
            detectedTerms.any { it.contains(keyword) }
        }

        return (matchCount.toDouble() / relevantKeywords.size) * 100.0
    }

    private fun getDisposalInfo(wasteType: String): String {
        return when (wasteType) {
            "glass" -> "Glass should be rinsed clean and placed in glass recycling bins. " +
                    "Different colors of glass (clear, green, brown) may need to be separated depending on your local recycling guidelines. " +
                    "Glass can be recycled indefinitely without loss of quality."

            "metal" -> "Metal items should be clean and dry before recycling. " +
                    "Aluminum cans, steel cans, and foil are widely recyclable. " +
                    "Some metal items may contain valuable materials that can be recovered. " +
                    "Check with your local recycling facility for specific guidelines."

            "plastic" -> "Clean plastic items before recycling. " +
                    "Check the recycling number (1-7) on the bottom of plastic items to determine recyclability. " +
                    "Not all plastics can be recycled in all areas. " +
                    "Consider reducing plastic use by opting for reusable alternatives."

            "paper/cardboard" -> "Paper and cardboard should be clean and dry. " +
                    "Remove any plastic wrapping, tape, or metal fasteners. " +
                    "Flatten cardboard boxes to save space. " +
                    "Soiled paper with food residue typically cannot be recycled."

            "textiles" -> "Wearable textiles can be donated to charity shops or textile recycling centers. " +
                    "Worn-out textiles can be recycled into industrial rags or insulation. " +
                    "Some municipalities have special textile collection programs. " +
                    "Consider repairing or upcycling before disposal."

            "food waste" -> "Food waste can be composted at home or through municipal composting programs. " +
                    "Composting reduces methane emissions from landfills. " +
                    "Food waste can be turned into nutrient-rich soil amendment. " +
                    "Some areas offer separate food waste collection services."

            else -> "Unable to determine specific disposal information. " +
                    "Please check your local waste management guidelines for proper disposal instructions."
        }
    }

    companion object {
        private const val TAG = "VisionService"
    }
}

data class WasteAnalysisResult(
    val wasteType: String,
    val confidence: Double,
    val detectedObjects: List<String>,
    val disposalInfo: String
)