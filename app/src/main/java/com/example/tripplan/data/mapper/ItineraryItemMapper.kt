package com.example.tripplan.data.mapper

import com.example.tripplan.data.local.ItineraryItemEntity
import com.example.tripplan.domain.model.ItineraryItem
import com.example.tripplan.domain.model.ItineraryCost
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalTime

fun ItineraryItemEntity.toDomain(): ItineraryItem = ItineraryItem(
    id = id,
    tripPlanId = tripPlanId,
    date = LocalDate.parse(date),
    title = title,
    place = place,
    startTime = startTime?.let(LocalTime::parse),
    endTime = endTime?.let(LocalTime::parse),
    note = note,
    order = sortOrder,
    placeId = placeId,
    estimatedCost = toCost(
        estimatedAmount,
        estimatedCurrencyCode,
        estimatedKrwPerUnit,
        estimatedRateDate,
        estimatedConvertedKrwAmount,
    ),
    actualCost = toCost(
        actualAmount,
        actualCurrencyCode,
        actualKrwPerUnit,
        actualRateDate,
        actualConvertedKrwAmount,
    ),
)

fun ItineraryItem.toEntity(): ItineraryItemEntity = ItineraryItemEntity(
    id = id,
    tripPlanId = tripPlanId,
    date = date.toString(),
    title = title,
    place = place,
    startTime = startTime?.toString(),
    endTime = endTime?.toString(),
    note = note,
    sortOrder = order,
    placeId = placeId,
    estimatedAmount = estimatedCost?.amount?.toPlainString(),
    estimatedCurrencyCode = estimatedCost?.currencyCode,
    estimatedKrwPerUnit = estimatedCost?.krwPerUnit?.toPlainString(),
    estimatedRateDate = estimatedCost?.rateDate?.toString(),
    estimatedConvertedKrwAmount = estimatedCost?.convertedKrwAmount?.toPlainString(),
    actualAmount = actualCost?.amount?.toPlainString(),
    actualCurrencyCode = actualCost?.currencyCode,
    actualKrwPerUnit = actualCost?.krwPerUnit?.toPlainString(),
    actualRateDate = actualCost?.rateDate?.toString(),
    actualConvertedKrwAmount = actualCost?.convertedKrwAmount?.toPlainString(),
)

private fun toCost(
    amount: String?,
    currencyCode: String?,
    krwPerUnit: String?,
    rateDate: String?,
    convertedKrwAmount: String?,
): ItineraryCost? {
    if (
        amount == null || currencyCode == null || krwPerUnit == null ||
        rateDate == null || convertedKrwAmount == null
    ) return null
    return ItineraryCost(
        amount = BigDecimal(amount),
        currencyCode = currencyCode,
        krwPerUnit = BigDecimal(krwPerUnit),
        rateDate = LocalDate.parse(rateDate),
        convertedKrwAmount = BigDecimal(convertedKrwAmount),
    )
}
