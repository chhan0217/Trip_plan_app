package com.example.tripplan.testutil

import com.example.tripplan.domain.model.Place
import com.example.tripplan.domain.repository.PlaceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakePlaceRepository : PlaceRepository {
    private val values = MutableStateFlow<List<Place>>(emptyList())
    fun setPlaces(items: List<Place>) { values.value = items }
    override fun observePlaces(): Flow<List<Place>> = values
    override suspend fun getPlace(placeId: Long): Place? = values.value.find { it.id == placeId }
    override suspend fun createPlace(place: Place): Long { val id=(values.value.maxOfOrNull{it.id}?:0)+1; values.value+=place.copy(id=id); return id }
    override suspend fun updatePlace(place: Place) { values.value=values.value.map{if(it.id==place.id)place else it} }
    override suspend fun deletePlace(placeId: Long) { values.value=values.value.filterNot{it.id==placeId} }
}
