package com.example.ra

import android.app.DatePickerDialog
import android.content.Intent
import java.util.Calendar
import java.util.Locale
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage

private val parishes = ParishSearch.parishes

@Composable
internal fun OwnerScreen(store: RentalStore) {
    var adding by rememberSaveable { mutableStateOf(false) }
    var editingId by rememberSaveable { mutableStateOf<String?>(null) }
    val editing = store.myCars.firstOrNull { it.id == editingId }
    var message by remember { mutableStateOf<String?>(null) }
    var preview by remember { mutableStateOf<RentalCar?>(null) }
    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Owner dashboard", fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Text("Manage your cars and rental requests.")
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Demo rental earnings", fontWeight = FontWeight.Bold)
                Text(jmd(store.myBookings.filter { it.status == "Completed" }.sumOf { it.total }),
                    fontSize = 28.sp, color = MaterialTheme.colorScheme.primary)
                Text("Approved upcoming rentals: " + jmd(store.myBookings.filter { it.status == "Approved" }.sumOf { it.total }))
                Text("Calculated rental totals only. No money collected, fees deducted, or payouts sent.",
                    style = MaterialTheme.typography.bodySmall)
            }
        }
        Button(onClick = { adding = true }, enabled = store.loadError == null) { Text("List a car") }
        Text("Your cars", fontSize = 21.sp, fontWeight = FontWeight.Bold)
        if (store.myCars.isEmpty()) Text("Add your first car to start receiving demo booking requests.")
        store.myCars.forEach { car ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (car.photo.isNotEmpty()) AsyncImage(car.photo, car.name, Modifier.fillMaxWidth().height(150.dp))
                    Text(car.name, fontWeight = FontWeight.Bold)
                    Text("${car.parish} · ${jmd(car.dailyRate)} / day")
                    Text(if (car.active) "Listed in Explore" else "Paused · hidden from Explore")
                    Text(if (car.pickup != null) "Pickup location set · included in Nearby" else "Set a pickup location to appear in Nearby")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { editingId = car.id }) { Text("Edit") }
                        TextButton(onClick = { preview = car }) { Text("Preview") }
                        TextButton(onClick = { store.toggle(car) }) { Text(if (car.active) "Pause listing" else "Publish listing") }
                    }
                }
            }
        }
        Text("Booking requests", fontSize = 21.sp, fontWeight = FontWeight.Bold)
        if (store.myBookings.isEmpty()) Text("No booking requests yet. Requests for your cars will appear here.")
        store.myBookings.asReversed().forEach { booking ->
            BookingCard(booking) {
                when (booking.status) {
                    "Pending" -> {
                        Button(onClick = { message = store.transition(booking.id, "Approved") }) { Text("Approve") }
                        TextButton(onClick = { message = store.transition(booking.id, "Declined") }) { Text("Decline") }
                    }
                    "Approved" -> {
                        Button(onClick = { message = store.transition(booking.id, "Completed") },
                            enabled = booking.end <= RentalRules.today()) { Text("Complete trip") }
                        TextButton(onClick = { message = store.transition(booking.id, "Cancelled") }) { Text("Cancel") }
                    }
                }
            }
        }
        message?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    }
    if (adding || editing != null) CarEditor(store, editing) { adding = false; editingId = null }
    preview?.let { VehicleDetailsScreen(it, store, preview = true) { preview = null } }
}

