package com.example.tripplan.data.network.exchange

import kotlinx.serialization.Serializable

@Serializable
data class FrankfurterRateDto(
    val date: String,
    val base: String,
    val quote: String,
    val rate: Double,
)
