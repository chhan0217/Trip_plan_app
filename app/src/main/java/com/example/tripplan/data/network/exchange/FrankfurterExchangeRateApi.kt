package com.example.tripplan.data.network.exchange

import retrofit2.http.GET
import retrofit2.http.Query

interface FrankfurterExchangeRateApi {
    @GET("v2/rates")
    suspend fun getRates(
        @Query("base") base: String = BASE_CURRENCY,
        @Query("date") date: String,
    ): List<FrankfurterRateDto>

    private companion object {
        const val BASE_CURRENCY = "KRW"
    }
}
