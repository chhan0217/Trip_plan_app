package com.example.tripplan.domain.model

data class ExternalPlaceSuggestion(
    val externalId: String,
    val name: String,
    val addressHint: String,
)

data class ExternalPlaceDetails(
    val externalId: String,
    val name: String,
    val address: String,
    val latitude: Double?,
    val longitude: Double?,
)
