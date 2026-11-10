package com.example.tripplan.data.repository

import com.example.tripplan.data.local.ExchangeRateDao
import com.example.tripplan.data.mapper.toDomainOrNull
import com.example.tripplan.data.mapper.toEntityOrNull
import com.example.tripplan.data.network.NetworkResult
import com.example.tripplan.data.network.exchange.ExchangeRateRemoteDataSource
import com.example.tripplan.domain.model.ExchangeRateRefreshResult
import com.example.tripplan.domain.model.ExchangeRateSnapshot
import com.example.tripplan.domain.repository.ExchangeRateRepository
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class DefaultExchangeRateRepository @Inject constructor(
    private val exchangeRateDao: ExchangeRateDao,
    private val remoteDataSource: ExchangeRateRemoteDataSource,
    private val clock: Clock,
) : ExchangeRateRepository {
    override fun observeLatestRates(): Flow<ExchangeRateSnapshot?> {
        return exchangeRateDao.observeRates().map { entities ->
            val first = entities.firstOrNull() ?: return@map null
            ExchangeRateSnapshot(
                rateDate = LocalDate.parse(first.rateDate),
                fetchedAt = Instant.ofEpochMilli(first.fetchedAt),
                rates = entities.mapNotNull { it.toDomainOrNull() },
            )
        }
    }

    override suspend fun refreshLatestRates(today: LocalDate): ExchangeRateRefreshResult {
        val hasCache = exchangeRateDao.getRates().isNotEmpty()
        for (daysAgo in 0..MAX_LOOKBACK_DAYS) {
            val rateDate = today.minusDays(daysAgo.toLong())
            when (val result = remoteDataSource.getExchangeRates(rateDate)) {
                is NetworkResult.Failure -> {
                    return if (hasCache) {
                        ExchangeRateRefreshResult.KeptCached
                    } else {
                        ExchangeRateRefreshResult.Failed
                    }
                }
                is NetworkResult.Success -> {
                    val fetchedAt = clock.instant()
                    val entities = result.value.rates.mapNotNull { rate ->
                        rate.toEntityOrNull(
                            rateDate = result.value.rateDate,
                            fetchedAt = fetchedAt,
                        )
                    }
                    if (entities.isNotEmpty()) {
                        exchangeRateDao.replaceAll(entities)
                        return ExchangeRateRefreshResult.Updated(result.value.rateDate)
                    }
                }
            }
        }
        return if (hasCache) ExchangeRateRefreshResult.KeptCached else ExchangeRateRefreshResult.Failed
    }

    private companion object {
        const val MAX_LOOKBACK_DAYS = 7
    }
}
