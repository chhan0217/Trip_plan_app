package com.example.tripplan.data.network.exchange

import com.example.tripplan.data.network.NetworkResult
import com.example.tripplan.data.network.executeNetworkRequest
import java.time.LocalDate
import javax.inject.Inject

data class ExchangeRateRemoteSnapshot(
    val rateDate: LocalDate,
    val rates: List<FrankfurterRateDto>,
)

interface ExchangeRateRemoteDataSource {
    suspend fun getExchangeRates(
        searchDate: LocalDate,
    ): NetworkResult<ExchangeRateRemoteSnapshot>
}

class FrankfurterExchangeRateRemoteDataSource @Inject constructor(
    private val api: FrankfurterExchangeRateApi,
) : ExchangeRateRemoteDataSource {
    override suspend fun getExchangeRates(
        searchDate: LocalDate,
    ): NetworkResult<ExchangeRateRemoteSnapshot> {
        return when (
            val response = executeNetworkRequest {
                api.getRates(date = searchDate.toString())
            }
        ) {
            is NetworkResult.Failure -> response
            is NetworkResult.Success -> {
                val rates = response.value.filter { rate ->
                    rate.base.equals(BASE_CURRENCY, ignoreCase = true) &&
                        !rate.quote.equals(BASE_CURRENCY, ignoreCase = true) &&
                        rate.rate > 0.0
                }
                val rateDate = rates.firstNotNullOfOrNull { rate ->
                    runCatching { LocalDate.parse(rate.date) }.getOrNull()
                } ?: searchDate
                NetworkResult.Success(
                    ExchangeRateRemoteSnapshot(
                        rateDate = rateDate,
                        rates = rates,
                    )
                )
            }
        }
    }

    private companion object {
        const val BASE_CURRENCY = "KRW"
    }
}
