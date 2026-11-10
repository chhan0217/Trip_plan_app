package com.example.tripplan.data.mapper

import com.example.tripplan.data.file.CURRENT_TRIP_PLAN_FILE_VERSION
import com.example.tripplan.data.file.SharedItineraryItemDto
import com.example.tripplan.data.file.SharedTripDto
import com.example.tripplan.data.file.TripPlanFileDto
import com.example.tripplan.data.local.ItineraryItemEntity
import com.example.tripplan.data.local.TripPlanEntity

object TripPlanFileMapper {
    fun toFileDto(
        trip: TripPlanEntity,
        items: List<ItineraryItemEntity>,
    ) = TripPlanFileDto(
        formatVersion = CURRENT_TRIP_PLAN_FILE_VERSION,
        trip = SharedTripDto(
            title = trip.title,
            destination = trip.destination,
            startDate = trip.startDate,
            endDate = trip.endDate,
            description = trip.description,
        ),
        itineraryItems = items.map {
            SharedItineraryItemDto(
                date = it.date,
                title = it.title,
                place = it.place,
                startTime = it.startTime,
                endTime = it.endTime,
                note = it.note,
                order = it.sortOrder,
            )
        },
    )

    fun toTripEntity(dto: SharedTripDto, now: Long) = TripPlanEntity(
        title = dto.title.trim(),
        destination = dto.destination.trim(),
        startDate = dto.startDate,
        endDate = dto.endDate,
        description = dto.description.trim(),
        createdAt = now,
        updatedAt = now,
    )

    fun toItineraryEntity(dto: SharedItineraryItemDto, tripPlanId: Long) =
        ItineraryItemEntity(
            tripPlanId = tripPlanId,
            date = dto.date,
            title = dto.title.trim(),
            place = dto.place.trim(),
            startTime = dto.startTime,
            endTime = dto.endTime,
            note = dto.note.trim(),
            sortOrder = dto.order,
        )
}
