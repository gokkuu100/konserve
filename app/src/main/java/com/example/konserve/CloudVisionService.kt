package com.example.konserve

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.util.Log
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.mapbox.core.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.math.sqrt

class CloudVisionService(private val context: Context) {

    companion object {
        private const val TAG = "CloudVisionService"
        private const val MAX_IMAGE_SIZE = 1024 * 1024 // 1MB
        private const val VISION_API_URL = "https://vision.googleapis.com/v1/images:annotate"
        private const val API_KEY = "5"
    }


    data class WasteClassification(
        val wasteType: WasteType,
        val confidence: Float
    )

    enum class WasteType {
        PLASTIC,
        PAPER_CARDBOARD,
        GLASS,
        METAL,
        TEXTILE,
        ORGANIC,
        UNKNOWN
    }

    // REST API request classes
    private data class AnnotateImageRequest(
        @SerializedName("image") val image: Image,
        @SerializedName("features") val features: List<Feature>
    )

    private data class Image(
        @SerializedName("content") val content: String
    )

    private data class Feature(
        @SerializedName("type") val type: String,
        @SerializedName("maxResults") val maxResults: Int
    )

    private data class BatchAnnotateImagesRequest(
        @SerializedName("requests") val requests: List<AnnotateImageRequest>
    )

    // REST API response classes
    private data class BatchAnnotateImagesResponse(
        @SerializedName("responses") val responses: List<AnnotateImageResponse>
    )

    private data class AnnotateImageResponse(
        @SerializedName("labelAnnotations") val labelAnnotations: List<EntityAnnotation>?
    )

    private data class EntityAnnotation(
        @SerializedName("description") val description: String,
        @SerializedName("score") val score: Float
    )

    suspend fun classifyWasteImage(imageUri: Uri): WasteClassification {
        return withContext(Dispatchers.IO) {
            try {
                // Load and resize the image
                val bitmap = loadAndResizeBitmap(imageUri)
                
                // Convert bitmap to base64
                val base64Image = convertBitmapToBase64(bitmap)
                
                // Create Vision API request
                val request = createAnnotateImageRequest(base64Image)
                
                // Execute request
                val response = executeRequest(request)
                
                // Process results to determine waste type
                if (response.responses.isNotEmpty() && response.responses[0].labelAnnotations != null) {
                    determineWasteType(response.responses[0].labelAnnotations ?: emptyList())
                } else {
                    WasteClassification(WasteType.UNKNOWN, 0.0f)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error classifying image", e)
                WasteClassification(WasteType.UNKNOWN, 0.0f)
            }
        }
    }

    private fun loadAndResizeBitmap(imageUri: Uri): Bitmap {
        val inputStream = context.contentResolver.openInputStream(imageUri)
        val originalBitmap = BitmapFactory.decodeStream(inputStream)
        inputStream?.close()
        
        // Resize if needed
        val width = originalBitmap.width
        val height = originalBitmap.height
        
        if (width * height * 4 > MAX_IMAGE_SIZE) {
            val scaleFactor = sqrt(MAX_IMAGE_SIZE.toDouble() / (width * height * 4))
            val newWidth = (width * scaleFactor).toInt()
            val newHeight = (height * scaleFactor).toInt()
            return Bitmap.createScaledBitmap(originalBitmap, newWidth, newHeight, true)
        }
        
        return originalBitmap
    }
    
    private fun convertBitmapToBase64(bitmap: Bitmap): String {
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, byteArrayOutputStream)
        val imageBytes = byteArrayOutputStream.toByteArray()
        return Base64.encodeToString(imageBytes, Base64.NO_WRAP)
    }
    
    private fun createAnnotateImageRequest(base64Image: String): BatchAnnotateImagesRequest {
        val feature = Feature("LABEL_DETECTION", 20)
        val image = Image(base64Image)
        val request = AnnotateImageRequest(image, listOf(feature))
        return BatchAnnotateImagesRequest(listOf(request))
    }
    
    private fun executeRequest(request: BatchAnnotateImagesRequest): BatchAnnotateImagesResponse {
        val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
        
        val gson = Gson()
        val requestJson = gson.toJson(request)
        
        val requestBody = requestJson.toRequestBody("application/json".toMediaTypeOrNull())
        
        val httpRequest = Request.Builder()
            .url("$VISION_API_URL?key=$API_KEY")
            .post(requestBody)
            .build()
        
        val response = client.newCall(httpRequest).execute()
        
        if (!response.isSuccessful) {
            throw IOException("Unexpected response code: ${response.code}")
        }
        
        val responseBody = response.body?.string() ?: throw IOException("Empty response")
        return gson.fromJson(responseBody, BatchAnnotateImagesResponse::class.java)
    }
    
    private fun determineWasteType(labels: List<EntityAnnotation>): WasteClassification {
        // Keywords for each waste type
        val wasteTypeKeywords = mapOf(
            WasteType.PLASTIC to listOf("plastic", "bottle", "container", "polymer", "polyethylene", "polypropylene", "pvc"),
            WasteType.PAPER_CARDBOARD to listOf("paper", "cardboard", "carton", "box", "newspaper", "magazine", "book"),
            WasteType.GLASS to listOf("glass", "bottle", "jar", "window", "mirror"),
            WasteType.METAL to listOf("metal", "aluminum", "tin", "steel", "can", "foil"),
            WasteType.TEXTILE to listOf("textile", "fabric", "cloth", "clothing", "cotton", "wool", "polyester"),
            WasteType.ORGANIC to listOf("food", "vegetable", "fruit", "meat", "organic", "plant", "leaf", "wood")
        )
        
        // Score each waste type based on labels
        val scores = mutableMapOf<WasteType, Float>()
        for (wasteType in WasteType.entries) {
            if (wasteType != WasteType.UNKNOWN) {
                scores[wasteType] = 0.0f
            }
        }
        
        for (label in labels) {
            val description = label.description.lowercase()
            val score = label.score
            
            for ((wasteType, keywords) in wasteTypeKeywords) {
                for (keyword in keywords) {
                    if (description.contains(keyword)) {
                        scores[wasteType] = scores[wasteType]!! + score
                        break
                    }
                }
            }
        }
        
        // Find the waste type with the highest score
        var highestScore = 0.0f
        var bestWasteType = WasteType.UNKNOWN
        
        for ((wasteType, score) in scores) {
            if (score > highestScore) {
                highestScore = score
                bestWasteType = wasteType
            }
        }
        
        // Return unknown if confidence is too low
        return if (highestScore < 0.3f) {
            WasteClassification(WasteType.UNKNOWN, highestScore)
        } else {
            WasteClassification(bestWasteType, highestScore)
        }
    }
} 