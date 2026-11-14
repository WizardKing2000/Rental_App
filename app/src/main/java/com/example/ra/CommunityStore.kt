package com.example.ra

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONArray
import org.json.JSONObject

internal data class InboxMessage(
    val sequence: Long, val bookingId: String, val senderId: String,
    val text: String, val createdAt: Long
)

/** Local demo conversations and account-scoped favorites. No remote delivery is implied. */
internal class CommunityStore(context: Context, private val rentals: RentalStore) {
    private val prefs = context.getSharedPreferences("yaadride_community_v1", Context.MODE_PRIVATE)
    private var favorites by mutableStateOf<Map<String, Set<String>>>(emptyMap())
    private var messages by mutableStateOf<List<InboxMessage>>(emptyList())
    private var readThrough by mutableStateOf<Map<String, Map<String, Long>>>(emptyMap())
    var loadError by mutableStateOf<String?>(null)
        private set
    val favoriteIds: Set<String> get() = favorites[rentals.currentAccount?.id].orEmpty()
    val favoriteCars: List<RentalCar> get() = rentals.cars.filter { it.id in favoriteIds }
    val threads: List<RentalBooking> get() = rentals.myBookings.sortedWith(
        compareByDescending<RentalBooking> { threadMessages(it.id).lastOrNull()?.sequence ?: 0 }
            .thenByDescending { it.start })
    val unreadCount: Int get() = threads.sumOf { unread(it.id) }

    init {
        prefs.getString("data", null)?.let { saved ->
            runCatching {
                val root = JSONObject(saved)
                val savedFavorites = root.optJSONObject("favorites") ?: JSONObject()
                val loadedFavorites = savedFavorites.keys().asSequence().associateWith { user ->
                    val ids = savedFavorites.getJSONArray(user)
                    (0 until ids.length()).map { ids.getString(it) }.toSet()
                }
                val savedMessages = root.optJSONArray("messages") ?: JSONArray()
                val loadedMessages = (0 until savedMessages.length()).map { index ->
                    val item = savedMessages.getJSONObject(index)
                    InboxMessage(item.getLong("sequence"), item.getString("bookingId"),
                        item.getString("senderId"), item.getString("text"), item.getLong("createdAt"))
                }
                val savedReads = root.optJSONObject("readThrough") ?: JSONObject()
                val loadedReads = savedReads.keys().asSequence().associateWith { user ->
                    val entries = savedReads.getJSONObject(user)
                    entries.keys().asSequence().associateWith { entries.getLong(it) }
                }
                favorites = loadedFavorites
                messages = loadedMessages
                readThrough = loadedReads
            }.onFailure { loadError = "Favorites and messages could not be loaded. Your saved data has been preserved." }
        }
    }

    private fun save() {
        if (loadError != null) return
        val savedFavorites = JSONObject()
        favorites.forEach { (user, ids) -> savedFavorites.put(user, JSONArray(ids.toList())) }
        val savedMessages = JSONArray()
        messages.forEach {
            savedMessages.put(JSONObject().put("sequence", it.sequence).put("bookingId", it.bookingId)
                .put("senderId", it.senderId).put("text", it.text).put("createdAt", it.createdAt))
        }
        val savedReads = JSONObject()
        readThrough.forEach { (user, threads) -> savedReads.put(user, JSONObject(threads)) }
        prefs.edit().putString("data", JSONObject().put("favorites", savedFavorites)
            .put("messages", savedMessages).put("readThrough", savedReads).toString()).apply()
    }

    fun toggleFavorite(carId: String) {
        val user = rentals.currentAccount?.takeIf(AccountAccess::canRent) ?: return
        if (loadError != null || rentals.cars.none { it.id == carId }) return
        val current = favorites[user.id].orEmpty()
        favorites = favorites + (user.id to if (carId in current) current - carId else current + carId)
        save()
    }

    fun threadMessages(bookingId: String): List<InboxMessage> {
        if (rentals.myBookings.none { it.id == bookingId }) return emptyList()
        return messages.filter { it.bookingId == bookingId }.sortedBy { it.sequence }
    }

    fun unread(bookingId: String): Int {
        val user = rentals.currentAccount ?: return 0
        val seen = readThrough[user.id]?.get(bookingId) ?: 0L
        return threadMessages(bookingId).count { it.senderId != user.id && it.sequence > seen }
    }

    fun markRead(bookingId: String) {
        val user = rentals.currentAccount ?: return
        val latest = threadMessages(bookingId).lastOrNull()?.sequence ?: return
        if (loadError != null || (readThrough[user.id]?.get(bookingId) ?: 0) >= latest) return
        readThrough = readThrough + (user.id to (readThrough[user.id].orEmpty() + (bookingId to latest)))
        save()
    }

    fun send(bookingId: String, text: String): String? {
        loadError?.let { return it }
        val user = rentals.currentAccount ?: return "Log in to send a message."
        val booking = rentals.myBookings.firstOrNull { it.id == bookingId }
            ?: return "This conversation belongs to another account."
        if (booking.ownerId.isBlank() || booking.renterId.isBlank()) return "This older booking does not have a connected conversation."
        val body = text.trim()
        if (body.isEmpty() || body.length > 2000) return "Enter a message between 1 and 2,000 characters."
        messages = messages + InboxMessage((messages.maxOfOrNull { it.sequence } ?: 0) + 1,
            booking.id, user.id, body, System.currentTimeMillis())
        markRead(bookingId)
        save()
        return null
    }
}
