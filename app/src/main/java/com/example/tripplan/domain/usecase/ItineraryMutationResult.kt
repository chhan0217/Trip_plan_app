package com.example.tripplan.domain.usecase

import com.example.tripplan.domain.rule.ItineraryItemValidationError

sealed interface ItineraryMutationResult {
    data class Success(val itemId: Long) : ItineraryMutationResult
    data class Invalid(
        val errors: Set<ItineraryItemValidationError>,
    ) : ItineraryMutationResult

    data object TripNotFound : ItineraryMutationResult
    data object ItemNotFound : ItineraryMutationResult
}

enum class ItineraryMoveDirection {
    Up,
    Down,
}
