package com.example.tripplan.domain.usecase

import com.example.tripplan.domain.model.TripPlanInput
import com.example.tripplan.domain.repository.TripPlanRepository
import com.example.tripplan.domain.rule.TripPlanValidationError
import com.example.tripplan.domain.rule.TripPlanValidator
import javax.inject.Inject

class CreateTripPlanUseCase @Inject constructor(
    private val repository: TripPlanRepository,
) {
    suspend operator fun invoke(input: TripPlanInput): CreateTripPlanResult {
        val errors = TripPlanValidator.validate(input)
        if (errors.isNotEmpty()) {
            return CreateTripPlanResult.Invalid(errors)
        }

        return CreateTripPlanResult.Success(
            tripPlanId = repository.createTripPlan(input)
        )
    }
}

sealed interface CreateTripPlanResult {
    data class Success(val tripPlanId: Long) : CreateTripPlanResult

    data class Invalid(
        val errors: Set<TripPlanValidationError>,
    ) : CreateTripPlanResult
}
