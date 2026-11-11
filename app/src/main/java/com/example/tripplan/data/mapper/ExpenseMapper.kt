package com.example.tripplan.data.mapper

import com.example.tripplan.data.local.ExpenseEntity
import com.example.tripplan.domain.model.Expense
import com.example.tripplan.domain.model.ExpenseCategory
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

fun ExpenseEntity.toDomain() = Expense(
    id = id,
    tripPlanId = tripPlanId,
    itineraryItemId = itineraryItemId,
    occurredDate = LocalDate.parse(occurredDate),
    occurredTime = LocalTime.of(occurredTimeMinutes / 60, occurredTimeMinutes % 60),
    amount = amount.toBigDecimal(),
    currencyCode = currencyCode,
    krwPerUnit = krwPerUnit.toBigDecimal(),
    rateDate = LocalDate.parse(rateDate),
    convertedKrwAmount = convertedKrwAmount.toBigDecimal(),
    category = ExpenseCategory.valueOf(category),
    createdAt = Instant.ofEpochMilli(createdAt),
)

fun Expense.toEntity() = ExpenseEntity(
    id = id,
    tripPlanId = tripPlanId,
    itineraryItemId = itineraryItemId,
    occurredDate = occurredDate.toString(),
    occurredTimeMinutes = occurredTime.hour * 60 + occurredTime.minute,
    amount = amount.toPlainString(),
    currencyCode = currencyCode,
    krwPerUnit = krwPerUnit.toPlainString(),
    rateDate = rateDate.toString(),
    convertedKrwAmount = convertedKrwAmount.toPlainString(),
    category = category.name,
    createdAt = createdAt.toEpochMilli(),
)
