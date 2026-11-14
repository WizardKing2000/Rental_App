package com.example.ra

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Forest = Color(0xFF0E1712)
private val Surface = Color(0xFF1F2C25)
private val Gold = Color(0xFFF5D66F)
private val Muted = Color(0xFFB9C3BB)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(
                primary = Gold, background = Forest, surface = Surface,
                onPrimary = Forest, onSurface = Color.White
            )) {
                val auth = remember { androidx.lifecycle.ViewModelProvider(this@MainActivity)[AccountSession::class.java] }
                val user = auth.account
                if (user == null) AccountScreens(auth)
                else key(user.id) { YaadRideScreen(user, auth::signOut) }
            }
        }
    }
}

@Composable
internal fun YaadRideScreen(account: AccountUser, onSignOut: () -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    var tab by rememberSaveable { mutableStateOf(if (account.role == AccountRole.OWNER) "Dashboard" else "Explore") }
    var nearbyPoint by remember { mutableStateOf<GeoPoint?>(null) }
    var nearbyRequested by remember { mutableStateOf(false) }
    val location = rememberLocationControl("YaadRide uses your location to find available cars within 25 km of you. Your location is used only while searching.") {
        nearbyPoint = it
        query = ""
    }
    val nearby = nearbyRequested
    val owner = account.role == AccountRole.OWNER
    val context = LocalContext.current.applicationContext
    val store = remember(account.id) { RentalStore(context) { account } }
    val community = remember(account.id) { CommunityStore(context, store) }
    var conversationId by rememberSaveable { mutableStateOf<String?>(null) }
    val cars = store.cars.filter { it.active }
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedTripId by rememberSaveable { mutableStateOf<String?>(null) }
    var tripError by remember { mutableStateOf<String?>(null) }
    Scaffold(
        containerColor = Forest,
        bottomBar = {
            Column(Modifier.background(Forest)) {
                Text("Demo mode · Booking requests stay on this device",
                    color = Muted, fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
                NavigationBar(containerColor = Forest) {
                    (if (owner) listOf("Dashboard", "Inbox", "More")
                    else listOf("Explore", "Favorites", "Trips", "Inbox", "More")).forEach { name ->
                        NavigationBarItem(
                            selected = tab == name, onClick = { tab = name },
                            icon = {
                                if (name == "Inbox" && community.unreadCount > 0) BadgedBox(badge = {
                                    Badge { Text(community.unreadCount.toString()) }
                                }) { NavGlyph(name) }
                                else NavGlyph(name, tab == name)
                            },
                            label = { Text(name, fontSize = 11.sp, maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Gold, selectedTextColor = Gold,
                                indicatorColor = Color(0xFF303E23),
                                unselectedIconColor = Muted, unselectedTextColor = Muted
                            )
                        )
                    }
                }
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())) {
            Row(Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                Image(painterResource(R.drawable.yaadride_logo), "YaadRide logo",
                    Modifier.size(52.dp).clip(RoundedCornerShape(12.dp)))
                Column(Modifier.weight(1f).padding(start = 10.dp)) {
                    Text("YaadRide", color = Gold, fontSize = 25.sp, fontWeight = FontWeight.Bold)
                    Text("${account.name} · ${account.role.label}", color = Muted, fontSize = 14.sp)
                }
                TextButton(onClick = onSignOut) { Text("Log out", color = Gold) }
            }
            store.loadError?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(20.dp)) }
            community.loadError?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(20.dp)) }
            if (tab == "More") {
                MoreScreen(account, onSignOut)
            } else if (tab == "Inbox") {
                InboxScreen(store, community) { conversationId = it }
            } else if (tab == "Favorites" && !owner) {
                FavoritesScreen(community, onOpen = { selectedId = it }, onExplore = { tab = "Explore" })
            } else if (owner) {
                OwnerScreen(store)
            } else if (tab == "Explore") {
                OutlinedTextField(
                    value = query, onValueChange = { query = it; nearbyRequested = false; nearbyPoint = null; location.cancel() },
                    placeholder = { Text("Search car or parish", color = Muted) },
                    singleLine = true, shape = RoundedCornerShape(22.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = Surface,
                        focusedContainerColor = Surface,
                        unfocusedBorderColor = Color(0xFF48584E)
                    ),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)
                )
                Row(Modifier.padding(horizontal = 20.dp, vertical = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = !nearby && !location.busy,
                        onClick = { location.cancel(); nearbyRequested = false; nearbyPoint = null }, label = { Text("All") },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Gold, selectedLabelColor = Forest))
                    FilterChip(selected = nearby || location.busy, enabled = !location.busy,
                        onClick = { query = ""; nearbyRequested = true; nearbyPoint = null; location.request() },
                        label = { Text(if (location.busy) "Finding nearby…" else "Nearby") },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Gold, selectedLabelColor = Forest))
                }
                if (location.busy) LinearProgressIndicator(Modifier.fillMaxWidth().padding(horizontal = 20.dp))
                location.message?.let {
                    Text(it, color = Muted, modifier = Modifier.padding(horizontal = 20.dp))
                }
                if (location.needsSettings) TextButton(onClick = { openLocationAppSettings(context) },
                    modifier = Modifier.padding(horizontal = 12.dp)) { Text("App settings") }
                Text(if (nearby) "Cars near you" else "Find your next ride", fontSize = 24.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 20.dp, top = 10.dp, end = 20.dp))
                Text(if (nearby) "Within 25 km · Nearest first · Approximate distances" else "Explore vehicles across Jamaica · JMD per day", color = Muted, fontSize = 17.sp,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp))
                val nearbyCars = nearbyPoint?.let { NearbySearch.find(cars, it, query, store.unavailableToday) }
                val distances = nearbyCars.orEmpty().associate { it.car.id to it.distanceKm }
                val results = if (location.busy) emptyList() else if (nearby)
                    nearbyCars.orEmpty().map { it.car }
                else cars.filter { ParishSearch.matches(it, query) }
                if (results.isEmpty() && !location.busy && (!nearby || nearbyPoint != null)) {
                    Text(if (nearby) "No available cars within 25 km of your location." else "No cars match your search.",
                        color = Muted, modifier = Modifier.padding(20.dp))
                    if (nearby) Text("Only vehicles with a saved pickup pin can appear nearby.",
                        color = Muted, modifier = Modifier.padding(horizontal = 20.dp))
                    TextButton(onClick = { query = ""; nearbyRequested = false; nearbyPoint = null; location.cancel() }) { Text("Show all cars") }
                }
                LazyRow(contentPadding = PaddingValues(20.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    items(results, key = { it.id }) { car ->
                        Card(onClick = { selectedId = car.id }, shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = Surface),
                            modifier = Modifier.width(270.dp)) {
                            if (car.photo.isNotEmpty()) {
                                AsyncImage(car.photo, car.name, Modifier.fillMaxWidth().height(178.dp),
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop)
                            } else CarIllustration(Color(0xFF518F7D))
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(car.name, fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                    FavoriteButton(car, community)
                                }
                                Text(car.parish, color = Muted, fontSize = 17.sp)
                                distances[car.id]?.let { distance ->
                                    Text(String.format(java.util.Locale.US, "~%.1f km away", distance), color = Gold)
                                }
                                Text("${jmd(car.dailyRate)} / day", color = Gold, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                Text(if (car.ownerId.isNotBlank()) "View vehicle →" else "Sample vehicle", color = Muted, modifier = Modifier.padding(top = 2.dp))
                            }
                        }
                    }
                }
            } else {
                Text("Your trips", fontSize = 26.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(20.dp))
                if (store.myBookings.isEmpty()) Text("No booking requests yet. Find your next ride in Explore.",
                    color = Muted, modifier = Modifier.padding(horizontal = 20.dp))
                Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    store.myBookings.asReversed().forEach { booking ->
                        BookingCard(booking) {
                            Column {
                                OutlinedButton(onClick = { selectedTripId = booking.id }) { Text("View trip details") }
                                TextButton(onClick = { conversationId = booking.id }) { Text("Message owner") }
                                if (booking.status in listOf("Pending", "Approved"))
                                    TextButton(onClick = { tripError = store.transition(booking.id, "Cancelled") }) { Text("Cancel request") }
                            }
                        }
                    }
                    tripError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
                TextButton(onClick = { tab = "Explore" }, modifier = Modifier.padding(12.dp)) { Text("Explore cars") }
            }
        }
    }
    store.myBookings.firstOrNull { it.id == conversationId }?.let { thread ->
        key(thread.id) {
            ConversationScreen(thread, store, community, onTrip = {
                conversationId = null
                selectedTripId = thread.id
            }, onClose = { conversationId = null; tab = "Inbox" })
        }
    }
    store.myBookings.firstOrNull { it.id == selectedTripId }?.let { trip ->
        val car = store.cars.firstOrNull { it.id == trip.carId }
            ?: RentalCar(trip.carId, trip.carName, "", trip.dailyRate, ownerId = trip.ownerId)
        key(trip.id) {
            VehicleDetailsScreen(car, store, trip = trip, community = community) { selectedTripId = null }
        }
    }
    store.cars.firstOrNull { it.id == selectedId }?.let { car ->
        key(car.id) { VehicleDetailsScreen(car, store, community = community) { selectedId = null } }
    }
}

