package com.example.ra

import org.junit.Assert.*
import org.junit.Test

class ParishSearchTest {
    private val car = RentalCar("1", "Toyota Axio", "St. Ann", 8000)

    @Test fun allFourteenParishesAreAvailable() {
        assertEquals(14, ParishSearch.parishes.distinct().size)
        assertTrue(ParishSearch.parishes.contains("St. Thomas"))
        assertTrue(ParishSearch.parishes.contains("Clarendon"))
    }

    @Test fun commonSpellingsAndWhitespaceMatch() {
        listOf("Saint Ann", "st ann", " ST.   ANN ", "  Toyota ").forEach {
            assertTrue(it, ParishSearch.matches(car, it))
        }
    }

    @Test fun parishFilterAndVisibilityAreRespected() {
        assertTrue(ParishSearch.matches(car, "", "Saint Ann"))
        assertFalse(ParishSearch.matches(car, "", "St. Mary"))
        assertFalse(ParishSearch.matches(car.copy(active = false), ""))
    }

    @Test fun emptyParishDoesNotInventListings() {
        assertFalse(ParishSearch.matches(car, "", "Clarendon"))
    }
}
