package com.example.ra

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import java.util.UUID

internal data class RentalCar(
    val id: String, val name: String, val parish: String, val dailyRate: Long,
    val photo: String = "", val owned: Boolean = true, val active: Boolean = true,
    val ownerId: String = "", val pickup: GeoPoint? = null,
    val details: VehicleDetails = VehicleDetails()
)
internal data class RentalBooking(
    val id: String, val carId: String, val carName: String, val renter: String,
    val start: Long, val end: Long, val dailyRate: Long, val status: String = "Pending",
    val ownerId: String = "", val renterId: String = "",
    val pickupLocation: String = "", val returnLocation: String = ""
) {
    val total: Long get() = (end - start) * dailyRate
}
internal object RentalRules {
    fun day(value: String): Long? {
        if (!Regex("\\d{4}-\\d{2}-\\d{2}").matches(value)) return null
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            isLenient = false
            timeZone = TimeZone.getTimeZone("UTC")
        }
        return runCatching { format.parse(value)!!.time / 86_400_000 }.getOrNull()
    }
    fun today(): Long = day(SimpleDateFormat("yyyy-MM-dd", Locale.US).format(java.util.Date()))!!
    fun date(day: Long): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }.format(java.util.Date(day * 86_400_000))
    fun overlaps(start: Long, end: Long, otherStart: Long, otherEnd: Long) =
        start < otherEnd && otherStart < end
}
internal fun jmd(amount: Long) = "J$" + String.format(Locale.US, "%,d", amount)

/** Account-scoped rental demo data remains on this device. Payments are not connected. */
internal class RentalStore(context: Context, private val session: () -> AccountUser? = { null }) {
    val myCars: List<RentalCar> get() = cars.filter { AccountAccess.canManage(session(), it.ownerId) }
    val myBookings: List<RentalBooking> get() = bookings.filter {
        AccountAccess.canViewBooking(session(), it.ownerId, it.renterId)
    }
    val unavailableToday: Set<String> get() {
        val today = RentalRules.today()
        return bookings.filter { it.status == "Approved" && it.start <= today && it.end > today }
            .map { it.carId }.toSet()
    }
    val currentAccount: AccountUser? get() = session()
    private val prefs = context.getSharedPreferences("yaadride_rentals_v1", Context.MODE_PRIVATE)
    var cars by mutableStateOf<List<RentalCar>>(emptyList())
        private set
    var bookings by mutableStateOf<List<RentalBooking>>(emptyList())
        private set
    var loadError by mutableStateOf<String?>(null)
        private set

    init {
        val saved = prefs.getString("data", null)
        if (saved == null) {
            cars = listOf(
                RentalCar("sample-axio", "Toyota Axio", "Kingston", 8000, owned = false),
                RentalCar("sample-fit", "Honda Fit", "St. Catherine", 6500, owned = false),
                RentalCar("sample-swift", "Suzuki Swift", "St. James", 7000, owned = false)
            )
        } else {
            runCatching {
                val root = JSONObject(saved)
                val vehicles = root.getJSONArray("cars")
                val requests = root.getJSONArray("bookings")
                val loadedCars = (0 until vehicles.length()).map { i ->
                    val c = vehicles.getJSONObject(i)
                    RentalCar(c.getString("id"), c.getString("name"), c.getString("parish"),
                        c.getLong("rate"), c.optString("photo"), c.getBoolean("owned"), c.getBoolean("active"), c.optString("ownerId"),
                        GeoPoint.from(c.optDouble("latitude", Double.NaN), c.optDouble("longitude", Double.NaN)),
                        VehicleDetails.fromJson(c.optJSONObject("details")))
                }
                val loadedBookings = (0 until requests.length()).map { i ->
                    val b = requests.getJSONObject(i)
                    RentalBooking(b.getString("id"), b.getString("carId"), b.getString("carName"),
                        b.getString("renter"), b.getLong("start"), b.getLong("end"),
                        b.getLong("rate"), b.getString("status"), b.optString("ownerId"), b.optString("renterId"),
                        b.optString("pickupLocation"), b.optString("returnLocation"))
                }
                cars = loadedCars
                bookings = loadedBookings
            }.onFailure { loadError = "Saved rentals could not be loaded. Your saved data has been preserved." }
        }
    }

