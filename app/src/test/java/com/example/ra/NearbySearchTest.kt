package com.example.ra

import org.junit.Assert.*
import org.junit.Test

class NearbySearchTest {
    private val kingston = GeoPoint(17.9712, -76.7936)
    private fun car(id: String, point: GeoPoint?, active: Boolean = true) =
        RentalCar(id, "Toyota $id", "Kingston", 8000, active = active, ownerId = "owner", pickup = point)

    @Test fun distanceIsMeasuredInKilometres() {
        assertEquals(0.0, NearbySearch.distanceKm(kingston, kingston), 0.00001)
        assertEquals(111.195, NearbySearch.distanceKm(GeoPoint(0.0, 0.0), GeoPoint(1.0, 0.0)), 0.01)
        assertEquals(NearbySearch.distanceKm(kingston, GeoPoint(18.0, -77.0)),
            NearbySearch.distanceKm(GeoPoint(18.0, -77.0), kingston), 0.00001)
    }

    @Test fun nearbySortsByDistanceAcrossParishBoundaries() {
        val close = car("close", GeoPoint(17.98, -76.79)).copy(parish = "St. Andrew")
        val far = car("far", GeoPoint(18.05, -76.8))
        val outside = car("outside", GeoPoint(18.47, -77.92))
        val found = NearbySearch.find(listOf(far, outside, close), kingston)
        assertEquals(listOf("close", "far"), found.map { it.car.id })
        assertTrue(found[0].distanceKm < found[1].distanceKm)
    }

    @Test fun missingLocationPausedBookedAndSampleCarsAreExcluded() {
        val available = car("available", kingston)
        val booked = car("booked", kingston)
        val missing = car("missing", null)
        val paused = car("paused", kingston, active = false)
        val sample = car("sample", kingston).copy(ownerId = "")
        assertEquals(listOf("available"), NearbySearch.find(
            listOf(available, booked, missing, paused, sample), kingston,
            unavailable = setOf("booked")).map { it.car.id })
    }

    @Test fun radiusAndTextSearchAreRespected() {
        val car = car("Axio", GeoPoint(18.0, -76.79))
        assertEquals(1, NearbySearch.find(listOf(car), kingston, query = "axio").size)
        assertTrue(NearbySearch.find(listOf(car), kingston, query = "Honda").isEmpty())
        assertTrue(NearbySearch.find(listOf(car), kingston, radiusKm = 0.1).isEmpty())
    }

    @Test fun invalidCoordinatesAreNeverUsed() {
        assertNull(GeoPoint.from(Double.NaN, 0.0))
        assertNull(GeoPoint.from(91.0, 0.0))
        assertNull(GeoPoint.from(0.0, -181.0))
        assertNotNull(GeoPoint.from(0.0, 0.0))
    }
}
