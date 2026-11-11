package com.example.tripplan.domain.usecase

import com.example.tripplan.domain.model.AddBudgetEntryInput
import com.example.tripplan.domain.model.BudgetEntry
import com.example.tripplan.domain.model.BudgetEntryType
import com.example.tripplan.domain.model.CreateTripBudgetInput
import com.example.tripplan.domain.model.TripBudget
import com.example.tripplan.domain.repository.TripBudgetRepository
import com.example.tripplan.domain.rule.BudgetCalculator
import com.example.tripplan.domain.rule.BudgetValidationError
import java.time.Clock
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class ObserveTripBudgetUseCase @Inject constructor(
    private val repository: TripBudgetRepository,
) {
    operator fun invoke(tripPlanId: Long): Flow<TripBudget?> = repository.observeBudget(tripPlanId)
}

class CreateTripBudgetUseCase @Inject constructor(
    private val repository: TripBudgetRepository,
    private val clock: Clock,
) {
    suspend operator fun invoke(
        tripPlanId: Long,
        input: CreateTripBudgetInput,
    ): BudgetMutationResult {
        val normalized = input.copy(currencyCode = input.currencyCode.trim().uppercase())
        val errors = BudgetCalculator.validate(normalized)
        if (errors.isNotEmpty()) return BudgetMutationResult.Invalid(errors)
        if (repository.getBudget(tripPlanId) != null) return BudgetMutationResult.BudgetAlreadyExists
        val now = clock.instant()
        val id = repository.createBudget(
            tripPlanId = tripPlanId,
            currencyCode = normalized.currencyCode,
            initialEntry = BudgetEntry(
                id = 0,
                tripPlanId = tripPlanId,
                type = BudgetEntryType.Initial,
                amount = BudgetCalculator.convertFromKrw(
                    normalized.krwAmount,
                    normalized.krwPerUnit,
                ),
                krwPerUnit = normalized.krwPerUnit,
                rateDate = normalized.rateDate,
                convertedKrwAmount = normalized.krwAmount.setScale(0, java.math.RoundingMode.HALF_UP),
                memo = normalized.memo.trim(),
                order = 0,
                createdAt = now,
                updatedAt = now,
            ),
        )
        return BudgetMutationResult.Success(id)
    }
}

class AddTripBudgetEntryUseCase @Inject constructor(
    private val repository: TripBudgetRepository,
    private val clock: Clock,
) {
    suspend operator fun invoke(
        tripPlanId: Long,
        input: AddBudgetEntryInput,
    ): BudgetMutationResult {
        val errors = BudgetCalculator.validate(input)
        if (errors.isNotEmpty()) return BudgetMutationResult.Invalid(errors)
        val budget = repository.getBudget(tripPlanId) ?: return BudgetMutationResult.BudgetNotFound
        if (budget.currencyCode == "KRW" && input.krwPerUnit.compareTo(java.math.BigDecimal.ONE) != 0) {
            return BudgetMutationResult.Invalid(setOf(BudgetValidationError.InvalidKrwExchangeRate))
        }
        val now = clock.instant()
        val id = repository.addEntry(
            BudgetEntry(
                id = 0,
                tripPlanId = tripPlanId,
                type = BudgetEntryType.Additional,
                amount = BudgetCalculator.convertFromKrw(input.krwAmount, input.krwPerUnit),
                krwPerUnit = input.krwPerUnit,
                rateDate = input.rateDate,
                convertedKrwAmount = input.krwAmount.setScale(0, java.math.RoundingMode.HALF_UP),
                memo = input.memo.trim(),
                order = budget.entries.maxOfOrNull { it.order }?.plus(1) ?: 1,
                createdAt = now,
                updatedAt = now,
            )
        )
        return BudgetMutationResult.Success(id)
    }
}

sealed interface BudgetMutationResult {
    data class Success(val entryId: Long) : BudgetMutationResult
    data class Invalid(val errors: Set<BudgetValidationError>) : BudgetMutationResult
    data object BudgetAlreadyExists : BudgetMutationResult
    data object BudgetNotFound : BudgetMutationResult
}
