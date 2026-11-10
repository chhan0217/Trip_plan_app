package com.example.tripplan.domain.model

import java.time.Instant

enum class PlaceSource {
    Manual,
    MapPin,
    ExternalProvider,
}

data class Place(
    val id: Long,
    val name: String,
    val address: String,
    val latitude: Double?,
    val longitude: Double?,
    val source: PlaceSource,
    val providerName: String?,
    val externalId: String?,
    val createdAt: Instant,
    val updatedAt: Instant,
)

data class PlaceInput(
    val name: String,
    val address: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val source: PlaceSource = PlaceSource.Manual,
    val providerName: String? = null,
    val externalId: String? = null,
)
