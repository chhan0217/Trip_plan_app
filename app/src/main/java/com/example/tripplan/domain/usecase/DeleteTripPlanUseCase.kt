package com.example.tripplan.domain.usecase

import com.example.tripplan.domain.repository.TripPlanRepository
import javax.inject.Inject

class DeleteTripPlanUseCase @Inject constructor(
    private val tripPlanRepository: TripPlanRepository,
) {
    suspend operator fun invoke(tripPlanId: Long): DeleteTripPlanResult {
        if (tripPlanRepository.getTripPlan(tripPlanId) == null) {
            return DeleteTripPlanResult.NotFound
        }
        tripPlanRepository.deleteTripPlan(tripPlanId)
        return DeleteTripPlanResult.Success
    }
}

sealed interface DeleteTripPlanResult {
    data object Success : DeleteTripPlanResult
    data object NotFound : DeleteTripPlanResult
}
