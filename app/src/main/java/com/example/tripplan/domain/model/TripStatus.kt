package com.example.tripplan.domain.model

enum class TripDatePhase {
    Upcoming,
    Ongoing,
    Ended,
}

data class TripDateStatus(
    val phase: TripDatePhase,
    val daysUntilStart: Long? = null,
    val currentDayNumber: Int? = null,
    val isLastDay: Boolean = false,
)

data class TripStatus(
    val dateStatus: TripDateStatus,
    val isCompleted: Boolean,
)