@Composable
private fun CarIllustration(color: Color) {
    Box(Modifier.fillMaxWidth().height(178.dp)) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            drawRect(Brush.linearGradient(listOf(Color(0xFF30473D), Color(0xFFBEC6B4))))
            drawCircle(Color(0xFFD8C86C), w * .205f, Offset(w * .75f, h * .36f))
            drawOval(Color(0xFF415A49).copy(alpha = .22f), Offset(w * .16f, h * .74f), Size(w * .69f, h * .10f))
            val roof = Path().apply {
                moveTo(w * .30f, h * .50f); lineTo(w * .40f, h * .30f)
                lineTo(w * .63f, h * .30f); lineTo(w * .74f, h * .50f); close()
            }
            drawPath(roof, color)
            drawRoundRect(color, Offset(w * .17f, h * .49f), Size(w * .66f, h * .24f), CornerRadius(w * .055f))
            val windows = Path().apply {
                moveTo(w * .35f, h * .49f); lineTo(w * .42f, h * .34f)
                lineTo(w * .515f, h * .34f); lineTo(w * .515f, h * .49f); close()
                moveTo(w * .53f, h * .34f); lineTo(w * .61f, h * .34f)
                lineTo(w * .68f, h * .49f); lineTo(w * .53f, h * .49f); close()
            }
            drawPath(windows, Color(0xFFD5E8DF))
            drawRect(Gold, Offset(w * .78f, h * .53f), Size(w * .05f, h * .06f))
            listOf(.32f, .69f).forEach { x ->
                drawCircle(Color(0xFF193E31), w * .068f, Offset(w * x, h * .72f))
                drawCircle(Color(0xFFD5DFD5), w * .032f, Offset(w * x, h * .72f))
            }
        }
        Text("ILLUSTRATION", color = Color(0xFF4E6052), fontSize = 12.sp,
            modifier = Modifier.align(Alignment.BottomEnd).padding(10.dp))
    }
}
