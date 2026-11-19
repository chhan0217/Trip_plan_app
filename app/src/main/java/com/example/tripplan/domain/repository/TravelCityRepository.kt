package com.example.tripplan.domain.repository

import com.example.tripplan.domain.model.TravelCity

interface TravelCityRepository {
    suspend fun search(countryCode: String, query: String): List<TravelCity>
}
