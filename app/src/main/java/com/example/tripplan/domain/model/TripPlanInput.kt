package com.example.tripplan.domain.model

import java.time.LocalDate

data class TripPlanInput(
    val title: String,
    val destination: String,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val description: String = "",
    val countryCode: String = "KR",
    val currencyCode: String = "KRW",
)
