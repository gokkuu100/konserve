package com.example.konserve.models
import kotlinx.serialization.Serializable

@Serializable
data class RedeemedCode(
    val user_id: String,
    val code: String,
    val points: Int,
    val redeemed_at: String
)