@Composable
private fun CarEditor(store: RentalStore, original: RentalCar?, onClose: () -> Unit) {
    var name by rememberSaveable { mutableStateOf(original?.name ?: "") }
    var parish by rememberSaveable { mutableStateOf(original?.parish ?: parishes.first()) }
    var rate by rememberSaveable { mutableStateOf(original?.dailyRate?.toString() ?: "") }
    var details by rememberSaveable { mutableStateOf(original?.details ?: VehicleDetails()) }
    var photos by rememberSaveable { mutableStateOf(ArrayList(original?.allPhotos.orEmpty())) }
    var pickupLatitude by rememberSaveable { mutableStateOf(original?.pickup?.latitude) }
    var pickupLongitude by rememberSaveable { mutableStateOf(original?.pickup?.longitude) }
    val location = rememberLocationControl("YaadRide uses your location to set this car's pickup point. Use this while you are at the pickup point.") {
        pickupLatitude = it.latitude
        pickupLongitude = it.longitude
    }
    var error by remember { mutableStateOf<String?>(null) }
    var expanded by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        val accepted = photos.toMutableList()
        var failed = false
        uris.distinct().filter { it.toString() !in accepted }.take(8 - accepted.size).forEach { uri ->
            runCatching { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
                .onSuccess { accepted.add(uri.toString()) }
                .onFailure { failed = true }
        }
        photos = ArrayList(accepted)
        error = if (failed) "Some photos could not be added. Please choose them again." else null
    }
    Dialog(onDismissRequest = onClose,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(Modifier.safeDrawingPadding().imePadding().padding(20.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                TextButton(onClick = onClose) { Text("‹ Back to dashboard") }
                Text(if (original == null) "List your car" else "Edit your car", fontSize = 23.sp)
                OutlinedTextField(name, { name = it.take(80) }, label = { Text("Listing title") },
                    placeholder = { Text("2025 Toyota Axio") }, singleLine = true)
                VehicleDetailsFields(details) { details = it }
                Box {
                    OutlinedButton(onClick = { expanded = true }) { Text("Parish: $parish ▾") }
                    DropdownMenu(expanded, { expanded = false }) {
                        parishes.forEach { item ->
                            DropdownMenuItem(text = { Text(item) }, onClick = { parish = item; expanded = false })
                        }
                    }
                }
                OutlinedTextField(rate, { rate = it.filter(Char::isDigit).take(7) },
                    label = { Text("Daily rental price (JMD)") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                Text("Pickup location", fontWeight = FontWeight.Bold)
                Text("Use your location while you are at the car's pickup point.")
                OutlinedButton(onClick = location.request, enabled = !location.busy) {
                    Text(if (location.busy) "Finding pickup location…" else if (pickupLatitude != null) "Update pickup location" else "Use current location for pickup")
                }
                if (pickupLatitude != null && pickupLongitude != null)
                    Text("Pickup location set", color = MaterialTheme.colorScheme.primary)
                location.message?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                if (location.needsSettings) TextButton(onClick = { openLocationAppSettings(context) }) { Text("App settings") }
                Text("Vehicle photos · ${photos.size}/8", style = MaterialTheme.typography.titleMedium)
                Text("The first photo is your cover. Add clear photos of the exterior and interior.")
                photos.forEachIndexed { index, uri ->
                    Column {
                        AsyncImage(uri, "Vehicle photo ${index + 1}", Modifier.fillMaxWidth().height(160.dp))
                        Row {
                            if (index > 0) TextButton(onClick = {
                                photos = ArrayList(photos.toMutableList().apply { remove(uri); add(0, uri) })
                            }) { Text("Make cover") }
                            TextButton(onClick = { photos = ArrayList(photos.filterNot { it == uri }) }) {
                                Text("Remove photo ${index + 1}")
                            }
                        }
                    }
                }
                OutlinedButton(enabled = photos.size < 8, onClick = { picker.launch(arrayOf("image/*")) }) {
                    Text("Add vehicle photos")
                }
                Text("Rate changes apply to new requests. Existing bookings keep their agreed rate.",
                    style = MaterialTheme.typography.bodySmall)
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                Button(onClick = {
                    error = if (photos.isEmpty()) "Add at least one vehicle photo." else
                        store.saveCar(original, name, parish, rate.toLongOrNull() ?: 0, photos.first(),
                            GeoPoint.from(pickupLatitude ?: Double.NaN, pickupLongitude ?: Double.NaN),
                            details.copy(photos = photos.drop(1)))
                    if (error == null) onClose()
                }) { Text("Save listing") }
                TextButton(onClick = onClose) { Text("Cancel") }
            }
        }
    }
}

@Composable
internal fun BookingCard(booking: RentalBooking, actions: @Composable RowScope.() -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(booking.carName, fontWeight = FontWeight.Bold)
            Text("Renter: ${booking.renter}")
            Text("${RentalRules.date(booking.start)} → ${RentalRules.date(booking.end)}")
            Text("${booking.end - booking.start} days · ${jmd(booking.total)}")
            Text(booking.status, color = MaterialTheme.colorScheme.primary)
            if (booking.status == "Approved" && booking.end > RentalRules.today())
                Text("Trip can be completed on or after the return date.", style = MaterialTheme.typography.bodySmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), content = actions)
        }
    }
}

