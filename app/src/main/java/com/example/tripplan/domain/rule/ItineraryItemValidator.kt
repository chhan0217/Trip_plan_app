package com.example.tripplan.domain.rule

import com.example.tripplan.domain.model.ItineraryItemInput
import java.time.LocalDate

enum class ItineraryItemValidationError {
    BlankTitle,
    DateOutsideTrip,
    InvalidTimeRange,
}

object ItineraryItemValidator {
    fun validate(
        input: ItineraryItemInput,
        tripStartDate: LocalDate,
        tripEndDate: LocalDate,
    ): Set<ItineraryItemValidationError> = buildSet {
        if (input.title.isBlank()) {
            add(ItineraryItemValidationError.BlankTitle)
        }
        if (input.date.isBefore(tripStartDate) || input.date.isAfter(tripEndDate)) {
            add(ItineraryItemValidationError.DateOutsideTrip)
        }

        val startTime = input.startTime
        val endTime = input.endTime
        if (startTime != null && endTime != null && endTime.isBefore(startTime)) {
            add(ItineraryItemValidationError.InvalidTimeRange)
        }
    }
}
