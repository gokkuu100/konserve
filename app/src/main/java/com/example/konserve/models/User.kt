package com.example.konserve.models

import kotlinx.serialization.Serializable

@Serializable
data class User(
    val user_id: String,
    val full_name: String? = null,
    val imageUrl: String? = null,
    val email: String = "",
    val reward_points: Int? = null,  // Changed from NumericType to Int
    val phone: String? = null,
    val gender: String? = null,
    val address: String? = null,
    val created_at: String,
    )