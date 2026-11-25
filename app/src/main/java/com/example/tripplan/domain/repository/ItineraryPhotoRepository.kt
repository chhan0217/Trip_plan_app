package com.example.tripplan.domain.repository

import kotlinx.coroutines.flow.Flow

data class ItineraryPhotoRef(val id: Long, val itineraryId: Long)

interface ItineraryPhotoRepository {
    fun observe(tripId: Long): Flow<List<ItineraryPhotoRef>>
    suspend fun getJpeg(tripId: Long, photoId: Long): ByteArray?
    suspend fun add(tripId: Long, itineraryId: Long, source: String): Boolean
    suspend fun createCaptureTarget(): String
    suspend fun discardCaptureTarget(target: String)
}
