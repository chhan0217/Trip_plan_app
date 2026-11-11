package com.example.tripplan.domain.rule

import com.example.tripplan.domain.model.Expense
import com.example.tripplan.domain.model.ExpenseCategory
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Test

class ExpenseSummaryCalculatorTest {
    @Test
    fun calculate_groupsByCategoryAndSortsByKrwAmount() {
        val result = ExpenseSummaryCalculator.calculate(
            expenses = listOf(
                expense(ExpenseCategory.Food, "1000", "JPY", "9520"),
                expense(ExpenseCategory.Transportation, "500", "JPY", "4760"),
                expense(ExpenseCategory.Food, "300", "JPY", "2856"),
            ),
            tripCurrencyCode = "JPY",
            fallbackKrwPerUnit = BigDecimal("9.52"),
        )

        assertEquals(listOf(ExpenseCategory.Food, ExpenseCategory.Transportation), result.map { it.category })
        assertEquals(BigDecimal("12376"), result[0].krwAmount)
        assertEquals(BigDecimal("1300"), result[0].localAmount)
    }

    @Test
    fun calculate_convertsKrwExpenseUsingItsRecordedRate() {
        val result = ExpenseSummaryCalculator.calculate(
            expenses = listOf(expense(ExpenseCategory.Shopping, "23800", "KRW", "23800")),
            tripCurrencyCode = "JPY",
            fallbackKrwPerUnit = BigDecimal("10.00"),
        )

        assertEquals(BigDecimal("2500.00"), result.single().localAmount)
    }

    private fun expense(
        category: ExpenseCategory,
        amount: String,
        currencyCode: String,
        convertedKrwAmount: String,
    ) = Expense(
        id = 1,
        tripPlanId = 1,
        itineraryItemId = null,
        occurredDate = LocalDate.of(2026, 11, 9),
        occurredTime = LocalTime.NOON,
        amount = BigDecimal(amount),
        currencyCode = currencyCode,
        krwPerUnit = BigDecimal("9.52"),
        rateDate = LocalDate.of(2026, 11, 8),
        convertedKrwAmount = BigDecimal(convertedKrwAmount),
        category = category,
        createdAt = Instant.EPOCH,
    )
}
