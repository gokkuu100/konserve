package com.example.konserve

data class VisionResponse(
    val responses: List<LabelResponse>?
)

data class LabelResponse(
    val labelAnnotations: List<LabelAnnotation>?
)

data class LabelAnnotation(
    val description: String?,
    val score: Float?
)
