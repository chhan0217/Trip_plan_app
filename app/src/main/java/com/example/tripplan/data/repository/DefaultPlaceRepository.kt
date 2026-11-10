package com.example.tripplan.data.repository

import com.example.tripplan.data.local.PlaceDao
import com.example.tripplan.data.mapper.toDomain
import com.example.tripplan.data.mapper.toEntity
import com.example.tripplan.domain.model.Place
import com.example.tripplan.domain.repository.PlaceRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class DefaultPlaceRepository @Inject constructor(private val dao: PlaceDao) : PlaceRepository {
    override fun observePlaces(): Flow<List<Place>> = dao.observePlaces().map { list -> list.map { it.toDomain() } }
    override suspend fun getPlace(placeId: Long): Place? = dao.getPlace(placeId)?.toDomain()
    override suspend fun createPlace(place: Place): Long = dao.insertPlace(place.toEntity())
    override suspend fun updatePlace(place: Place) = dao.updatePlace(place.toEntity())
    override suspend fun deletePlace(placeId: Long) = dao.deletePlace(placeId)
}
