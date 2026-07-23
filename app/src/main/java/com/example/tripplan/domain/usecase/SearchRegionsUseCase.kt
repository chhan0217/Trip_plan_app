package com.example.tripplan.domain.usecase

import com.example.tripplan.domain.model.Region
import com.example.tripplan.domain.repository.RegionRepository
import javax.inject.Inject

class SearchRegionsUseCase @Inject constructor(
    private val repository: RegionRepository,
) {
    suspend operator fun invoke(query: String, limit: Int = DEFAULT_LIMIT): List<Region> {
        val normalizedQuery = query.trim()
        if (normalizedQuery.length < MIN_QUERY_LENGTH) return emptyList()
        return repository.searchRegions(normalizedQuery, limit)
    }

    private companion object {
        const val MIN_QUERY_LENGTH = 2
        const val DEFAULT_LIMIT = 10
    }
}
