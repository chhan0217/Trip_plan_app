package com.example.tripplan.domain.model

import java.time.LocalDate
import java.time.LocalTime

data class ItineraryItem(
    val id: Long,
    val tripPlanId: Long,
    val date: LocalDate,
    val title: String,
    val place: String,
    val startTime: LocalTime? = null,
    val endTime: LocalTime? = null,
    val note: String = "",
    val order: Int,
    val placeId: Long? = null,
)

data class ItineraryItemInput(
    val date: LocalDate,
    val title: String,
    val place: String,
    val startTime: LocalTime? = null,
    val endTime: LocalTime? = null,
    val note: String = "",
    val placeId: Long? = null,
)
