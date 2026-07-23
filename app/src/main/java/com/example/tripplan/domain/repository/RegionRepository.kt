package com.example.tripplan.domain.repository

import com.example.tripplan.domain.model.Region

interface RegionRepository {
    suspend fun searchRegions(query: String, limit: Int): List<Region>
}
