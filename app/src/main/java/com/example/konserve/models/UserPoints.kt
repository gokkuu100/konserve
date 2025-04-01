package com.example.konserve.models

import kotlinx.serialization.Serializable

@Serializable
data class UserPoints(
    val user_id: String,
    val points: Double
)
