package com.example.tripplan.data.mapper

import com.example.tripplan.data.local.TripPlanEntity
import com.example.tripplan.domain.model.TripPlan

fun TripPlanEntity.toDomain(): TripPlan = TripPlan(
    id = id,
    title = title,
    startDate = startDate,
    endDate = endDate,
)

fun TripPlan.toEntity(): TripPlanEntity = TripPlanEntity(
    id = id,
    title = title,
    startDate = startDate,
    endDate = endDate,
)
