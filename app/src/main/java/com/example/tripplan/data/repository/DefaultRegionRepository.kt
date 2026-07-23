package com.example.tripplan.data.repository

import com.example.tripplan.data.local.RegionDao
import com.example.tripplan.data.mapper.toDomain
import com.example.tripplan.domain.model.Region
import com.example.tripplan.domain.repository.RegionRepository
import javax.inject.Inject

class DefaultRegionRepository @Inject constructor(
    private val regionDao: RegionDao,
) : RegionRepository {
    override suspend fun searchRegions(query: String, limit: Int): List<Region> {
        return regionDao.searchRegions(query = query, limit = limit)
            .map { it.toDomain() }
    }
}
