package com.example.tripplan.domain.usecase

import com.example.tripplan.domain.model.ItineraryCost
import com.example.tripplan.domain.model.ItineraryCostInput
import com.example.tripplan.domain.repository.ItineraryRepository
import com.example.tripplan.domain.repository.TripBudgetRepository
import com.example.tripplan.domain.rule.BudgetCalculator
import java.math.BigDecimal
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

class UpdateItineraryCostsUseCase @Inject constructor(
    private val itineraryRepository: ItineraryRepository,
    private val budgetRepository: TripBudgetRepository,
    private val clock: Clock,
) {
    suspend operator fun invoke(
        itemId: Long,
        estimatedCost: ItineraryCostInput?,
    ): ItineraryCostMutationResult {
        val item = itineraryRepository.getItem(itemId)
            ?: return ItineraryCostMutationResult.ItemNotFound
        val budget = budgetRepository.getBudget(item.tripPlanId)
            ?: return ItineraryCostMutationResult.BudgetNotFound
        val allowedCurrencies = setOf(budget.currencyCode, "KRW")
        if (estimatedCost != null && estimatedCost.currencyCode !in allowedCurrencies) {
            return ItineraryCostMutationResult.InvalidCurrency
        }
        if (estimatedCost != null && (
                estimatedCost.amount <= BigDecimal.ZERO || estimatedCost.krwPerUnit <= BigDecimal.ZERO ||
                    (estimatedCost.currencyCode == "KRW" &&
                        estimatedCost.krwPerUnit.compareTo(BigDecimal.ONE) != 0)
                )
        ) {
            return ItineraryCostMutationResult.InvalidAmountOrRate
        }
        val rateDate = LocalDate.now(clock)
        itineraryRepository.updateItem(
            item.copy(
                estimatedCost = estimatedCost?.toSnapshot(rateDate),
                actualCost = null,
            )
        )
        return ItineraryCostMutationResult.Success
    }

    private fun ItineraryCostInput.toSnapshot(rateDate: LocalDate) = ItineraryCost(
        amount = amount,
        currencyCode = currencyCode,
        krwPerUnit = krwPerUnit,
        rateDate = rateDate,
        convertedKrwAmount = BudgetCalculator.convertToKrw(amount, krwPerUnit),
    )
}

sealed interface ItineraryCostMutationResult {
    data object Success : ItineraryCostMutationResult
    data object ItemNotFound : ItineraryCostMutationResult
    data object BudgetNotFound : ItineraryCostMutationResult
    data object InvalidCurrency : ItineraryCostMutationResult
    data object InvalidAmountOrRate : ItineraryCostMutationResult
}