    private fun save() {
        if (loadError != null) return
        val vehicles = JSONArray()
        cars.forEach { c -> vehicles.put(JSONObject().put("id", c.id).put("name", c.name)
            .put("parish", c.parish).put("rate", c.dailyRate).put("photo", c.photo)
            .put("owned", c.owned).put("active", c.active).put("ownerId", c.ownerId)
            .put("latitude", c.pickup?.latitude).put("longitude", c.pickup?.longitude)
            .put("details", c.details.toJson())) }
        val requests = JSONArray()
        bookings.forEach { b -> requests.put(JSONObject().put("id", b.id).put("carId", b.carId)
            .put("carName", b.carName).put("renter", b.renter).put("start", b.start)
            .put("end", b.end).put("rate", b.dailyRate).put("status", b.status)
            .put("ownerId", b.ownerId).put("renterId", b.renterId)
            .put("pickupLocation", b.pickupLocation).put("returnLocation", b.returnLocation)) }
        prefs.edit().putString("data", JSONObject().put("cars", vehicles)
            .put("bookings", requests).toString()).apply()
    }

    fun saveCar(original: RentalCar?, name: String, parish: String, rate: Long, photo: String, pickup: GeoPoint?, details: VehicleDetails? = null): String? {
        if (loadError != null) return loadError
        val user = session()?.takeIf { it.role == AccountRole.OWNER } ?: return "Log in to your owner account to manage cars."
        if (name.isBlank() || parish !in ParishSearch.parishes || rate !in 1..1_000_000) return "Enter a car, parish, and daily rate between J$1 and J$1,000,000."
        val current = original?.let { old -> cars.firstOrNull { it.id == old.id } }
        if (original != null && (current == null || !AccountAccess.canManage(user, current.ownerId))) return "You can only edit your own cars."
        if (pickup == null) return "Set the car\'s pickup location so renters can find it nearby."
        details?.validationError()?.let { return it }
        val listingDetails = (details ?: current?.details ?: VehicleDetails()).copy(hostName = user.name)
        val car = RentalCar(current?.id ?: UUID.randomUUID().toString(), name.trim(), parish,
            rate, photo, active = current?.active ?: true, ownerId = user.id, pickup = pickup,
            details = listingDetails)
        cars = if (original == null) cars + car else cars.map { if (it.id == car.id) car else it }
        save()
        return null
    }

    fun toggle(car: RentalCar) {
        val current = cars.firstOrNull { it.id == car.id } ?: return
        if (!AccountAccess.canManage(session(), current.ownerId) || loadError != null) return
        cars = cars.map { if (it.id == car.id) it.copy(active = !it.active) else it }
        save()
    }

    fun request(carId: String, startText: String, endText: String): String? {
        if (loadError != null) return loadError
        val user = session()?.takeIf(AccountAccess::canRent) ?: return "Log in to a renter account to request a booking."
        val car = cars.firstOrNull { it.id == carId && it.active } ?: return "This listing is unavailable."
        if (car.ownerId.isBlank()) return "This listing is unavailable for new bookings."
        val start = RentalRules.day(startText) ?: return "Enter a valid pickup date (YYYY-MM-DD)."
        val end = RentalRules.day(endText) ?: return "Enter a valid return date (YYYY-MM-DD)."
        if (start < RentalRules.today() || end <= start || end - start > 365) return "Choose a future pickup and a return 1–365 days later."
        if (bookings.any { it.carId == carId && it.status in listOf("Pending", "Approved") &&
                RentalRules.overlaps(start, end, it.start, it.end) }) return "Those dates already have a pending or approved booking."
        bookings = bookings + RentalBooking(UUID.randomUUID().toString(), car.id, car.name,
            user.name, start, end, car.dailyRate, ownerId = car.ownerId, renterId = user.id,
            pickupLocation = car.details.pickupLocation, returnLocation = car.details.returnLocation)
        save()
        return null
    }

    fun transition(id: String, status: String): String? {
        if (loadError != null) return loadError
        val user = session() ?: return "Log in to manage bookings."
        val booking = bookings.firstOrNull { it.id == id } ?: return "Booking not found."
        if (!AccountAccess.canViewBooking(user, booking.ownerId, booking.renterId)) return "This booking belongs to another account."
        if (user.role == AccountRole.RENTER && status != "Cancelled") return "Only the car owner can approve or complete this booking."
        val allowed = when (booking.status) {
            "Pending" -> listOf("Approved", "Declined", "Cancelled")
            "Approved" -> listOf("Completed", "Cancelled")
            else -> emptyList()
        }
        if (status !in allowed) return "This booking can no longer be changed."
        if (status == "Completed" && booking.end > RentalRules.today()) return "Complete a trip on or after its return date."
        if (status == "Approved" && booking.end <= RentalRules.today()) return "This request has expired."
        bookings = bookings.map { if (it.id == id) it.copy(status = status) else it }
        save()
        return null
    }
}
