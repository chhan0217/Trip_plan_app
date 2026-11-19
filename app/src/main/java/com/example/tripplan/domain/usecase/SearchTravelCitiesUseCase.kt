package com.example.tripplan.domain.usecase

import com.example.tripplan.domain.model.TravelCity
import com.example.tripplan.domain.repository.TravelCityRepository
import javax.inject.Inject

class SearchTravelCitiesUseCase @Inject constructor(
    private val repository: TravelCityRepository,
) {
    suspend operator fun invoke(countryCode: String, query: String): List<TravelCity> {
        val normalizedQuery = query.trim()
        if (normalizedQuery.length < MIN_QUERY_LENGTH) return emptyList()
        return repository.search(countryCode.uppercase(), normalizedQuery)
    }

    private companion object {
        const val MIN_QUERY_LENGTH = 2
    }
}
