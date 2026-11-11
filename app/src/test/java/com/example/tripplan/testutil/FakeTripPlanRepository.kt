package com.example.tripplan.testutil

import com.example.tripplan.domain.model.TripPlan
import com.example.tripplan.domain.model.TripPlanInput
import com.example.tripplan.domain.repository.TripPlanRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.time.Instant

class FakeTripPlanRepository : TripPlanRepository {
    private val trips = MutableStateFlow<List<TripPlan>>(emptyList())
    val createdInputs = mutableListOf<TripPlanInput>()
    var nextCreatedId: Long = 1L
    var createFailure: Exception? = null
    var lastDeletedItineraryItemIds: List<Long> = emptyList()
    var budgetReset = false
    var containsExpenses = false

    override fun observeTripPlans(): Flow<List<TripPlan>> = trips

    override fun observeTripPlan(tripPlanId: Long): Flow<TripPlan?> {
        return trips.map { values -> values.firstOrNull { it.id == tripPlanId } }
    }

    fun setTrips(values: List<TripPlan>) {
        trips.value = values
    }

    override suspend fun getTripPlan(tripPlanId: Long): TripPlan? {
        return trips.value.firstOrNull { it.id == tripPlanId }
    }

    override suspend fun hasExpenses(tripPlanId: Long): Boolean = containsExpenses

    override suspend fun createTripPlan(input: TripPlanInput): Long {
        createFailure?.let { throw it }
        createdInputs += input
        return nextCreatedId
    }

    override suspend fun updateTripPlan(tripPlan: TripPlan) {
        setTrips(trips.value.map { current -> if (current.id == tripPlan.id) tripPlan else current })
    }

    override suspend fun updateTripCompletion(
        tripPlanId: Long,
        completedAt: Instant?,
        updatedAt: Instant,
    ): Boolean {
        val existing = trips.value.firstOrNull { it.id == tripPlanId } ?: return false
        updateTripPlan(existing.copy(completedAt = completedAt, updatedAt = updatedAt))
        return true
    }

    override suspend fun updateTripPlanAndDeleteItineraryItems(
        tripPlan: TripPlan,
        itemIds: List<Long>,
    ) {
        lastDeletedItineraryItemIds = itemIds
        setTrips(trips.value.map { current -> if (current.id == tripPlan.id) tripPlan else current })
    }

    override suspend fun updateTripPlanAndResetBudget(
        tripPlan: TripPlan,
        itemIds: List<Long>,
    ) {
        budgetReset = true
        updateTripPlanAndDeleteItineraryItems(tripPlan, itemIds)
    }

    override suspend fun deleteTripPlan(tripPlanId: Long) {
        setTrips(trips.value.filterNot { it.id == tripPlanId })
    }
}
