package com.example.konserve

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query

interface GoogleVisionAPI {
    @POST("v1/images:annotate")
    fun analyzeImage(
        @Body requestBody: Map<String, Any>,
        @Query("key") apiKey: String
    ): Call<VisionResponse>
}
