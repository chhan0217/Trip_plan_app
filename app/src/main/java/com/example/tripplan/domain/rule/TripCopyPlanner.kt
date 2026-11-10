package com.example.tripplan.domain.rule

import com.example.tripplan.domain.model.TripCopyDayMapping
import com.example.tripplan.domain.model.TripCopyPlan
import com.example.tripplan.domain.model.TripCopyPlanError
import com.example.tripplan.domain.model.TripCopyPlanResult
import java.time.LocalDate
import java.time.temporal.ChronoUnit

object TripCopyPlanner {
    fun plan(
        originalStartDate: LocalDate,
        originalEndDate: LocalDate,
        newStartDate: LocalDate,
        newEndDate: LocalDate,
        includeItinerary: Boolean,
        retainedOriginalDayNumbers: Set<Int> = emptySet(),
    ): TripCopyPlanResult {
        val errors = buildSet {
            if (originalEndDate.isBefore(originalStartDate)) {
                add(TripCopyPlanError.InvalidOriginalDateRange)
            }
            if (newEndDate.isBefore(newStartDate)) {
                add(TripCopyPlanError.InvalidNewDateRange)
            }
        }
        if (errors.isNotEmpty()) return TripCopyPlanResult.Invalid(errors)

        val originalDayCount = dayCount(originalStartDate, originalEndDate)
        val newDayCount = dayCount(newStartDate, newEndDate)
        if (!includeItinerary) {
            return TripCopyPlanResult.Success(
                TripCopyPlan(originalDayCount, newDayCount, emptyList())
            )
        }

        val availableDays = (1..originalDayCount).toSet()
        val selectedDays = if (originalDayCount <= newDayCount) {
            if (retainedOriginalDayNumbers.isEmpty()) availableDays else retainedOriginalDayNumbers
        } else {
            retainedOriginalDayNumbers
        }
        val requiredCount = minOf(originalDayCount, newDayCount)
        if (selectedDays.size != requiredCount || !availableDays.containsAll(selectedDays)) {
            return TripCopyPlanResult.Invalid(setOf(TripCopyPlanError.InvalidDaySelection))
        }

        val mappings = selectedDays.sorted().mapIndexed { index, originalDayNumber ->
            TripCopyDayMapping(
                originalDayNumber = originalDayNumber,
                newDayNumber = index + 1,
                newDate = newStartDate.plusDays(index.toLong()),
            )
        }
        return TripCopyPlanResult.Success(
            TripCopyPlan(originalDayCount, newDayCount, mappings)
        )
    }

    private fun dayCount(startDate: LocalDate, endDate: LocalDate): Int =
        ChronoUnit.DAYS.between(startDate, endDate).toInt() + 1
}
