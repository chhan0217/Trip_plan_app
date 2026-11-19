package com.example.tripplan.domain.usecase

import com.example.tripplan.domain.model.ExternalPlaceDetails
import com.example.tripplan.domain.model.ExternalPlaceSuggestion
import com.example.tripplan.domain.repository.ExternalPlaceSearchRepository
import javax.inject.Inject

class SearchExternalPlacesUseCase @Inject constructor(
    private val repository: ExternalPlaceSearchRepository,
) {
    suspend operator fun invoke(countryCode: String, query: String): List<ExternalPlaceSuggestion> {
        val normalized = query.trim()
        if (normalized.length < 2) return emptyList()
        return repository.search(countryCode.uppercase(), normalized)
    }
}

class GetExternalPlaceDetailsUseCase @Inject constructor(
    private val repository: ExternalPlaceSearchRepository,
) {
    suspend operator fun invoke(externalId: String): ExternalPlaceDetails? =
        repository.getDetails(externalId)
}
