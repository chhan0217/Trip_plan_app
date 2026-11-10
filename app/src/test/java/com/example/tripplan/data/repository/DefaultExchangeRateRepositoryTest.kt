package com.example.tripplan.data.repository

import com.example.tripplan.data.local.ExchangeRateDao
import com.example.tripplan.data.local.ExchangeRateEntity
import com.example.tripplan.data.network.NetworkFailureReason
import com.example.tripplan.data.network.NetworkResult
import com.example.tripplan.data.network.exchange.ExchangeRateRemoteDataSource
import com.example.tripplan.data.network.exchange.ExchangeRateRemoteSnapshot
import com.example.tripplan.data.network.exchange.FrankfurterRateDto
import com.example.tripplan.domain.model.ExchangeRateRefreshResult
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DefaultExchangeRateRepositoryTest {
    private val clock = Clock.fixed(Instant.parse("2026-11-10T03:00:00Z"), ZoneOffset.UTC)

    @Test
    fun refreshLatestRates_fallsBackAndStoresKrwPerForeignCurrencyUnit() = runTest {
        val dao = FakeExchangeRateDao()
        val remote = FakeExchangeRateRemoteDataSource(
            results = mutableMapOf(
                LocalDate.of(2026, 11, 10) to success(
                    requestedDate = LocalDate.of(2026, 11, 10),
                    rates = emptyList(),
                ),
                LocalDate.of(2026, 11, 9) to success(
                    requestedDate = LocalDate.of(2026, 11, 9),
                    rates = listOf(vndRate()),
                ),
            )
        )
        val repository = DefaultExchangeRateRepository(dao, remote, clock)

        val result = repository.refreshLatestRates(LocalDate.of(2026, 11, 10))

        assertEquals(
            ExchangeRateRefreshResult.Updated(LocalDate.of(2026, 11, 9)),
            result,
        )
        assertEquals(
            listOf(LocalDate.of(2026, 11, 10), LocalDate.of(2026, 11, 9)),
            remote.requestedDates,
        )
        assertEquals("VND", dao.rates.value.single().currencyCode)
        assertEquals("0.05271759186", dao.rates.value.single().krwPerUnit)
        assertEquals("2026-11-09", dao.rates.value.single().rateDate)
        assertEquals(clock.instant().toEpochMilli(), dao.rates.value.single().fetchedAt)
    }

    @Test
    fun refreshLatestRates_keepsCacheWhenNetworkFails() = runTest {
        val cached = cachedRate()
        val dao = FakeExchangeRateDao(listOf(cached))
        val remote = FakeExchangeRateRemoteDataSource(
            defaultResult = NetworkResult.Failure(NetworkFailureReason.NoConnection)
        )
        val repository = DefaultExchangeRateRepository(dao, remote, clock)

        val result = repository.refreshLatestRates(LocalDate.of(2026, 11, 10))

        assertEquals(ExchangeRateRefreshResult.KeptCached, result)
        assertEquals(listOf(cached), dao.rates.value)
    }

    @Test
    fun refreshLatestRates_reportsFailureWithoutCache() = runTest {
        val dao = FakeExchangeRateDao()
        val remote = FakeExchangeRateRemoteDataSource(
            defaultResult = NetworkResult.Failure(NetworkFailureReason.NoConnection)
        )
        val repository = DefaultExchangeRateRepository(dao, remote, clock)

        val result = repository.refreshLatestRates(LocalDate.of(2026, 11, 10))

        assertEquals(ExchangeRateRefreshResult.Failed, result)
        assertTrue(dao.rates.value.isEmpty())
    }

    private fun success(
        requestedDate: LocalDate,
        rates: List<FrankfurterRateDto>,
    ): NetworkResult<ExchangeRateRemoteSnapshot> {
        return NetworkResult.Success(
            ExchangeRateRemoteSnapshot(rateDate = requestedDate, rates = rates)
        )
    }

    private fun vndRate() = FrankfurterRateDto(
        date = "2026-11-09",
        base = "KRW",
        quote = "VND",
        rate = 18.969,
    )

    private fun cachedRate() = ExchangeRateEntity(
        currencyCode = "USD",
        currencyName = "미국 달러",
        krwPerUnit = "1400",
        rateDate = "2026-11-07",
        fetchedAt = 1L,
    )

    private class FakeExchangeRateDao(initial: List<ExchangeRateEntity> = emptyList()) :
        ExchangeRateDao {
        val rates = MutableStateFlow(initial)

        override fun observeRates(): Flow<List<ExchangeRateEntity>> = rates

        override suspend fun getRates(): List<ExchangeRateEntity> = rates.value

        override suspend fun insertAll(rates: List<ExchangeRateEntity>) {
            this.rates.value = rates
        }

        override suspend fun clear() {
            rates.value = emptyList()
        }
    }

    private class FakeExchangeRateRemoteDataSource(
        private val results:
            MutableMap<LocalDate, NetworkResult<ExchangeRateRemoteSnapshot>> = mutableMapOf(),
        private val defaultResult: NetworkResult<ExchangeRateRemoteSnapshot> =
            NetworkResult.Success(
                ExchangeRateRemoteSnapshot(LocalDate.MIN, emptyList())
            ),
    ) : ExchangeRateRemoteDataSource {
        val requestedDates = mutableListOf<LocalDate>()

        override suspend fun getExchangeRates(
            searchDate: LocalDate,
        ): NetworkResult<ExchangeRateRemoteSnapshot> {
            requestedDates += searchDate
            return results[searchDate] ?: defaultResult
        }
    }
}
