package com.example.konserve.models

import kotlinx.serialization.Serializable

@Serializable
data class Message(
    val id: Int,
    val timestamp: String,  // Use String if ISO format, otherwise LocalDateTime
    val username: String,
    val text: String,
    val user_id: String
)
