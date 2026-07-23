package com.example.tripplan.domain.repository

import com.example.tripplan.domain.model.TripPlan
import kotlinx.coroutines.flow.Flow

interface TripPlanRepository {
    fun observeTripPlans(): Flow<List<TripPlan>>
}
