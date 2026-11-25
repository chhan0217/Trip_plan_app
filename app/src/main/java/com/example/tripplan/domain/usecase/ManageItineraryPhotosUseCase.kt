package com.example.tripplan.domain.usecase

import com.example.tripplan.domain.repository.ItineraryPhotoRepository
import javax.inject.Inject

class ManageItineraryPhotosUseCase @Inject constructor(private val photos: ItineraryPhotoRepository) {
    fun observe(tripId: Long) = photos.observe(tripId)

    suspend fun read(tripId: Long, photoId: Long) = photos.getJpeg(tripId, photoId)

    suspend fun add(tripId: Long, itineraryId: Long, source: String) = photos.add(tripId, itineraryId, source)

    suspend fun prepareCapture() = photos.createCaptureTarget()

    suspend fun discardCapture(target: String) = photos.discardCaptureTarget(target)
}
