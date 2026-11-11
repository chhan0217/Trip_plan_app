package com.example.tripplan.domain.model

import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

enum class BudgetEntryType {
    Initial,
    Additional,
}

data class BudgetEntry(
    val id: Long,
    val tripPlanId: Long,
    val type: BudgetEntryType,
    val amount: BigDecimal,
    val krwPerUnit: BigDecimal,
    val rateDate: LocalDate,
    val convertedKrwAmount: BigDecimal,
    val memo: String,
    val order: Int,
    val createdAt: Instant,
    val updatedAt: Instant,
)

data class TripBudget(
    val tripPlanId: Long,
    val currencyCode: String,
    val entries: List<BudgetEntry>,
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    val totalAmount: BigDecimal
        get() = entries.fold(BigDecimal.ZERO) { total, entry -> total + entry.amount }

    val totalConvertedKrwAmount: BigDecimal
        get() = entries.fold(BigDecimal.ZERO) { total, entry -> total + entry.convertedKrwAmount }
}

data class CreateTripBudgetInput(
    val currencyCode: String,
    val krwAmount: BigDecimal,
    val krwPerUnit: BigDecimal,
    val rateDate: LocalDate,
    val memo: String = "",
)

data class AddBudgetEntryInput(
    val krwAmount: BigDecimal,
    val krwPerUnit: BigDecimal,
    val rateDate: LocalDate,
    val memo: String = "",
)
