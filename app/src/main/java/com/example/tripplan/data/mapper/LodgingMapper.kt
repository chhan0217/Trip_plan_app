package com.example.tripplan.data.mapper

import com.example.tripplan.data.local.LodgingEntity
import com.example.tripplan.domain.model.Lodging
import java.time.LocalDate
import java.time.LocalTime

fun LodgingEntity.toDomain() = Lodging(
    id = id,
    name = name,
    checkIn = LocalDate.parse(checkIn),
    checkOut = LocalDate.parse(checkOut),
    address = address,
    checkInTime = checkInTime?.let(LocalTime::parse),
    note = note,
    checkOutTime = checkOutTime?.let(LocalTime::parse),
    latitude = latitude,
    longitude = longitude,
    googlePlaceId = googlePlaceId,
)

fun Lodging.toEntity(tripId: Long) = LodgingEntity(
    id = id,
    tripPlanId = tripId,
    name = name,
    checkIn = checkIn.toString(),
    checkOut = checkOut.toString(),
    address = address,
    checkInTime = checkInTime?.toString(),
    note = note,
    checkOutTime = checkOutTime?.toString(),
    latitude = latitude,
    longitude = longitude,
    googlePlaceId = googlePlaceId,
)
