package com.example.ra.model

enum class BookingStatus(val label: String) {
    PENDING("Pending Host Approval"),
    CONFIRMED("Confirmed"),
    CANCELLED("Cancelled"),
    COMPLETED("Completed")
}

data class BookingRequest(
    val id: String,
    val car: Car,
    val renterName: String,
    val startDate: String,
    val endDate: String,
    val totalDays: Int,
    val totalPriceJmd: Double,
    val status: BookingStatus = BookingStatus.PENDING
)
