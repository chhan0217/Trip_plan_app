package com.example.tripplan.domain.usecase

import com.example.tripplan.domain.model.ExternalPlaceDetails
import com.example.tripplan.domain.model.Place
import com.example.tripplan.domain.model.PlaceSource
import com.example.tripplan.domain.repository.PlaceCandidateRepository
import com.example.tripplan.domain.repository.PlaceRepository
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

class ManagePlaceCandidatesUseCase @Inject constructor(
    private val candidates: PlaceCandidateRepository,
    private val places: PlaceRepository,
    private val clock: Clock,
) {
    fun observe(tripId: Long) = candidates.observe(tripId)

    suspend fun add(
        tripId: Long,
        name: String,
        address: String,
        note: String,
        savedPlaceId: Long?,
        externalPlace: ExternalPlaceDetails?,
    ): Boolean {
        if (name.isBlank()) return false
        val place = when {
            savedPlaceId != null -> places.getPlace(savedPlaceId) ?: return false
            externalPlace != null -> {
                val now = clock.instant()
                Place(
                    id = 0,
                    name = externalPlace.name,
                    address = externalPlace.address,
                    latitude = externalPlace.latitude,
                    longitude = externalPlace.longitude,
                    source = PlaceSource.ExternalProvider,
                    providerName = "Google Places",
                    externalId = externalPlace.externalId,
                    createdAt = now,
                    updatedAt = now,
                )
            }
            else -> null
        }
        val resolvedAddress = place?.address?.takeIf(String::isNotBlank) ?: address.trim()
        return candidates.add(tripId, name.trim(), resolvedAddress, note.trim(), place)
    }

    suspend fun schedule(tripId: Long, candidateId: Long, date: LocalDate) =
        candidates.schedule(tripId, candidateId, date)

    suspend fun remove(tripId: Long, candidateId: Long) = candidates.remove(tripId, candidateId)
}
