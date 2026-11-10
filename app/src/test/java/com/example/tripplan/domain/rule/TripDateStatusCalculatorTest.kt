package com.example.tripplan.domain.rule

import com.example.tripplan.domain.model.TripDatePhase
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TripDateStatusCalculatorTest {
    private val start = LocalDate.of(2028, 2, 28)
    private val end = LocalDate.of(2028, 3, 1)

    @Test
    fun beforeTrip_returnsUpcomingAndDaysUntilStart() {
        val result = TripDateStatusCalculator.calculate(start, end, start.minusDays(3))

        assertEquals(TripDatePhase.Upcoming, result.phase)
        assertEquals(3L, result.daysUntilStart)
    }

    @Test
    fun startDate_returnsFirstOngoingDay() {
        val result = TripDateStatusCalculator.calculate(start, end, start)

        assertEquals(TripDatePhase.Ongoing, result.phase)
        assertEquals(1, result.currentDayNumber)
        assertFalse(result.isLastDay)
    }

    @Test
    fun leapDay_returnsSecondOngoingDay() {
        val result = TripDateStatusCalculator.calculate(start, end, LocalDate.of(2028, 2, 29))

        assertEquals(2, result.currentDayNumber)
    }

    @Test
    fun endDate_isIncludedAndMarkedLastDay() {
        val result = TripDateStatusCalculator.calculate(start, end, end)

        assertEquals(TripDatePhase.Ongoing, result.phase)
        assertEquals(3, result.currentDayNumber)
        assertTrue(result.isLastDay)
    }

    @Test
    fun afterTrip_returnsEnded() {
        val result = TripDateStatusCalculator.calculate(start, end, end.plusDays(1))

        assertEquals(TripDatePhase.Ended, result.phase)
    }
}
