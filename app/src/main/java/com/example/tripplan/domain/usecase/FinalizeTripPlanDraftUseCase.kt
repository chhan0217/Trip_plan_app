package com.example.tripplan.domain.usecase

import com.example.tripplan.domain.repository.TripPlanRepository
import java.time.Clock
import javax.inject.Inject

class FinalizeTripPlanDraftUseCase @Inject constructor(
    private val repository: TripPlanRepository,
    private val clock: Clock,
) {
    suspend operator fun invoke(tripPlanId: Long): Boolean =
        repository.updateTripDraftStatus(
            tripPlanId = tripPlanId,
            isDraft = false,
            updatedAt = clock.instant(),
        )
}
