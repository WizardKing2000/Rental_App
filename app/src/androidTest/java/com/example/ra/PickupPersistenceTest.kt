package com.example.ra

import android.content.Context
import android.content.ContextWrapper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class PickupPersistenceTest {
    private fun isolatedContext(): Context {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val prefix = "pickup-test-" + UUID.randomUUID()
        return object : ContextWrapper(context) {
            override fun getSharedPreferences(name: String, mode: Int) =
                super.getSharedPreferences("$prefix-$name", mode)
        }
    }

    @Test fun ownerPickupCoordinatesSurviveReload() {
        val context = isolatedContext()
        val owner = AccountUser("owner", "Owner", "owner@example.com", AccountRole.OWNER)
        val point = GeoPoint(17.9712, -76.7936)
        val store = RentalStore(context) { owner }
        try {
            assertNull(store.saveCar(null, "Toyota Axio", "Kingston", 8000, "", point))
            val reloaded = RentalStore(context) { owner }
            assertNull(reloaded.loadError)
            assertEquals(point, reloaded.myCars.single().pickup)
            assertEquals(1, NearbySearch.find(reloaded.cars, point).size)
            assertNotNull(store.saveCar(null, "Honda Fit", "Kingston", 6500, "", null))
        } finally {
            context.getSharedPreferences("yaadride_rentals_v1", Context.MODE_PRIVATE).edit().clear().commit()
        }
    }

    @Test fun legacyCarsRemainReadableWithoutInventingPickupCoordinates() {
        val context = isolatedContext()
        val prefs = context.getSharedPreferences("yaadride_rentals_v1", Context.MODE_PRIVATE)
        try {
            prefs.edit().putString("data",
                """{"cars":[{"id":"legacy","name":"Toyota","parish":"Kingston","rate":8000,"owned":true,"active":true,"ownerId":"owner"}],"bookings":[]}"""
            ).commit()
            val store = RentalStore(context)
            assertNull(store.loadError)
            assertNull(store.cars.single().pickup)
            assertTrue(NearbySearch.find(store.cars, GeoPoint(17.9712, -76.7936)).isEmpty())
        } finally {
            prefs.edit().clear().commit()
        }
    }
}
