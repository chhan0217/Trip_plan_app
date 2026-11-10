package com.example.tripplan.domain.repository

import com.example.tripplan.domain.model.ExchangeRateRefreshResult
import com.example.tripplan.domain.model.ExchangeRateSnapshot
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

interface ExchangeRateRepository {
    fun observeLatestRates(): Flow<ExchangeRateSnapshot?>
    suspend fun refreshLatestRates(today: LocalDate): ExchangeRateRefreshResult
}

interface ExchangeRatePreferenceRepository {
    fun observeSelectedCurrencyUnits(): Flow<Set<String>>
    suspend fun updateSelectedCurrencyUnits(currencyUnits: Set<String>)
}
