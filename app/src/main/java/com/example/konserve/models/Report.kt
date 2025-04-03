package com.example.konserve.models

import kotlinx.serialization.Serializable

@Serializable
data class Report(
    var id: Int = 0,
    val title: String = "",
    val description: String = "",
    val imageUrl: String = "",
    val author: String? = null,
    val date: String
)
