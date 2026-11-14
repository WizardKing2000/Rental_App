package com.example.ra.model

enum class TransmissionType(val label: String) {
    AUTOMATIC("Automatic"),
    MANUAL("Manual")
}

enum class FuelType(val label: String) {
    GASOLINE("Gasoline"),
    HYBRID("Hybrid"),
    ELECTRIC("Electric")
}

data class Car(
    val id: String,
    val make: String,
    val model: String,
    val year: Int,
    val parish: Parish,
    val pricePerDayJmd: Double,
    val transmission: TransmissionType = TransmissionType.AUTOMATIC,
    val fuelType: FuelType = FuelType.GASOLINE,
    val seats: Int = 5,
    val imageUrl: String,
    val galleryUrls: List<String> = emptyList(),
    val host: Host,
    val rating: Double = 4.9,
    val tripCount: Int = 18,
    val features: List<String> = listOf("Air Conditioning", "Bluetooth", "Tinted Windows", "Backup Camera"),
    val description: String = "Clean, reliable, and fuel-efficient vehicle ready for your trip across Jamaica."
) {
    val title: String get() = "$year $make $model"
    val priceFormattedJmd: String get() = "$${String.format("%,.0f", pricePerDayJmd)} JMD/day"
    val estimatedUsdPrice: String get() = "~$${(pricePerDayJmd / 155.0).toInt()} USD/day"
}
