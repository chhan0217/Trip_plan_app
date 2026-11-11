package com.example.tripplan.domain.model

import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

enum class ExpenseCategory {
    Food,
    Transportation,
    Accommodation,
    Sightseeing,
    Shopping,
    Other,
}

data class Expense(
    val id: Long,
    val tripPlanId: Long,
    val itineraryItemId: Long?,
    val occurredDate: LocalDate,
    val occurredTime: LocalTime,
    val amount: BigDecimal,
    val currencyCode: String,
    val krwPerUnit: BigDecimal,
    val rateDate: LocalDate,
    val convertedKrwAmount: BigDecimal,
    val category: ExpenseCategory,
    val createdAt: Instant,
)

data class RecordExpenseInput(
    val tripPlanId: Long,
    val itineraryItemId: Long?,
    val occurredDate: LocalDate,
    val occurredTime: LocalTime,
    val amount: BigDecimal,
    val currencyCode: String,
    val krwPerUnit: BigDecimal,
    val rateDate: LocalDate,
    val category: ExpenseCategory,
)

data class UpdateExpenseInput(
    val expenseId: Long,
    val tripPlanId: Long,
    val itineraryItemId: Long?,
    val occurredDate: LocalDate,
    val occurredTime: LocalTime,
    val amount: BigDecimal,
    val currencyCode: String,
    val category: ExpenseCategory,
)
