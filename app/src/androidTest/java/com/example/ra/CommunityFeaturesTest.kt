package com.example.ra

import android.content.Context
import android.content.ContextWrapper
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.util.UUID

class CommunityFeaturesTest {
    @get:Rule val compose = createComposeRule()
    private val owner = AccountUser("owner", "Test owner", "owner@example.com", AccountRole.OWNER)
    private val renter = AccountUser("renter", "Test renter", "renter@example.com", AccountRole.RENTER)
    private fun context(): Context {
        val prefix = "community-test-" + UUID.randomUUID()
        return object : ContextWrapper(InstrumentationRegistry.getInstrumentation().targetContext) {
            override fun getApplicationContext(): Context = this
            override fun getSharedPreferences(name: String, mode: Int) =
                super.getSharedPreferences("$prefix-$name", mode)
        }
    }

    private fun clean(context: Context) {
        listOf("yaadride_rentals_v1", "yaadride_community_v1").forEach {
            context.getSharedPreferences(it, Context.MODE_PRIVATE).edit().clear().commit()
        }
    }

    @Test fun favoritesPersistAndStaySeparateForEachAccount() {
        val context = context()
        var user = renter
        try {
            val rentals = RentalStore(context) { user }
            val social = CommunityStore(context, rentals)
            val carId = rentals.cars.first().id
            social.toggleFavorite(carId)
            assertEquals(setOf(carId), CommunityStore(context, rentals).favoriteIds)
            user = renter.copy(id = "another-renter")
            assertTrue(social.favoriteCars.isEmpty())
            user = owner
            social.toggleFavorite(carId)
            assertTrue(social.favoriteIds.isEmpty())
            user = renter
            assertEquals(carId, social.favoriteCars.single().id)
            social.toggleFavorite(carId)
            assertTrue(CommunityStore(context, rentals).favoriteIds.isEmpty())
        } finally { clean(context) }
    }

    @Test fun bookingParticipantsCanReplyAndUnreadCountsPersistWithoutLeakingToOtherAccounts() {
        val context = context()
        var user = owner
        try {
            val rentals = RentalStore(context) { user }
            assertNull(rentals.saveCar(null, "Test ride", "Kingston", 8000, "", GeoPoint(18.0, -76.8)))
            val car = rentals.myCars.single()
            user = renter
            assertNull(rentals.request(car.id, RentalRules.date(RentalRules.today() + 1),
                RentalRules.date(RentalRules.today() + 2)))
            val booking = rentals.myBookings.single()
            val social = CommunityStore(context, rentals)
            assertNotNull(social.send(booking.id, "   "))
            assertNull(social.send(booking.id, "Where should I meet you?"))
            assertEquals(0, social.unreadCount)
            user = owner
            assertEquals(1, social.unreadCount)
            val restored = CommunityStore(context, rentals)
            assertEquals(1, restored.unreadCount)
            restored.markRead(booking.id)
            assertEquals(0, CommunityStore(context, rentals).unreadCount)
            assertNull(restored.send(booking.id, "Meet at the pickup location."))
            user = renter
            assertEquals(1, restored.unreadCount)
            assertEquals(2, restored.threadMessages(booking.id).size)
            user = renter.copy(id = "outsider")
            assertTrue(restored.threads.isEmpty())
            assertTrue(restored.threadMessages(booking.id).isEmpty())
            assertEquals(0, restored.unreadCount)
            assertNotNull(restored.send(booking.id, "Unauthorized"))
            user = renter
            assertEquals(2, CommunityStore(context, rentals).threadMessages(booking.id).size)
        } finally { clean(context) }
    }

    @Test fun newTabsLetRenterSaveVehiclesMessageOwnerAndOpenAccountMenu() {
        val context = context()
        var user = owner
        var signedOut = false
        try {
            val rentals = RentalStore(context) { user }
            assertNull(rentals.saveCar(null, "Test ride", "Kingston", 8000, "", GeoPoint(18.0, -76.8)))
            val car = rentals.myCars.single()
            user = renter
            assertNull(rentals.request(car.id, RentalRules.date(RentalRules.today() + 1),
                RentalRules.date(RentalRules.today() + 2)))
            compose.setContent {
                val registryOwner = checkNotNull(androidx.activity.compose.LocalActivityResultRegistryOwner.current)
                CompositionLocalProvider(LocalContext provides context,
                    androidx.activity.compose.LocalActivityResultRegistryOwner provides registryOwner) {
                    MaterialTheme { YaadRideScreen(renter) { signedOut = true } }
                }
            }
            compose.onNodeWithText("Favorites").assertIsDisplayed().performClick()
            compose.onNodeWithText("No favorites yet. Tap the heart on a vehicle to save it.").assertIsDisplayed()
            compose.onNodeWithText("Explore").performClick()
            compose.onNodeWithContentDescription("Save Toyota Axio to favorites").performClick()
            compose.onNodeWithText("Favorites").performClick()
            compose.onNodeWithText("Toyota Axio").assertIsDisplayed()
            compose.onNodeWithText("Remove from favorites").performClick()
            compose.onNodeWithText("No favorites yet. Tap the heart on a vehicle to save it.").assertIsDisplayed()
            compose.onNodeWithText("Trips").performClick()
            compose.onNodeWithText("Message owner").performScrollTo().performClick()
            compose.onNode(hasSetTextAction()).performTextInput("See you tomorrow")
            compose.onNodeWithText("Send").performClick()
            compose.onNodeWithText("See you tomorrow").assertIsDisplayed()
            compose.onNodeWithText("‹ Inbox").performClick()
            compose.onNodeWithText("You: See you tomorrow").assertIsDisplayed()
            compose.onNodeWithText("More").performClick()
            compose.onNodeWithText(renter.email).assertIsDisplayed()
            compose.onNodeWithText("How YaadRide works").performClick()
            compose.onNodeWithText("Hide help").assertIsDisplayed()
            compose.onAllNodesWithText("Log out")[ 1].performScrollTo().performClick()
            compose.runOnIdle { assertTrue(signedOut) }
        } finally { clean(context) }
    }
}
