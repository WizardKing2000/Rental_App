package com.example.ra

import kotlin.math.*

internal data class GeoPoint(val latitude: Double, val longitude: Double) {
    init { require(latitude.isFinite() && longitude.isFinite() && latitude in -90.0..90.0 && longitude in -180.0..180.0) }
    companion object {
        fun from(latitude: Double, longitude: Double): GeoPoint? =
            runCatching { GeoPoint(latitude, longitude) }.getOrNull()
    }
}

internal data class NearbyCar(val car: RentalCar, val distanceKm: Double)

internal object NearbySearch {
    const val RADIUS_KM = 25.0

    fun distanceKm(from: GeoPoint, to: GeoPoint): Double {
        val lat = Math.toRadians(to.latitude - from.latitude)
        val lon = Math.toRadians(to.longitude - from.longitude)
        val a = sin(lat / 2).pow(2) + cos(Math.toRadians(from.latitude)) *
            cos(Math.toRadians(to.latitude)) * sin(lon / 2).pow(2)
        return 6371.0088 * 2 * asin(sqrt(a.coerceIn(0.0, 1.0)))
    }

    fun find(cars: List<RentalCar>, origin: GeoPoint, query: String = "",
             unavailable: Set<String> = emptySet(), radiusKm: Double = RADIUS_KM): List<NearbyCar> {
        require(radiusKm.isFinite() && radiusKm > 0)
        return cars.asSequence()
            .filter { it.ownerId.isNotBlank() && it.id !in unavailable && ParishSearch.matches(it, query) }
            .mapNotNull { car -> car.pickup?.let { NearbyCar(car, distanceKm(origin, it)) } }
            .filter { it.distanceKm <= radiusKm }
            .sortedWith(compareBy<NearbyCar> { it.distanceKm }.thenBy { it.car.id })
            .toList()
    }
}
