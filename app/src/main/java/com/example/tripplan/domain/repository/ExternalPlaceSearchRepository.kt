package com.example.tripplan.domain.repository

import com.example.tripplan.domain.model.ExternalPlaceDetails
import com.example.tripplan.domain.model.ExternalPlaceSuggestion

interface ExternalPlaceSearchRepository {
    suspend fun search(countryCode: String, query: String): List<ExternalPlaceSuggestion>
    suspend fun getDetails(externalId: String): ExternalPlaceDetails?
}
