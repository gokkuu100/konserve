package com.example.konserve.models

import com.google.firebase.Timestamp

data class Report(
    var id: String = "",
    val title: String = "",
    val description: String = "",
    val imageUrl: String = "",
    val date: Timestamp = Timestamp.now()
) 