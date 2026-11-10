package com.example.tripplan.data.file

import kotlinx.serialization.Serializable

@Serializable
data class TripPlanFileDto(
    val formatVersion: Int,
    val trip: SharedTripDto,
    val itineraryItems: List<SharedItineraryItemDto>,
)

@Serializable
data class SharedTripDto(
    val title: String,
    val destination: String,
    val startDate: String,
    val endDate: String,
    val description: String,
)

@Serializable
data class SharedItineraryItemDto(
    val date: String,
    val title: String,
    val place: String,
    val startTime: String? = null,
    val endTime: String? = null,
    val note: String,
    val order: Int,
)

const val CURRENT_TRIP_PLAN_FILE_VERSION = 1
