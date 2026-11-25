package com.example.tripplan.domain.model

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LodgingTest {
    @Test
    fun checkoutDayCanBeTheNextHotelCheckInDay() {
        val start = LocalDate.of(2026, 11, 26)
        val first = Lodging(name = "마이애미 호텔", checkIn = start, checkOut = start.plusDays(2))
        val second = Lodging(name = "다른 호텔", checkIn = first.checkOut, checkOut = start.plusDays(4))

        assertEquals(2L, first.nights)
        assertFalse(first.overlaps(second))
        assertTrue(first.overlaps(second.copy(checkIn = start.plusDays(1))))
    }
}
