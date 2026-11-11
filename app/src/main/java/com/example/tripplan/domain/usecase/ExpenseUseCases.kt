package com.example.tripplan.domain.usecase

import com.example.tripplan.domain.model.Expense
import com.example.tripplan.domain.model.RecordExpenseInput
import com.example.tripplan.domain.model.UpdateExpenseInput
import com.example.tripplan.domain.repository.ExpenseRepository
import com.example.tripplan.domain.rule.BudgetCalculator
import java.math.BigDecimal
import java.time.Clock
import javax.inject.Inject

class ObserveExpensesUseCase @Inject constructor(
    private val repository: ExpenseRepository,
) {
    operator fun invoke(tripPlanId: Long) = repository.observeExpenses(tripPlanId)
}

class RecordExpenseUseCase @Inject constructor(
    private val repository: ExpenseRepository,
    private val clock: Clock,
) {
    suspend operator fun invoke(input: RecordExpenseInput): RecordExpenseResult {
        if (input.amount <= BigDecimal.ZERO || input.krwPerUnit <= BigDecimal.ZERO) {
            return RecordExpenseResult.InvalidAmount
        }
        val id = repository.addExpense(
            Expense(
                id = 0,
                tripPlanId = input.tripPlanId,
                itineraryItemId = input.itineraryItemId,
                occurredDate = input.occurredDate,
                occurredTime = input.occurredTime,
                amount = input.amount,
                currencyCode = input.currencyCode,
                krwPerUnit = input.krwPerUnit,
                rateDate = input.rateDate,
                convertedKrwAmount = if (input.currencyCode == "KRW") {
                    input.amount.setScale(0, java.math.RoundingMode.HALF_UP)
                } else {
                    BudgetCalculator.convertToKrw(input.amount, input.krwPerUnit)
                },
                category = input.category,
                createdAt = clock.instant(),
            )
        )
        return RecordExpenseResult.Success(id)
    }
}

sealed interface RecordExpenseResult {
    data class Success(val expenseId: Long) : RecordExpenseResult
    data object InvalidAmount : RecordExpenseResult
    data object InvalidRate : RecordExpenseResult
}

class UpdateExpenseUseCase @Inject constructor(
    private val repository: ExpenseRepository,
) {
    suspend operator fun invoke(input: UpdateExpenseInput): ExpenseMutationResult {
        if (input.amount <= BigDecimal.ZERO) return ExpenseMutationResult.InvalidAmount
        val original = repository.getExpense(input.expenseId)
            ?: return ExpenseMutationResult.NotFound
        if (original.tripPlanId != input.tripPlanId) return ExpenseMutationResult.NotFound
        val convertedKrwAmount = if (input.currencyCode == "KRW") {
            input.amount.setScale(0, java.math.RoundingMode.HALF_UP)
        } else {
            BudgetCalculator.convertToKrw(input.amount, original.krwPerUnit)
        }
        val updated = original.copy(
            itineraryItemId = input.itineraryItemId,
            occurredDate = input.occurredDate,
            occurredTime = input.occurredTime,
            amount = input.amount,
            currencyCode = input.currencyCode,
            convertedKrwAmount = convertedKrwAmount,
            category = input.category,
        )
        return if (repository.updateExpense(updated)) {
            ExpenseMutationResult.Success
        } else {
            ExpenseMutationResult.NotFound
        }
    }
}

class DeleteExpenseUseCase @Inject constructor(
    private val repository: ExpenseRepository,
) {
    suspend operator fun invoke(expenseId: Long): ExpenseMutationResult =
        if (repository.deleteExpense(expenseId)) {
            ExpenseMutationResult.Success
        } else {
            ExpenseMutationResult.NotFound
        }
}

sealed interface ExpenseMutationResult {
    data object Success : ExpenseMutationResult
    data object InvalidAmount : ExpenseMutationResult
    data object NotFound : ExpenseMutationResult
}
