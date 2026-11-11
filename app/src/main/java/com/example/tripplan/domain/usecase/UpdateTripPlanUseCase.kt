package com.example.tripplan.domain.usecase

import com.example.tripplan.domain.model.TripPlanInput
import com.example.tripplan.domain.repository.ItineraryRepository
import com.example.tripplan.domain.repository.TripPlanRepository
import com.example.tripplan.domain.repository.TripBudgetRepository
import com.example.tripplan.domain.rule.TripPlanValidationError
import com.example.tripplan.domain.rule.TripPlanValidator
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.first

class UpdateTripPlanUseCase @Inject constructor(
    private val tripPlanRepository: TripPlanRepository,
    private val itineraryRepository: ItineraryRepository,
    private val clock: Clock,
    private val tripBudgetRepository: TripBudgetRepository,
) {
    suspend operator fun invoke(
        tripPlanId: Long,
        input: TripPlanInput,
        deleteOutOfRangeItems: Boolean = false,
        clearCompletion: Boolean = false,
        resetBudgetForCurrencyChange: Boolean = false,
    ): UpdateTripPlanResult {
        val errors = TripPlanValidator.validate(input)
        if (errors.isNotEmpty()) return UpdateTripPlanResult.Invalid(errors)
        val existing = tripPlanRepository.getTripPlan(tripPlanId)
            ?: return UpdateTripPlanResult.NotFound
        val currencyChanged = existing.countryCode != input.countryCode ||
            existing.currencyCode != input.currencyCode
        if (
            currencyChanged &&
            (tripBudgetRepository.getBudget(tripPlanId) != null ||
                tripPlanRepository.hasExpenses(tripPlanId)) &&
            !resetBudgetForCurrencyChange
        ) {
            return UpdateTripPlanResult.RequiresBudgetResetConfirmation
        }
        val dateChanged = existing.startDate != input.startDate || existing.endDate != input.endDate
        val requiresCompletionReset = existing.completedAt != null && dateChanged &&
            !input.endDate.isBefore(LocalDate.now(clock))
        if (requiresCompletionReset && !clearCompletion) {
            return UpdateTripPlanResult.RequiresCompletionResetConfirmation
        }
        val outOfRangeItems = itineraryRepository.observeItems(tripPlanId).first()
            .filter { it.date.isBefore(input.startDate) || it.date.isAfter(input.endDate) }
        if (outOfRangeItems.isNotEmpty() && !deleteOutOfRangeItems) {
            return UpdateTripPlanResult.RequiresOutOfRangeConfirmation(outOfRangeItems.size)
        }

        val updated = existing.copy(
            title = input.title.trim(),
            destination = input.destination.trim(),
            startDate = input.startDate,
            endDate = input.endDate,
            description = input.description.trim(),
            updatedAt = clock.instant(),
            completedAt = if (clearCompletion) null else existing.completedAt,
            countryCode = input.countryCode,
            currencyCode = input.currencyCode,
        )
        if (currencyChanged && resetBudgetForCurrencyChange) {
            tripPlanRepository.updateTripPlanAndResetBudget(updated, outOfRangeItems.map { it.id })
        } else {
            tripPlanRepository.updateTripPlanAndDeleteItineraryItems(
                updated,
                outOfRangeItems.map { it.id },
            )
        }
        return UpdateTripPlanResult.Success(tripPlanId)
    }
}

sealed interface UpdateTripPlanResult {
    data class Success(val tripPlanId: Long) : UpdateTripPlanResult
    data class Invalid(val errors: Set<TripPlanValidationError>) : UpdateTripPlanResult
    data class RequiresOutOfRangeConfirmation(val itemCount: Int) : UpdateTripPlanResult
    data object RequiresCompletionResetConfirmation : UpdateTripPlanResult
    data object RequiresBudgetResetConfirmation : UpdateTripPlanResult
    data object NotFound : UpdateTripPlanResult
}
