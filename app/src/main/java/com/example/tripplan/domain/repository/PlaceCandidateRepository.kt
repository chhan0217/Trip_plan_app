package com.example.tripplan.domain.repository

import com.example.tripplan.domain.model.Place
import com.example.tripplan.domain.model.PlaceCandidate
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

interface PlaceCandidateRepository {
    fun observe(tripId: Long): Flow<List<PlaceCandidate>>
    suspend fun add(tripId: Long, name: String, address: String, note: String, place: Place?): Boolean
    suspend fun schedule(tripId: Long, candidateId: Long, date: LocalDate): Boolean
    suspend fun remove(tripId: Long, candidateId: Long): Boolean
}
