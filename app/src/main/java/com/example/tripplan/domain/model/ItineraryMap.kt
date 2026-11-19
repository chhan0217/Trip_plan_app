package com.example.tripplan.domain.model

import java.time.LocalDate
import java.time.LocalTime

data class ItineraryMapData(
    val tripTitle: String,
    val days: List<TripDay>,
    val items: List<ItineraryMapItem>,
)

data class ItineraryMapItem(
    val id: Long,
    val date: LocalDate,
    val title: String,
    val placeName: String,
    val address: String,
    val startTime: LocalTime?,
    val orderInDay: Int,
    val latitude: Double?,
    val longitude: Double?,
) {
    val hasCoordinates: Boolean get() = latitude != null && longitude != null
}
