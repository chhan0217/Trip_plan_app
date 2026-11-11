package com.example.tripplan.domain.model

import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalTime

data class ItineraryCost(
    val amount: BigDecimal,
    val currencyCode: String,
    val krwPerUnit: BigDecimal,
    val rateDate: LocalDate,
    val convertedKrwAmount: BigDecimal,
)

data class ItineraryCostInput(
    val amount: BigDecimal,
    val currencyCode: String,
    val krwPerUnit: BigDecimal,
)

data class ItineraryItem(
    val id: Long,
    val tripPlanId: Long,
    val date: LocalDate,
    val title: String,
    val place: String,
    val startTime: LocalTime? = null,
    val endTime: LocalTime? = null,
    val note: String = "",
    val order: Int,
    val placeId: Long? = null,
    val estimatedCost: ItineraryCost? = null,
    val actualCost: ItineraryCost? = null,
)

data class ItineraryItemInput(
    val date: LocalDate,
    val title: String,
    val place: String,
    val startTime: LocalTime? = null,
    val endTime: LocalTime? = null,
    val note: String = "",
    val placeId: Long? = null,
)
