package com.example.ra

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
internal fun NavGlyph(name: String, selected: Boolean = false) {
    val color = LocalContentColor.current
    Canvas(Modifier.size(24.dp)) {
        val scale = size.width / 24f
        fun point(x: Float, y: Float) = Offset(x * scale, y * scale)
        val stroke = Stroke(1.8f * scale)
        when (name) {
            "Favorites" -> {
                val heart = Path().apply {
                    moveTo(12f * scale, 21f * scale)
                    cubicTo(9f * scale, 18f * scale, 2f * scale, 13f * scale, 2f * scale, 7.5f * scale)
                    cubicTo(2f * scale, 2f * scale, 9f * scale, 1f * scale, 12f * scale, 6f * scale)
                    cubicTo(15f * scale, 1f * scale, 22f * scale, 2f * scale, 22f * scale, 7.5f * scale)
                    cubicTo(22f * scale, 13f * scale, 15f * scale, 18f * scale, 12f * scale, 21f * scale)
                    close()
                }
                drawPath(heart, color, style = if (selected) androidx.compose.ui.graphics.drawscope.Fill else stroke)
            }
            "Explore" -> {
                drawCircle(color, 7f * scale, point(10f, 10f), style = stroke)
                drawLine(color, point(15f, 15f), point(22f, 22f), strokeWidth = 1.8f * scale)
            }
            "Inbox" -> {
                val bubble = Path().apply {
                    moveTo(3f * scale, 3f * scale); lineTo(21f * scale, 3f * scale)
                    lineTo(21f * scale, 17f * scale); lineTo(10f * scale, 17f * scale)
                    lineTo(5f * scale, 22f * scale); lineTo(5f * scale, 17f * scale)
                    lineTo(3f * scale, 17f * scale); close()
                }
                drawPath(bubble, color, style = stroke)
            }
            "More" -> listOf(4f, 12f, 20f).forEach { drawCircle(color, 2f * scale, point(it, 12f)) }
            else -> {
                drawLine(color, point(6f, 2f), point(2f, 22f), 1.8f * scale)
                drawLine(color, point(18f, 2f), point(22f, 22f), 1.8f * scale)
                listOf(3f, 10f, 17f).forEach { drawLine(color, point(12f, it), point(12f, it + 4f), 1.8f * scale) }
            }
        }
    }
}

@Composable
internal fun FavoriteButton(car: RentalCar, community: CommunityStore) {
    val saved = car.id in community.favoriteIds
    IconToggleButton(checked = saved, onCheckedChange = { community.toggleFavorite(car.id) },
        enabled = community.loadError == null,
        modifier = Modifier.semantics {
            contentDescription = if (saved) "Remove ${car.name} from favorites" else "Save ${car.name} to favorites"
        }) {
        NavGlyph("Favorites", selected = saved)
    }
}

@Composable
internal fun FavoritesScreen(community: CommunityStore, onOpen: (String) -> Unit, onExplore: () -> Unit) {
    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Your favorites", style = MaterialTheme.typography.headlineMedium)
        Text("Keep the rides you love in one place.")
        if (community.favoriteCars.isEmpty()) {
            Text("No favorites yet. Tap the heart on a vehicle to save it.")
            OutlinedButton(onClick = onExplore) { Text("Explore cars") }
        }
        community.favoriteCars.forEach { car ->
            Card(onClick = { onOpen(car.id) }, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (car.photo.isNotBlank()) AsyncImage(car.photo, car.name, Modifier.fillMaxWidth().height(150.dp))
                    Text(car.name, style = MaterialTheme.typography.titleLarge)
                    Text("${car.parish} · ${jmd(car.dailyRate)} / day")
                    if (!car.active) Text("Currently unavailable", color = MaterialTheme.colorScheme.primary)
                    TextButton(onClick = { community.toggleFavorite(car.id) }) { Text("Remove from favorites") }
                }
            }
        }
    }
}

