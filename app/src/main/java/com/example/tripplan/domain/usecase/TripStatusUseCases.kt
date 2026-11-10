package com.example.tripplan.domain.usecase

import com.example.tripplan.domain.model.TripPlan
import com.example.tripplan.domain.model.TripStatus
import com.example.tripplan.domain.repository.TripPlanRepository
import com.example.tripplan.domain.rule.TripDateStatusCalculator
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

class GetTripStatusUseCase @Inject constructor(
    private val clock: Clock,
) {
    operator fun invoke(tripPlan: TripPlan): TripStatus = TripStatus(
        dateStatus = TripDateStatusCalculator.calculate(
            startDate = tripPlan.startDate,
            endDate = tripPlan.endDate,
            today = LocalDate.now(clock),
        ),
        isCompleted = tripPlan.completedAt != null,
    )
}

class CompleteTripPlanUseCase @Inject constructor(
    private val repository: TripPlanRepository,
    private val clock: Clock,
) {
    suspend operator fun invoke(tripPlanId: Long): TripCompletionResult {
        val now = clock.instant()
        return if (repository.updateTripCompletion(tripPlanId, now, now)) {
            TripCompletionResult.Success
        } else {
            TripCompletionResult.NotFound
        }
    }
}

class UndoTripCompletionUseCase @Inject constructor(
    private val repository: TripPlanRepository,
    private val clock: Clock,
) {
    suspend operator fun invoke(tripPlanId: Long): TripCompletionResult {
        return if (repository.updateTripCompletion(tripPlanId, null, clock.instant())) {
            TripCompletionResult.Success
        } else {
            TripCompletionResult.NotFound
        }
    }
}

enum class TripCompletionResult {
    Success,
    NotFound,
}
