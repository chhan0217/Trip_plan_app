package com.example.tripplan.domain.repository

import com.example.tripplan.domain.model.Lodging
import kotlinx.coroutines.flow.Flow

enum class LodgingSaveResult { Saved, InvalidDates, Overlap, Unavailable }

interface LodgingRepository {
    fun observe(tripId: Long): Flow<List<Lodging>>
    suspend fun save(tripId: Long, lodging: Lodging): LodgingSaveResult
    suspend fun remove(tripId: Long, id: Long): Boolean
}
