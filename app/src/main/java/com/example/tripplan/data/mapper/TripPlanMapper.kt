package com.example.tripplan.data.mapper

import com.example.tripplan.data.local.TripPlanEntity
import com.example.tripplan.domain.model.TripPlan
import java.time.Instant
import java.time.LocalDate

fun TripPlanEntity.toDomain(): TripPlan = TripPlan(
    id = id,
    title = title,
    destination = destination,
    startDate = LocalDate.parse(startDate),
    endDate = LocalDate.parse(endDate),
    description = description,
    createdAt = Instant.ofEpochMilli(createdAt),
    updatedAt = Instant.ofEpochMilli(updatedAt),
    completedAt = completedAt?.let(Instant::ofEpochMilli),
)

fun TripPlan.toEntity(): TripPlanEntity = TripPlanEntity(
    id = id,
    title = title,
    destination = destination,
    startDate = startDate.toString(),
    endDate = endDate.toString(),
    description = description,
    createdAt = createdAt.toEpochMilli(),
    updatedAt = updatedAt.toEpochMilli(),
    completedAt = completedAt?.toEpochMilli(),
)
