package com.example.ra.model

data class Host(
    val id: String,
    val name: String,
    val profilePicUrl: String,
    val rating: Double,
    val totalTrips: Int,
    val isSuperhost: Boolean = true,
    val joinedYear: String = "2023",
    val responseRate: String = "100%"
)
