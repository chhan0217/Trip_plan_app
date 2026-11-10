package com.example.tripplan.data.mapper

import com.example.tripplan.data.local.ItineraryItemEntity
import com.example.tripplan.domain.model.ItineraryItem
import java.time.LocalDate
import java.time.LocalTime

fun ItineraryItemEntity.toDomain(): ItineraryItem = ItineraryItem(
    id = id,
    tripPlanId = tripPlanId,
    date = LocalDate.parse(date),
    title = title,
    place = place,
    startTime = startTime?.let(LocalTime::parse),
    endTime = endTime?.let(LocalTime::parse),
    note = note,
    order = sortOrder,
    placeId = placeId,
)

fun ItineraryItem.toEntity(): ItineraryItemEntity = ItineraryItemEntity(
    id = id,
    tripPlanId = tripPlanId,
    date = date.toString(),
    title = title,
    place = place,
    startTime = startTime?.toString(),
    endTime = endTime?.toString(),
    note = note,
    sortOrder = order,
    placeId = placeId,
)
