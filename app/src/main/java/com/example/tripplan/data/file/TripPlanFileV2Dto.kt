package com.example.tripplan.data.file

import kotlinx.serialization.Serializable

@Serializable
data class TripPlanFileV2Dto(
    val formatVersion: Int,
    val trip: SharedTripDto,
    val itineraryItems: List<SharedItineraryItemV2Dto>,
    val sharedPackingItems: List<SharedPackingItemDto>,
)

@Serializable
data class SharedItineraryItemV2Dto(
    val date: String,
    val title: String,
    val place: String,
    val placeDetails: SharedPlaceDetailsDto? = null,
    val startTime: String? = null,
    val endTime: String? = null,
    val note: String,
    val order: Int,
)

@Serializable
data class SharedPlaceDetailsDto(
    val address: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
)

@Serializable
data class SharedPackingItemDto(
    val name: String,
    val category: String,
    val quantity: Int,
    val memo: String,
    val order: Int,
)

const val TRIP_PLAN_FILE_VERSION_2 = 2
