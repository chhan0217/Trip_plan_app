package com.example.tripplan.data.mapper

import com.example.tripplan.data.local.BudgetEntryEntity
import com.example.tripplan.data.local.TripBudgetWithEntries
import com.example.tripplan.domain.model.BudgetEntry
import com.example.tripplan.domain.model.BudgetEntryType
import com.example.tripplan.domain.model.TripBudget
import java.time.Instant
import java.time.LocalDate

fun TripBudgetWithEntries.toDomain(): TripBudget = TripBudget(
    tripPlanId = budget.tripPlanId,
    currencyCode = budget.currencyCode,
    entries = entries.sortedWith(compareBy<BudgetEntryEntity> { it.sortOrder }.thenBy { it.id })
        .map { it.toDomain() },
    createdAt = Instant.ofEpochMilli(budget.createdAt),
    updatedAt = Instant.ofEpochMilli(budget.updatedAt),
)

fun BudgetEntryEntity.toDomain(): BudgetEntry = BudgetEntry(
    id = id,
    tripPlanId = tripPlanId,
    type = BudgetEntryType.valueOf(type),
    amount = amount.toBigDecimal(),
    krwPerUnit = krwPerUnit.toBigDecimal(),
    rateDate = LocalDate.parse(rateDate),
    convertedKrwAmount = convertedKrwAmount.toBigDecimal(),
    memo = memo,
    order = sortOrder,
    createdAt = Instant.ofEpochMilli(createdAt),
    updatedAt = Instant.ofEpochMilli(updatedAt),
)

fun BudgetEntry.toEntity(): BudgetEntryEntity = BudgetEntryEntity(
    id = id,
    tripPlanId = tripPlanId,
    type = type.name,
    amount = amount.toPlainString(),
    krwPerUnit = krwPerUnit.toPlainString(),
    rateDate = rateDate.toString(),
    convertedKrwAmount = convertedKrwAmount.toPlainString(),
    memo = memo,
    sortOrder = order,
    createdAt = createdAt.toEpochMilli(),
    updatedAt = updatedAt.toEpochMilli(),
)
