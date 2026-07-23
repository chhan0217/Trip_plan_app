package com.example.tripplan.domain.usecase

import com.example.tripplan.domain.model.TripPlan
import com.example.tripplan.domain.repository.TripPlanRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class GetTripPlansUseCase @Inject constructor(
    private val repository: TripPlanRepository,
) {
    operator fun invoke(): Flow<List<TripPlan>> = repository.observeTripPlans()
}
