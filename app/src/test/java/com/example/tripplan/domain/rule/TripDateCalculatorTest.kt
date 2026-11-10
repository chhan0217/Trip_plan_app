package com.example.tripplan.domain.rule

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TripDateCalculatorTest {
    @Test
    fun createDays_includesStartAndEndDate() {
        val days = TripDateCalculator.createDays(
            startDate = LocalDate.of(2026, 11, 9),
            endDate = LocalDate.of(2026, 11, 11),
        )

        assertEquals(listOf(1, 2, 3), days.map { it.dayNumber })
        assertEquals(
            listOf(
                LocalDate.of(2026, 11, 9),
                LocalDate.of(2026, 11, 10),
                LocalDate.of(2026, 11, 11),
            ),
            days.map { it.date },
        )
    }

    @Test
    fun createDays_sameStartAndEndDate_returnsOneDay() {
        val date = LocalDate.of(2026, 11, 9)

        val days = TripDateCalculator.createDays(date, date)

        assertEquals(1, days.size)
        assertEquals(1, days.single().dayNumber)
        assertEquals(date, days.single().date)
    }

    @Test
    fun createDays_endBeforeStart_returnsEmptyList() {
        val days = TripDateCalculator.createDays(
            startDate = LocalDate.of(2026, 11, 10),
            endDate = LocalDate.of(2026, 11, 9),
        )

        assertTrue(days.isEmpty())
    }
}
