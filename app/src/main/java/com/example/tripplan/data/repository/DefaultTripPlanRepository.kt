package com.example.tripplan.data.repository

import com.example.tripplan.data.local.TripPlanDao
import com.example.tripplan.data.mapper.toDomain
import com.example.tripplan.data.mapper.toEntity
import com.example.tripplan.data.local.TripPlanEntity
import com.example.tripplan.domain.model.TripPlan
import com.example.tripplan.domain.model.TripPlanInput
import com.example.tripplan.domain.repository.TripPlanRepository
import java.time.Clock
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class DefaultTripPlanRepository @Inject constructor(
    private val tripPlanDao: TripPlanDao,
    private val clock: Clock,
) : TripPlanRepository {
    override fun observeTripPlans(): Flow<List<TripPlan>> {
        return tripPlanDao.observeTripPlans()
            .map { tripPlans -> tripPlans.map { it.toDomain() } }
    }

    override fun observeTripPlan(tripPlanId: Long): Flow<TripPlan?> {
        return tripPlanDao.observeTripPlan(tripPlanId).map { it?.toDomain() }
    }

    override suspend fun getTripPlan(tripPlanId: Long): TripPlan? {
        return tripPlanDao.getTripPlan(tripPlanId)?.toDomain()
    }

    override suspend fun createTripPlan(input: TripPlanInput): Long {
        val now = clock.instant().toEpochMilli()
        return tripPlanDao.insertTripPlan(
            TripPlanEntity(
                title = input.title.trim(),
                destination = input.destination.trim(),
                startDate = input.startDate.toString(),
                endDate = input.endDate.toString(),
                description = input.description.trim(),
                createdAt = now,
                updatedAt = now,
            )
        )
    }

    override suspend fun updateTripPlan(tripPlan: TripPlan) {
        tripPlanDao.updateTripPlan(tripPlan.toEntity())
    }

    override suspend fun updateTripCompletion(
        tripPlanId: Long,
        completedAt: Instant?,
        updatedAt: Instant,
    ): Boolean = tripPlanDao.updateTripCompletion(
        tripPlanId = tripPlanId,
        completedAt = completedAt?.toEpochMilli(),
        updatedAt = updatedAt.toEpochMilli(),
    ) > 0

    override suspend fun updateTripPlanAndDeleteItineraryItems(
        tripPlan: TripPlan,
        itemIds: List<Long>,
    ) {
        tripPlanDao.updateTripPlanAndDeleteItineraryItems(tripPlan.toEntity(), itemIds)
    }

    override suspend fun deleteTripPlan(tripPlanId: Long) {
        tripPlanDao.deleteTripPlan(tripPlanId)
    }
}
