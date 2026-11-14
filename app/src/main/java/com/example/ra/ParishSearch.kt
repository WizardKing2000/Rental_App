package com.example.ra

import java.util.Locale

internal object ParishSearch {
    val parishes = listOf(
        "Kingston", "St. Andrew", "St. Thomas", "Portland", "St. Mary", "St. Ann",
        "Trelawny", "St. James", "Hanover", "Westmoreland", "St. Elizabeth",
        "Manchester", "Clarendon", "St. Catherine"
    )

    fun normalize(value: String): String = value.lowercase(Locale.ROOT)
        .replace(Regex("\\bsaint\\b"), "st")
        .replace(".", "")
        .trim()
        .replace(Regex("\\s+"), " ")

    fun matches(car: RentalCar, query: String, parish: String? = null): Boolean {
        val term = normalize(query)
        return car.active &&
            (normalize(car.name).contains(term) || normalize(car.parish).contains(term)) &&
            (parish == null || normalize(car.parish) == normalize(parish))
    }
}
