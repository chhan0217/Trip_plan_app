package com.example.tripplan.data.repository

import com.example.tripplan.data.local.TripPlanDao
import com.example.tripplan.data.mapper.toDomain
import com.example.tripplan.domain.model.TripPlan
import com.example.tripplan.domain.repository.TripPlanRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class DefaultTripPlanRepository @Inject constructor(
    private val tripPlanDao: TripPlanDao,
) : TripPlanRepository {
    override fun observeTripPlans(): Flow<List<TripPlan>> {
        return tripPlanDao.observeTripPlans()
            .map { tripPlans -> tripPlans.map { it.toDomain() } }
    }
}
