package com.example.tripplan.domain.model

import java.time.LocalDate

data class TripCopyDayMapping(
    val originalDayNumber: Int,
    val newDayNumber: Int,
    val newDate: LocalDate,
)

data class TripCopyPlan(
    val originalDayCount: Int,
    val newDayCount: Int,
    val dayMappings: List<TripCopyDayMapping>,
)

sealed interface TripCopyPlanResult {
    data class Success(val plan: TripCopyPlan) : TripCopyPlanResult
    data class Invalid(val errors: Set<TripCopyPlanError>) : TripCopyPlanResult
}

enum class TripCopyPlanError {
    InvalidOriginalDateRange,
    InvalidNewDateRange,
    InvalidDaySelection,
}
