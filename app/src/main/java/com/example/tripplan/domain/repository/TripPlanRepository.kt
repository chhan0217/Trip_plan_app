package com.example.tripplan.domain.repository

import com.example.tripplan.domain.model.TripPlan
import com.example.tripplan.domain.model.TripPlanInput
import kotlinx.coroutines.flow.Flow
import java.time.Instant

interface TripPlanRepository {
    fun observeTripPlans(): Flow<List<TripPlan>>
    fun observeTripPlan(tripPlanId: Long): Flow<TripPlan?>
    suspend fun getTripPlan(tripPlanId: Long): TripPlan?
    suspend fun hasExpenses(tripPlanId: Long): Boolean = false
    suspend fun createTripPlan(input: TripPlanInput): Long
    suspend fun updateTripPlan(tripPlan: TripPlan)
    suspend fun updateTripCompletion(
        tripPlanId: Long,
        completedAt: Instant?,
        updatedAt: Instant,
    ): Boolean
    suspend fun updateTripPlanAndDeleteItineraryItems(
        tripPlan: TripPlan,
        itemIds: List<Long>,
    )
    suspend fun updateTripPlanAndResetBudget(
        tripPlan: TripPlan,
        itemIds: List<Long>,
    )
    suspend fun deleteTripPlan(tripPlanId: Long)
}
