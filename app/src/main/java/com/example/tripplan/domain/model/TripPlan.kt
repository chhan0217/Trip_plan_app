package com.example.tripplan.domain.model

import java.time.Instant
import java.time.LocalDate

data class TripPlan(
    val id: Long,
    val title: String,
    val destination: String,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val description: String,
    val createdAt: Instant,
    val updatedAt: Instant,
    val completedAt: Instant? = null,
)
