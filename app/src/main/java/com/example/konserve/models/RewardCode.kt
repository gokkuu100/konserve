package com.example.konserve.models

import kotlinx.serialization.Serializable

@Serializable
data class RewardCode(
    val code: String,
    val points: Int,
    val expires_at: String,
    val is_active: Boolean
)