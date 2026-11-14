package com.example.ra

import android.content.Context
import android.content.ContextWrapper
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.util.UUID

class VehicleDetailsTest {
    @get:Rule val compose = createComposeRule()

    private fun context(): Context {
        val prefix = "vehicle-details-test-" + UUID.randomUUID()
        return object : ContextWrapper(InstrumentationRegistry.getInstrumentation().targetContext) {
            override fun getSharedPreferences(name: String, mode: Int) =
                super.getSharedPreferences("$prefix-$name", mode)
        }
    }

    private val owner = AccountUser("owner", "Test host", "owner@example.com", AccountRole.OWNER)
    private val point = GeoPoint(17.9712, -76.7936)
    private val details = VehicleDetails(
        make = "Toyota", model = "Axio", year = "2025", seats = "5",
        transmission = "Automatic", fuel = "Hybrid", description = "Comfortable island drives.",
        features = "Air conditioning, Bluetooth", pickupInstructions = "Meet in central Kingston.",
        rules = "No smoking. Return with the same fuel level.",
        photos = listOf("content://test/second"),
        pickupLocation = "Half Way Tree, Kingston", returnLocation = "New Kingston meeting point"
    )

    @Test fun detailsAndPhotosPersistAndOtherOwnersCannotChangeThem() {
        val context = context()
        try {
            val store = RentalStore(context) { owner }
            assertNull(store.saveCar(null, "Toyota Axio", "Kingston", 8000, "content://test/cover", point, details))
            val reloaded = RentalStore(context) { owner }
            val car = reloaded.myCars.single()
            assertEquals(details.copy(hostName = owner.name), car.details)
            assertEquals(listOf("content://test/cover", "content://test/second"), car.allPhotos)
            val other = RentalStore(context) { owner.copy(id = "other") }
            assertNotNull(other.saveCar(car, car.name, car.parish, 1000, car.photo, point, details.copy(rules = "Changed")))
            assertEquals(details.rules, RentalStore(context).cars.single { it.id == car.id }.details.rules)
            assertNull(reloaded.saveCar(car, car.name, car.parish, 9000, car.photo, point))
            assertEquals(details.copy(hostName = owner.name), RentalStore(context).cars.single { it.id == car.id }.details)
        } finally {
            context.getSharedPreferences("yaadride_rentals_v1", Context.MODE_PRIVATE).edit().clear().commit()
        }
    }

    @Test fun bookedLocationsSurviveReloadAndLaterListingEdits() {
        val context = context()
        var user = owner
        try {
            val store = RentalStore(context) { user }
            assertNull(store.saveCar(null, "Toyota", "Kingston", 8000, "", point, details))
            val car = store.myCars.single()
            user = owner.copy(id = "renter", role = AccountRole.RENTER)
            assertNull(store.request(car.id, RentalRules.date(RentalRules.today() + 1),
                RentalRules.date(RentalRules.today() + 3)))
            val booking = store.myBookings.single()
            user = owner
            assertNull(store.transition(booking.id, "Approved"))
            assertNull(store.saveCar(car, car.name, car.parish, car.dailyRate, "", point,
                details.copy(pickupLocation = "Another pickup", returnLocation = "Another return")))
            user = owner.copy(id = "renter", role = AccountRole.RENTER)
            val saved = RentalStore(context) { user }.myBookings.single()
            assertEquals("Approved", saved.status)
            assertEquals(details.pickupLocation, saved.pickupLocation)
            assertEquals(details.returnLocation, saved.returnLocation)
        } finally {
            context.getSharedPreferences("yaadride_rentals_v1", Context.MODE_PRIVATE).edit().clear().commit()
        }
    }

    @Test fun invalidDetailsDoNotOverwriteAnExistingListing() {
        val context = context()
        try {
            val store = RentalStore(context) { owner }
            assertNull(store.saveCar(null, "Toyota", "Kingston", 8000, "", point, details))
            val car = store.myCars.single()
            assertNotNull(store.saveCar(car, "Changed", car.parish, 500, "", point, details.copy(seats = "0")))
            assertEquals("Toyota", store.myCars.single().name)
            assertNotNull(details.copy(year = "1800").validationError())
            assertNotNull(details.copy(pickupInstructions = "").validationError())
        } finally {
            context.getSharedPreferences("yaadride_rentals_v1", Context.MODE_PRIVATE).edit().clear().commit()
        }
    }

    @Test fun approvedTripKeepsBookedDatesAndRateWhenListingIsPausedOrRepriced() {
        val context = context()
        val renter = owner.copy(id = "renter", role = AccountRole.RENTER)
        val store = RentalStore(context) { renter }
        val trip = RentalBooking("trip", "car", "Toyota Axio", renter.name,
            RentalRules.today() + 1, RentalRules.today() + 3, 8000, "Approved", owner.id, renter.id)
        val car = RentalCar("car", "Toyota Axio", "Kingston", 12000,
            active = false, ownerId = owner.id, details = details.copy(photos = emptyList()))
        compose.setContent { MaterialTheme { VehicleDetailsScreen(car, store, trip = trip) {} } }
        compose.onNodeWithText("Your trip").assertIsDisplayed()
        compose.onNodeWithText("Pickup: ${RentalRules.date(trip.start)}\nReturn: ${RentalRules.date(trip.end)}\nStatus: Approved")
            .assertIsDisplayed()
        compose.onNodeWithText("Booked total: J$16,000").assertIsDisplayed()
        compose.onNodeWithText("Choose trip dates").assertDoesNotExist()
        compose.onNodeWithText(details.pickupLocation).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(details.returnLocation).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(details.pickupInstructions).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(details.rules).performScrollTo().assertIsDisplayed()
    }

    @Test fun renterSeesOwnerDetailsBeforeOpeningCalendarBooking() {
        val context = context()
        val renter = owner.copy(id = "renter", name = "Test renter", role = AccountRole.RENTER)
        val store = RentalStore(context) { renter }
        val car = RentalCar("ui-test", "Toyota Axio", "Kingston", 8000, ownerId = owner.id,
            details = details.copy(photos = emptyList(), hostName = owner.name))
        compose.setContent { MaterialTheme { VehicleDetailsScreen(car, store) {} } }
        compose.onNodeWithText(car.name).assertIsDisplayed()
        compose.onNodeWithText("Choose trip dates").assertIsDisplayed()
        compose.onNodeWithText(details.description).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(details.pickupLocation).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(details.returnLocation).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(details.pickupInstructions).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(details.rules).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Choose trip dates").performClick()
        compose.onNodeWithText("Select pickup date").assertIsDisplayed()
        compose.onNodeWithText("Select return date").assertIsNotEnabled()
        compose.onNodeWithText("Request booking").assertIsNotEnabled()
    }
}
