package com.example.konserve

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException
import kotlin.math.sqrt

class VisionService(private val context: Context) {
    private val client = OkHttpClient()
    
    companion object {
        private const val VISION_API_URL = "https://vision.googleapis.com/v1/images:annotate"
        private const val API_KEY = "YOUR_API_KEY_HERE" // Replace with your API key
        private const val MAX_IMAGE_SIZE = 1024 * 1024 // 1MB
        private const val TAG = "VisionService"
    }

    enum class WasteType {
        PLASTIC,
        PAPER_CARDBOARD,
        GLASS,
        METAL,
        TEXTILE,
        ORGANIC,
        UNKNOWN
    }

    data class ClassificationResult(
        val wasteType: WasteType,
        val confidence: Float,
        val labels: List<String>
    )

    suspend fun classifyImage(imageUri: Uri): ClassificationResult {
        return withContext(Dispatchers.IO) {
            try {
                val bitmap = loadAndResizeBitmap(imageUri) ?: Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
                val base64Image = bitmapToBase64(bitmap)
                val response = sendVisionRequest(base64Image)
                processResponse(response)
            } catch (e: Exception) {
                Log.e(TAG, "Error classifying image", e)
                ClassificationResult(WasteType.UNKNOWN, 0f, emptyList())
            }
        }
    }

    private fun loadAndResizeBitmap(uri: Uri): Bitmap? {
        // Get original dimensions
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        context.contentResolver.openInputStream(uri).use {
            BitmapFactory.decodeStream(it, null, options)
        }

        var width = options.outWidth
        var height = options.outHeight

        // Calculate scaling factor
        var scaleFactor = 1
        while ((width * height * 4) > MAX_IMAGE_SIZE) {
            width /= 2
            height /= 2
            scaleFactor *= 2
        }

        // Load scaled bitmap
        return context.contentResolver.openInputStream(uri).use {
            BitmapFactory.Options().apply {
                inSampleSize = scaleFactor
            }.let { scaledOptions ->
                BitmapFactory.decodeStream(it, null, scaledOptions)
            }
        }
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
        val bytes = outputStream.toByteArray()
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    private fun sendVisionRequest(base64Image: String): String {
        val requestJson = JSONObject().apply {
            put("requests", JSONArray().apply {
                put(JSONObject().apply {
                    put("image", JSONObject().apply {
                        put("content", base64Image)
                    })
                    put("features", JSONArray().apply {
                        put(JSONObject().apply {
                            put("type", "LABEL_DETECTION")
                            put("maxResults", 20)
                        })
                    })
                })
            })
        }.toString()

        val request = Request.Builder()
            .url("$VISION_API_URL?key=$API_KEY")
            .post(requestJson.toRequestBody("application/json".toMediaTypeOrNull()))
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Unexpected response ${response.code}")
            return response.body?.string() ?: throw IOException("Empty response body")
        }
    }

    private fun processResponse(jsonResponse: String): ClassificationResult {
        val response = JSONObject(jsonResponse)
        val labels = mutableListOf<Pair<String, Float>>()

        // Extract labels and scores
        response.getJSONArray("responses").getJSONObject(0)
            .getJSONArray("labelAnnotations")
            .let { annotations ->
                for (i in 0 until annotations.length()) {
                    annotations.getJSONObject(i).let {
                        labels.add(
                            it.getString("description") to it.getDouble("score").toFloat()
                        )
                    }
                }
            }

        // Define waste type keywords
        val wasteTypeKeywords = mapOf(
            WasteType.PLASTIC to listOf(
                "plastic", "bottle", "container", "polymer", 
                "polyethylene", "polypropylene", "pvc"
            ),
            WasteType.PAPER_CARDBOARD to listOf(
                "paper", "cardboard", "carton", "box", 
                "newspaper", "magazine", "book"
            ),
            WasteType.GLASS to listOf(
                "glass", "bottle", "jar", "window", "mirror"
            ),
            WasteType.METAL to listOf(
                "metal", "aluminum", "tin", "steel", 
                "can", "foil"
            ),
            WasteType.TEXTILE to listOf(
                "textile", "fabric", "cloth", "clothing", 
                "cotton", "wool", "polyester"
            ),
            WasteType.ORGANIC to listOf(
                "food", "vegetable", "fruit", "meat", 
                "organic", "plant", "leaf", "wood"
            )
        )

        // Calculate scores for each waste type
        val scores = wasteTypeKeywords.mapValues { (_, keywords) ->
            labels.sumOf { (label, score) ->
                if (keywords.any { keyword -> 
                    label.lowercase().contains(keyword.lowercase()) 
                }) {
                    score.toDouble()
                } else 0.0
            }.toFloat()
        }

        // Find the waste type with highest score
        val (bestType, bestScore) = scores.maxByOrNull { it.value }
            ?.let { it.key to it.value }
            ?: (WasteType.UNKNOWN to 0f)

        return ClassificationResult(
            wasteType = if (bestScore > 0.3f) bestType else WasteType.UNKNOWN,
            confidence = bestScore,
            labels = labels.map { it.first }
        )
    }
}