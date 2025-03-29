package com.example.konserve.models

import java.time.Instant

data class Report(
    var id: String = "",
    val title: String = "",
    val description: String = "",
    val imageUrl: String = "",
    val author: String? = null,
    val date: Instant = Instant.now()
)
