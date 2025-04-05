package com.example.konserve.models
import kotlinx.serialization.Serializable

@Serializable
data class Feedback (
    val timestamp: String,
    val user_id: String,
    val feedback: String
)