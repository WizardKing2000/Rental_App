package com.example.ra

import org.junit.Assert.*
import org.junit.Test

class RentalRulesTest {
    @Test fun invalidCalendarDatesAreRejected() {
        assertNull(RentalRules.day("2026-02-30"))
        assertNull(RentalRules.day("2026-2-03"))
        assertNull(RentalRules.day("2026-10-31oops"))
        assertNotNull(RentalRules.day("2028-02-29"))
    }

    @Test fun datesRoundTripAndCountNights() {
        val start = RentalRules.day("2026-12-30")!!
        val end = RentalRules.day("2027-01-02")!!
        assertEquals(3L, end - start)
        assertEquals("2026-12-30", RentalRules.date(start))
        assertEquals(24000L, RentalBooking("1", "car", "Toyota", "Renter", start, end, 8000).total)
    }

    @Test fun adjacentBookingsDoNotOverlap() {
        assertFalse(RentalRules.overlaps(10, 12, 12, 14))
        assertTrue(RentalRules.overlaps(10, 13, 12, 14))
        assertTrue(RentalRules.overlaps(10, 15, 11, 12))
    }
}
