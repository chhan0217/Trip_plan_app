package com.example.tripplan.domain.model

import java.time.Instant
import java.time.LocalDate
import java.math.BigDecimal

data class ExchangeRate(
    val currencyCode: String,
    val currencyName: String,
    val krwPerUnit: BigDecimal,
)

data class ExchangeRateSnapshot(
    val rateDate: LocalDate,
    val fetchedAt: Instant,
    val rates: List<ExchangeRate>,
)

sealed interface ExchangeRateRefreshResult {
    data class Updated(val rateDate: LocalDate) : ExchangeRateRefreshResult
    data object KeptCached : ExchangeRateRefreshResult
    data object Failed : ExchangeRateRefreshResult
}

data class HomeExchangeRateData(
    val snapshot: ExchangeRateSnapshot?,
    val selectedCurrencyUnits: Set<String>,
)
