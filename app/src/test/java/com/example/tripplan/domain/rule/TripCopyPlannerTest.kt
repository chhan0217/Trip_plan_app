package com.example.tripplan.domain.rule

import com.example.tripplan.domain.model.TripCopyPlanError
import com.example.tripplan.domain.model.TripCopyPlanResult
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TripCopyPlannerTest {
    private val originalStart = LocalDate.of(2026, 7, 21)
    private val originalEnd = LocalDate.of(2026, 7, 25)
    private val newStart = LocalDate.of(2026, 8, 10)

    @Test
    fun withoutItinerary_returnsNoDayMappings() {
        val result = TripCopyPlanner.plan(
            originalStart, originalEnd, newStart, newStart.plusDays(2), false
        ) as TripCopyPlanResult.Success

        assertTrue(result.plan.dayMappings.isEmpty())
    }

    @Test
    fun sameLength_defaultsToEveryOriginalDay() {
        val result = TripCopyPlanner.plan(
            originalStart, originalEnd, newStart, newStart.plusDays(4), true
        ) as TripCopyPlanResult.Success

        assertEquals((1..5).toList(), result.plan.dayMappings.map { it.originalDayNumber })
        assertEquals(newStart.plusDays(4), result.plan.dayMappings.last().newDate)
    }

    @Test
    fun shorterTrip_mapsRetainedDaysSequentially() {
        val result = TripCopyPlanner.plan(
            originalStartDate = originalStart,
            originalEndDate = originalEnd,
            newStartDate = newStart,
            newEndDate = newStart.plusDays(2),
            includeItinerary = true,
            retainedOriginalDayNumbers = setOf(1, 3, 5),
        ) as TripCopyPlanResult.Success

        assertEquals(listOf(1, 3, 5), result.plan.dayMappings.map { it.originalDayNumber })
        assertEquals(listOf(1, 2, 3), result.plan.dayMappings.map { it.newDayNumber })
        assertEquals(
            listOf(newStart, newStart.plusDays(1), newStart.plusDays(2)),
            result.plan.dayMappings.map { it.newDate },
        )
    }

    @Test
    fun shorterTripWithoutEnoughRemovedDays_isInvalid() {
        val result = TripCopyPlanner.plan(
            originalStart,
            originalEnd,
            newStart,
            newStart.plusDays(2),
            true,
            retainedOriginalDayNumbers = setOf(1, 2, 3, 4),
        )

        assertEquals(
            TripCopyPlanResult.Invalid(setOf(TripCopyPlanError.InvalidDaySelection)),
            result,
        )
    }

    @Test
    fun invalidDateRanges_returnBothErrors() {
        val result = TripCopyPlanner.plan(
            originalStart,
            originalStart.minusDays(1),
            newStart,
            newStart.minusDays(1),
            true,
        )

        assertEquals(
            TripCopyPlanResult.Invalid(
                setOf(
                    TripCopyPlanError.InvalidOriginalDateRange,
                    TripCopyPlanError.InvalidNewDateRange,
                )
            ),
            result,
        )
    }
}
