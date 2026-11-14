package com.example.ra

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.json.JSONArray
import org.json.JSONObject
import java.io.Serializable

/** Owner-supplied listing information. Missing legacy fields stay empty. */
internal data class VehicleDetails(
    val make: String = "", val model: String = "", val year: String = "",
    val seats: String = "", val transmission: String = "", val fuel: String = "",
    val description: String = "", val features: String = "",
    val pickupInstructions: String = "", val rules: String = "",
    val photos: List<String> = emptyList(), val hostName: String = "",
    val pickupLocation: String = "", val returnLocation: String = ""
) : Serializable {
    fun validationError(): String? {
        val nextYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR) + 1
        if (make.isBlank() || model.isBlank() || year.toIntOrNull() !in 1900..nextYear)
            return "Enter the vehicle's make, model, and a valid year."
        if (seats.toIntOrNull() !in 1..30) return "Enter a seat count between 1 and 30."
        if (transmission !in transmissions || fuel !in fuels) return "Choose the transmission and fuel type."
        if (description.isBlank()) return "Add a description of your vehicle."
        if (pickupLocation.isBlank() || returnLocation.isBlank()) return "Enter both the pickup and return locations."
        if (pickupLocation.length > 300 || returnLocation.length > 300) return "Keep each location under 300 characters."
        if (pickupInstructions.isBlank()) return "Add pickup and return options for renters."
        if (rules.isBlank()) return "Add your vehicle's rental rules."
        if (make.length > 40 || model.length > 60 || description.length > 2000 ||
            features.length > 1000 || pickupInstructions.length > 1500 || rules.length > 1500 ||
            photos.size > 8) return "Some listing details are too long."
        return null
    }

    fun toJson(): JSONObject = JSONObject()
        .put("make", make).put("model", model).put("year", year).put("seats", seats)
        .put("transmission", transmission).put("fuel", fuel).put("description", description)
        .put("features", features).put("pickupInstructions", pickupInstructions)
        .put("rules", rules).put("photos", JSONArray(photos)).put("hostName", hostName)
        .put("pickupLocation", pickupLocation).put("returnLocation", returnLocation)

    companion object {
        val transmissions = listOf("Automatic", "Manual")
        val fuels = listOf("Petrol", "Diesel", "Hybrid", "Electric")
        fun fromJson(json: JSONObject?): VehicleDetails {
            if (json == null) return VehicleDetails()
            val photos = json.optJSONArray("photos")
            return VehicleDetails(
                make = json.optString("make"), model = json.optString("model"),
                year = json.optString("year"), seats = json.optString("seats"),
                transmission = json.optString("transmission"), fuel = json.optString("fuel"),
                description = json.optString("description"), features = json.optString("features"),
                pickupInstructions = json.optString("pickupInstructions"), rules = json.optString("rules"),
                photos = if (photos == null) emptyList() else (0 until photos.length()).map { photos.getString(it) },
                hostName = json.optString("hostName"),
                pickupLocation = json.optString("pickupLocation"), returnLocation = json.optString("returnLocation")
            )
        }
    }
}

internal val RentalCar.allPhotos: List<String>
    get() = (listOf(photo) + details.photos).filter(String::isNotBlank).distinct().take(8)

@Composable
internal fun VehicleDetailsFields(value: VehicleDetails, onChange: (VehicleDetails) -> Unit) {
    Text("Vehicle details", style = MaterialTheme.typography.titleLarge)
    OutlinedTextField(value.make, { onChange(value.copy(make = it.take(40))) },
        label = { Text("Make · e.g. Toyota") }, singleLine = true, modifier = Modifier.fillMaxWidth())
    OutlinedTextField(value.model, { onChange(value.copy(model = it.take(60))) },
        label = { Text("Model · e.g. Axio") }, singleLine = true, modifier = Modifier.fillMaxWidth())
    OutlinedTextField(value.year, { onChange(value.copy(year = it.filter(Char::isDigit).take(4))) },
        label = { Text("Year") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number))
    OutlinedTextField(value.seats, { onChange(value.copy(seats = it.filter(Char::isDigit).take(2))) },
        label = { Text("Number of seats") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number))
    ListingChoice("Transmission", value.transmission, VehicleDetails.transmissions) { onChange(value.copy(transmission = it)) }
    ListingChoice("Fuel type", value.fuel, VehicleDetails.fuels) { onChange(value.copy(fuel = it)) }
    OutlinedTextField(value.description, { onChange(value.copy(description = it.take(2000))) },
        label = { Text("About your vehicle") }, minLines = 3, modifier = Modifier.fillMaxWidth())
    OutlinedTextField(value.features, { onChange(value.copy(features = it.take(1000))) },
        label = { Text("Features (optional)") }, placeholder = { Text("Air conditioning, Bluetooth, reversing camera…") },
        minLines = 2, modifier = Modifier.fillMaxWidth())
    OutlinedTextField(value.pickupLocation, { onChange(value.copy(pickupLocation = it.take(300))) },
        label = { Text("Pickup location") }, placeholder = { Text("Meeting place, street or landmark, town") },
        modifier = Modifier.fillMaxWidth())
    OutlinedTextField(value.returnLocation, { onChange(value.copy(returnLocation = it.take(300))) },
        label = { Text("Return location") }, placeholder = { Text("Where renters should return the vehicle") },
        modifier = Modifier.fillMaxWidth())
    TextButton(onClick = { onChange(value.copy(returnLocation = value.pickupLocation)) },
        enabled = value.pickupLocation.isNotBlank()) { Text("Return to pickup location") }
    OutlinedTextField(value.pickupInstructions, { onChange(value.copy(pickupInstructions = it.take(1500))) },
        label = { Text("Pickup & return options") }, placeholder = { Text("General pickup area, meeting arrangements, return instructions") },
        minLines = 3, modifier = Modifier.fillMaxWidth())
    Text("These details are public. Share the general pickup area rather than a private home address.",
        style = MaterialTheme.typography.bodySmall)
    OutlinedTextField(value.rules, { onChange(value.copy(rules = it.take(1500))) },
        label = { Text("Rental rules") }, placeholder = { Text("Smoking, pets, cleanliness, fuel on return…") },
        minLines = 3, modifier = Modifier.fillMaxWidth())
}

@Composable
private fun ListingChoice(label: String, selected: String, options: List<String>, onSelect: (String) -> Unit) {
    Text(label, style = MaterialTheme.typography.titleSmall)
    Column {
        options.forEach { option ->
            FilterChip(selected = selected == option, onClick = { onSelect(option) }, label = { Text(option) })
        }
    }
}
