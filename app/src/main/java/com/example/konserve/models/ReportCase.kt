package com.example.konserve.models
import kotlinx.serialization.Serializable

@Serializable
data class ReportCase (
    val description: String,
    val location: String,
    val imageUrl: String,
    val user_id: String,
    val timestamp: String
)