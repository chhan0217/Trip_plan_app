package com.example.tripplan.domain.usecase

import com.example.tripplan.domain.model.ExpenseCategory
import com.example.tripplan.domain.model.RecordExpenseInput
import com.example.tripplan.domain.model.UpdateExpenseInput
import com.example.tripplan.domain.model.Expense
import com.example.tripplan.testutil.FakeExpenseRepository
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExpenseUseCasesTest {
    @Test
    fun recordExpense_preservesPlacementTimeCategoryAndRateSnapshot() = runTest {
        val repository = FakeExpenseRepository()
        val clock = Clock.fixed(Instant.parse("2026-11-11T02:00:00Z"), ZoneOffset.UTC)
        val useCase = RecordExpenseUseCase(repository, clock)

        val result = useCase(
            RecordExpenseInput(
                tripPlanId = 7,
                itineraryItemId = 21,
                occurredDate = LocalDate.of(2026, 11, 11),
                occurredTime = LocalTime.of(11, 30),
                amount = BigDecimal("2500"),
                currencyCode = "JPY",
                krwPerUnit = BigDecimal("9.52"),
                rateDate = LocalDate.of(2026, 11, 10),
                category = ExpenseCategory.Food,
            )
        )

        assertTrue(result is RecordExpenseResult.Success)
        val expense = repository.values().single()
        assertEquals(21L, expense.itineraryItemId)
        assertEquals(ExpenseCategory.Food, expense.category)
        assertEquals(BigDecimal("23800"), expense.convertedKrwAmount)
        assertEquals(LocalTime.of(11, 30), expense.occurredTime)
    }

    @Test
    fun krwInput_keepsOriginalKrwAndTravelCurrencyRateForSecondaryDisplay() = runTest {
        val repository = FakeExpenseRepository()
        val clock = Clock.fixed(Instant.parse("2026-11-11T02:00:00Z"), ZoneOffset.UTC)
        val useCase = RecordExpenseUseCase(repository, clock)

        useCase(
            RecordExpenseInput(
                tripPlanId = 7,
                itineraryItemId = null,
                occurredDate = LocalDate.of(2026, 11, 11),
                occurredTime = LocalTime.of(14, 0),
                amount = BigDecimal("23800"),
                currencyCode = "KRW",
                krwPerUnit = BigDecimal("9.52"),
                rateDate = LocalDate.of(2026, 11, 10),
                category = ExpenseCategory.Shopping,
            )
        )

        val expense = repository.values().single()
        assertEquals(BigDecimal("23800"), expense.convertedKrwAmount)
        assertEquals(BigDecimal("9.52"), expense.krwPerUnit)
    }

    @Test
    fun updateExpense_preservesRateSnapshotAndRecalculatesKrwAmount() = runTest {
        val original = sampleExpense()
        val repository = FakeExpenseRepository().apply { setExpenses(listOf(original)) }

        val result = UpdateExpenseUseCase(repository)(
            UpdateExpenseInput(
                expenseId = original.id,
                tripPlanId = original.tripPlanId,
                itineraryItemId = null,
                occurredDate = original.occurredDate,
                occurredTime = LocalTime.of(15, 20),
                amount = BigDecimal("3000"),
                currencyCode = "JPY",
                category = ExpenseCategory.Transportation,
            )
        )

        assertEquals(ExpenseMutationResult.Success, result)
        val updated = repository.values().single()
        assertEquals(BigDecimal("9.52"), updated.krwPerUnit)
        assertEquals(LocalDate.of(2026, 11, 10), updated.rateDate)
        assertEquals(BigDecimal("28560"), updated.convertedKrwAmount)
        assertEquals(LocalTime.of(15, 20), updated.occurredTime)
        assertEquals(ExpenseCategory.Transportation, updated.category)
    }

    @Test
    fun deleteExpense_removesRecordedExpense() = runTest {
        val original = sampleExpense()
        val repository = FakeExpenseRepository().apply { setExpenses(listOf(original)) }

        val result = DeleteExpenseUseCase(repository)(original.id)

        assertEquals(ExpenseMutationResult.Success, result)
        assertTrue(repository.values().isEmpty())
    }

    private fun sampleExpense() = Expense(
        id = 5,
        tripPlanId = 7,
        itineraryItemId = 21,
        occurredDate = LocalDate.of(2026, 11, 11),
        occurredTime = LocalTime.of(11, 30),
        amount = BigDecimal("2500"),
        currencyCode = "JPY",
        krwPerUnit = BigDecimal("9.52"),
        rateDate = LocalDate.of(2026, 11, 10),
        convertedKrwAmount = BigDecimal("23800"),
        category = ExpenseCategory.Food,
        createdAt = Instant.parse("2026-11-11T02:00:00Z"),
    )
}
