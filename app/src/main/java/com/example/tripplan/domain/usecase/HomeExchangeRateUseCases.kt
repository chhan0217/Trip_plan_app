package com.example.tripplan.domain.usecase

import com.example.tripplan.domain.model.ExchangeRateRefreshResult
import com.example.tripplan.domain.model.HomeExchangeRateData
import com.example.tripplan.domain.repository.ExchangeRatePreferenceRepository
import com.example.tripplan.domain.repository.ExchangeRateRepository
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class ObserveLatestExchangeRatesUseCase @Inject constructor(
    private val exchangeRateRepository: ExchangeRateRepository,
) {
    operator fun invoke() = exchangeRateRepository.observeLatestRates()
}

class ObserveHomeExchangeRatesUseCase @Inject constructor(
    private val exchangeRateRepository: ExchangeRateRepository,
    private val preferenceRepository: ExchangeRatePreferenceRepository,
) {
    operator fun invoke(): Flow<HomeExchangeRateData> {
        return combine(
            exchangeRateRepository.observeLatestRates(),
            preferenceRepository.observeSelectedCurrencyUnits(),
        ) { snapshot, selectedCurrencyUnits ->
            HomeExchangeRateData(snapshot, selectedCurrencyUnits)
        }
    }
}

class RefreshHomeExchangeRatesUseCase @Inject constructor(
    private val exchangeRateRepository: ExchangeRateRepository,
    private val clock: Clock,
) {
    suspend operator fun invoke(): ExchangeRateRefreshResult {
        return exchangeRateRepository.refreshLatestRates(LocalDate.now(clock))
    }
}

class UpdateHomeExchangeRateSelectionUseCase @Inject constructor(
    private val preferenceRepository: ExchangeRatePreferenceRepository,
) {
    suspend operator fun invoke(currencyUnits: Set<String>) {
        preferenceRepository.updateSelectedCurrencyUnits(currencyUnits)
    }
}
