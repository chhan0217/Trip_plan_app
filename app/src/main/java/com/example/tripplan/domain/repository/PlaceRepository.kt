package com.example.tripplan.domain.repository

import com.example.tripplan.domain.model.Place
import kotlinx.coroutines.flow.Flow

interface PlaceRepository {
    fun observePlaces(): Flow<List<Place>>
    suspend fun getPlace(placeId: Long): Place?
    suspend fun createPlace(place: Place): Long
    suspend fun updatePlace(place: Place)
    suspend fun deletePlace(placeId: Long)
}