@Composable
internal fun BookingDialog(car: RentalCar, store: RentalStore, onClose: () -> Unit) {
    var start by rememberSaveable { mutableStateOf("") }
    var end by rememberSaveable { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var sent by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current
    fun chooseDate(pickup: Boolean) {
        val today = RentalRules.today()
        val first = RentalRules.day(start)
        val minimum = if (pickup) today else (first ?: today) + 1
        val maximum = if (pickup) null else first?.plus(365)
        val current = RentalRules.day(if (pickup) start else end)
            ?.coerceAtLeast(minimum)?.let { if (maximum != null) it.coerceAtMost(maximum) else it }
            ?: minimum
        // Build local calendar midnights from date components to avoid UTC date shifts.
        fun calendar(day: Long): Calendar {
            val parts = RentalRules.date(day).split("-").map(String::toInt)
            return Calendar.getInstance().apply {
                clear()
                set(parts[0], parts[1] - 1, parts[2])
            }
        }
        val initial = calendar(current)
        DatePickerDialog(context, { _, year, month, day ->
            val date = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, day)
            if (pickup) {
                start = date
                val returnDay = RentalRules.day(end)
                val pickupDay = RentalRules.day(date)!!
                if (returnDay != null && returnDay - pickupDay !in 1..365) end = ""
            } else end = date
            error = null
        }, initial.get(Calendar.YEAR), initial.get(Calendar.MONTH), initial.get(Calendar.DAY_OF_MONTH)).apply {
            datePicker.minDate = calendar(minimum).timeInMillis
            maximum?.let { datePicker.maxDate = calendar(it).timeInMillis }
        }.show()
    }
    Dialog(onDismissRequest = onClose) {
        Surface(shape = MaterialTheme.shapes.large) {
            Column(Modifier.padding(20.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(car.name, fontSize = 23.sp, fontWeight = FontWeight.Bold)
                Text("${car.parish} · ${jmd(car.dailyRate)} / day")
                if (car.ownerId.isBlank()) {
                    Text("This listing is unavailable for new bookings.")
                } else if (sent) {
                    Text("Request sent to the owner. Check Trips for updates.")
                } else {
                    Text("Demo booking · no payment will be taken.")
                    Text("Booking as ${store.currentAccount?.name.orEmpty()}")
                    OutlinedButton(onClick = { chooseDate(pickup = true) }, modifier = Modifier.fillMaxWidth()) {
                        Text(if (start.isEmpty()) "Select pickup date" else "Pickup: $start")
                    }
                    OutlinedButton(onClick = { chooseDate(pickup = false) },
                        enabled = start.isNotEmpty(), modifier = Modifier.fillMaxWidth()) {
                        Text(if (end.isEmpty()) "Select return date" else "Return: $end")
                    }
                    val first = RentalRules.day(start)
                    val last = RentalRules.day(end)
                    if (first != null && last != null && last - first in 1..365)
                        Text("Rental total: ${jmd((last - first) * car.dailyRate)}")
                    error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    Button(enabled = start.isNotEmpty() && end.isNotEmpty(), onClick = {
                        error = store.request(car.id, start, end)
                        if (error == null) sent = true
                    }) { Text("Request booking") }
                }
                TextButton(onClick = onClose) { Text("Close") }
            }
        }
    }
}
