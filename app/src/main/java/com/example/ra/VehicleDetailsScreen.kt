package com.example.ra

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.SubcomposeAsyncImage

@Composable
internal fun VehicleDetailsScreen(car: RentalCar, store: RentalStore, preview: Boolean = false,
    trip: RentalBooking? = null, community: CommunityStore? = null, onClose: () -> Unit) {
    var booking by rememberSaveable { mutableStateOf(false) }
    val details = car.details
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                Surface(color = MaterialTheme.colorScheme.background) {
                    Row(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = onClose) { Text("‹ Back") }
                        Text(if (trip != null) "Your trip" else if (preview) "Listing preview" else "Discover your ride",
                            style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f).padding(8.dp))
                        if (!preview && community != null && store.currentAccount?.role == AccountRole.RENTER)
                            FavoriteButton(car, community)
                    }
                }
            },
            bottomBar = {
                Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 4.dp) {
                    Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(if (trip != null) "Booked total: ${jmd(trip.total)}" else "${jmd(car.dailyRate)} / day",
                            color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleLarge)
                        if (trip != null) Text("${trip.end - trip.start} days · ${jmd(trip.dailyRate)} / day · ${trip.status}")
                        else if (preview) Text("This is how renters see your vehicle.")
                        else Button(onClick = { booking = true }, enabled = car.active && car.ownerId.isNotBlank(),
                            modifier = Modifier.fillMaxWidth()) {
                            Text(if (car.ownerId.isBlank()) "Sample vehicle · unavailable" else "Choose trip dates")
                        }
                    }
                }
            }
        ) { padding ->
            Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(18.dp)) {
                if (trip != null) DetailSection("Your booked dates",
                    "Pickup: ${RentalRules.date(trip.start)}\nReturn: ${RentalRules.date(trip.end)}\nStatus: ${trip.status}")
                if (trip != null) {
                    DetailSection("Pickup location", trip.pickupLocation.ifBlank { details.pickupLocation }
                        .ifBlank { "The owner has not provided a pickup location yet." })
                    DetailSection("Return location", trip.returnLocation.ifBlank { details.returnLocation }
                        .ifBlank { "The owner has not provided a return location yet." })
                }
                VehiclePhotoGallery(car)
                Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(trip?.carName ?: car.name, style = MaterialTheme.typography.headlineMedium)
                    if (car.parish.isNotBlank()) Text(car.parish, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleMedium)
                    val specs = listOfNotNull(
                        listOf(details.year, details.make, details.model).filter(String::isNotBlank)
                            .joinToString(" ").takeIf(String::isNotBlank),
                        details.seats.takeIf(String::isNotBlank)?.let { "$it seats" },
                        details.transmission.takeIf(String::isNotBlank),
                        details.fuel.takeIf(String::isNotBlank)
                    )
                    if (specs.isNotEmpty()) Text(specs.joinToString(" · "), style = MaterialTheme.typography.bodyLarge)
                }
                DetailSection("Meet your ride", details.description.ifBlank { "The owner hasn't added a description yet." })
                if (details.features.isNotBlank()) DetailSection("Made for your journey", details.features)
                if (trip == null) {
                    DetailSection("Pickup location", details.pickupLocation.ifBlank { "The owner has not provided a pickup location yet." })
                    DetailSection("Return location", details.returnLocation.ifBlank { "The owner has not provided a return location yet." })
                }
                DetailSection("Pickup & return instructions",
                    details.pickupInstructions.ifBlank {
                        if (car.parish.isBlank()) "Pickup arrangements have not been provided yet."
                        else "Pickup area: ${car.parish}. Arrangements have not been provided yet."
                    })
                if (details.hostName.isNotBlank()) DetailSection("Your host", details.hostName)
                DetailSection("Before you drive", details.rules.ifBlank { "The owner hasn't added rental rules yet." })
                if (!preview) Text("Demo bookings · no payment is collected.",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 20.dp))
                Spacer(Modifier.height(8.dp))
            }
        }
        if (booking && trip == null) BookingDialog(car, store) { booking = false }
    }
}

@Composable
private fun DetailSection(title: String, body: String) {
    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleLarge)
            Text(body, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun VehiclePhotoGallery(car: RentalCar) {
    val photos = car.allPhotos
    if (photos.isEmpty()) {
        Box(Modifier.fillMaxWidth().height(220.dp).background(MaterialTheme.colorScheme.surface),
            contentAlignment = Alignment.Center) { Text("Vehicle photos coming soon") }
        return
    }
    val pager = rememberPagerState { photos.size }
    Column {
        HorizontalPager(state = pager, modifier = Modifier.fillMaxWidth().height(280.dp)) { page ->
            SubcomposeAsyncImage(model = photos[page], contentDescription = "${car.name}, photo ${page + 1}",
                contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface),
                loading = { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() } },
                error = { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Photo unavailable") } })
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("${pager.currentPage + 1} / ${photos.size}", style = MaterialTheme.typography.labelLarge)
            if (photos.size > 1) {
                Text("Swipe to explore", color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}
