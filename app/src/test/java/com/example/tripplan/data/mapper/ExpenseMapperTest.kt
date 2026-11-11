package com.example.tripplan.data.mapper

import com.example.tripplan.domain.model.Expense
import com.example.tripplan.domain.model.ExpenseCategory
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Test

class ExpenseMapperTest {
    @Test
    fun expense_roundTripsAllTimelineAndMoneyFields() {
        val expense = Expense(
            id = 4,
            tripPlanId = 7,
            itineraryItemId = 9,
            occurredDate = LocalDate.of(2026, 11, 11),
            occurredTime = LocalTime.of(12, 35),
            amount = BigDecimal("2500"),
            currencyCode = "JPY",
            krwPerUnit = BigDecimal("9.52"),
            rateDate = LocalDate.of(2026, 11, 10),
            convertedKrwAmount = BigDecimal("23800"),
            category = ExpenseCategory.Food,
            createdAt = Instant.parse("2026-11-11T03:35:00Z"),
        )

        assertEquals(expense, expense.toEntity().toDomain())
        assertEquals(755, expense.toEntity().occurredTimeMinutes)
    }
}
