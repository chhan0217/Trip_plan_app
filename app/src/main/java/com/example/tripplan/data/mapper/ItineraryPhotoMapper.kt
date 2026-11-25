package com.example.tripplan.data.mapper

import com.example.tripplan.data.local.ItineraryPhotoRow
import com.example.tripplan.domain.repository.ItineraryPhotoRef

fun ItineraryPhotoRow.toDomain() = ItineraryPhotoRef(id, itineraryId)
