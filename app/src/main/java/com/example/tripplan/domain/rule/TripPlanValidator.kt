package com.example.tripplan.domain.rule

import com.example.tripplan.domain.model.TripPlanInput

enum class TripPlanValidationError {
    BlankTitle,
    InvalidDateRange,
}

object TripPlanValidator {
    fun validate(input: TripPlanInput): Set<TripPlanValidationError> = buildSet {
        if (input.title.isBlank()) {
            add(TripPlanValidationError.BlankTitle)
        }
        if (input.endDate.isBefore(input.startDate)) {
            add(TripPlanValidationError.InvalidDateRange)
        }
    }
}