@Composable
internal fun InboxScreen(rentals: RentalStore, community: CommunityStore, onOpen: (String) -> Unit) {
    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Inbox", style = MaterialTheme.typography.headlineMedium)
        Text("Talk about pickup, return, and your trip.")
        Text("Demo messages stay on this device. They are not delivered to another phone.",
            style = MaterialTheme.typography.bodySmall)
        if (community.threads.isEmpty()) Text("Your booking conversations will appear here after a rental request.")
        community.threads.forEach { thread ->
            val last = community.threadMessages(thread.id).lastOrNull()
            val unread = community.unread(thread.id)
            Card(onClick = { onOpen(thread.id) }, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(thread.carName, style = MaterialTheme.typography.titleLarge)
                    Text(if (rentals.currentAccount?.role == AccountRole.OWNER) "Renter: ${thread.renter}" else "Chat with the owner")
                    Text("${RentalRules.date(thread.start)} → ${RentalRules.date(thread.end)} · ${thread.status}")
                    Text(last?.let { (if (it.senderId == rentals.currentAccount?.id) "You: " else "") + it.text }
                        ?: "Start a conversation", maxLines = 2, overflow = TextOverflow.Ellipsis)
                    if (unread > 0) Text("$unread unread", color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
internal fun ConversationScreen(booking: RentalBooking, rentals: RentalStore, community: CommunityStore,
    onTrip: () -> Unit, onClose: () -> Unit) {
    var draft by rememberSaveable(booking.id) { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val messages = community.threadMessages(booking.id)
    val list = rememberLazyListState()
    val focus = androidx.compose.ui.platform.LocalFocusManager.current
    val keyboard = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
    LaunchedEffect(booking.id, messages.lastOrNull()?.sequence) {
        community.markRead(booking.id)
        if (messages.isNotEmpty()) list.animateScrollToItem(messages.lastIndex)
    }
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Scaffold(
            modifier = Modifier.imePadding(),
            topBar = {
                Surface {
                    Column(Modifier.fillMaxWidth().statusBarsPadding().padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TextButton(onClick = onClose) { Text("‹ Inbox") }
                            Text(booking.carName, style = MaterialTheme.typography.titleMedium,
                                maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                            TextButton(onClick = onTrip) { Text("Trip") }
                        }
                        Text("Demo conversation · only on this device", style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            bottomBar = {
                Surface {
                    Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(draft, { draft = it.take(2000); error = null },
                                label = { Text("Message") }, modifier = Modifier.weight(1f), maxLines = 3)
                            Button(enabled = draft.isNotBlank() && community.loadError == null,
                                onClick = {
                                    error = community.send(booking.id, draft)
                                    if (error == null) { draft = ""; focus.clearFocus(); keyboard?.hide() }
                                }) { Text("Send") }
                        }
                    }
                }
            }
        ) { padding ->
            LazyColumn(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
                state = list, verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(vertical = 16.dp)) {
                if (messages.isEmpty()) item { Text("No messages yet. Start by discussing pickup or return arrangements.") }
                items(messages, key = { it.sequence }) { message ->
                    val mine = message.senderId == rentals.currentAccount?.id
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start) {
                        Surface(shape = RoundedCornerShape(16.dp),
                            color = if (mine) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.widthIn(max = 300.dp)) {
                            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(if (mine) "You" else if (rentals.currentAccount?.role == AccountRole.OWNER) booking.renter else "Owner",
                                    style = MaterialTheme.typography.labelMedium)
                                Text(message.text)
                                Text(SimpleDateFormat("d MMM, h:mm a", Locale.getDefault()).format(Date(message.createdAt)),
                                    style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun MoreScreen(account: AccountUser, onSignOut: () -> Unit) {
    val context = LocalContext.current
    var help by rememberSaveable { mutableStateOf(false) }
    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("More", style = MaterialTheme.typography.headlineMedium)
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(account.name, style = MaterialTheme.typography.titleLarge)
                Text(account.email)
                Text("${account.role.label} account", color = MaterialTheme.colorScheme.primary)
            }
        }
        OutlinedButton(onClick = { openLocationAppSettings(context) }, modifier = Modifier.fillMaxWidth()) { Text("App permissions & settings") }
        OutlinedButton(onClick = { help = !help }, modifier = Modifier.fillMaxWidth()) { Text(if (help) "Hide help" else "How YaadRide works") }
        if (help) {
            Text(if (account.role == AccountRole.RENTER)
                "Explore vehicles, save favorites with the heart, choose dates, and request a booking. Check Trips for approval and pickup details. Use Inbox to discuss your booking."
                else "List your vehicle with photos, price, pickup and return locations. Approve requests in your dashboard, and use Inbox for booking conversations.")
            Text("Accounts are online. Listings, bookings, favorites, and messages currently stay on this device. Payments and payouts are not connected.")
        }
        Button(onClick = onSignOut, modifier = Modifier.fillMaxWidth()) { Text("Log out") }
        Text("YaadRide · Your journey starts here", style = MaterialTheme.typography.bodySmall)
    }
}
