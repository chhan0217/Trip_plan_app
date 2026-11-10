package com.example.tripplan.data.network.exchange

import com.example.tripplan.data.network.NetworkClientFactory
import com.example.tripplan.data.network.NetworkResult
import java.time.LocalDate
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class FrankfurterExchangeRateRemoteDataSourceTest {
    private lateinit var server: MockWebServer
    private lateinit var api: FrankfurterExchangeRateApi

    @Before
    fun setUp() {
        server = MockWebServer().apply { start() }
        val retrofit = NetworkClientFactory(
            okHttpClient = OkHttpClient(),
            json = Json { ignoreUnknownKeys = true },
        ).create(server.url("/"))
        api = retrofit.create(FrankfurterExchangeRateApi::class.java)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun getExchangeRates_sendsKrwBaseAndDecodesVndRate() = runTest {
        server.enqueue(
            jsonResponse(
                """
                [
                  {"date":"2026-11-10","base":"KRW","quote":"KRW","rate":1.0},
                  {"date":"2026-11-10","base":"KRW","quote":"VND","rate":18.969},
                  {"date":"2026-11-10","base":"KRW","quote":"USD","rate":0.00073933}
                ]
                """.trimIndent(),
            ),
        )

        val result = FrankfurterExchangeRateRemoteDataSource(api).getExchangeRates(
            searchDate = LocalDate.of(2026, 11, 10),
        )

        val snapshot = (result as NetworkResult.Success).value
        assertEquals(LocalDate.of(2026, 11, 10), snapshot.rateDate)
        assertEquals(listOf("VND", "USD"), snapshot.rates.map { it.quote })
        assertEquals(18.969, snapshot.rates.first().rate, 0.0)
        val request = server.takeRequest()
        assertEquals("/v2/rates", request.requestUrl?.encodedPath)
        assertEquals("KRW", request.requestUrl?.queryParameter("base"))
        assertEquals("2026-11-10", request.requestUrl?.queryParameter("date"))
    }

    @Test
    fun getExchangeRates_keepsEmptyResponseForRepositoryFallback() = runTest {
        server.enqueue(jsonResponse("[]"))

        val result = FrankfurterExchangeRateRemoteDataSource(api).getExchangeRates(
            searchDate = LocalDate.of(2026, 11, 8),
        )

        assertEquals(
            NetworkResult.Success(
                ExchangeRateRemoteSnapshot(
                    rateDate = LocalDate.of(2026, 11, 8),
                    rates = emptyList(),
                )
            ),
            result,
        )
    }

    private fun jsonResponse(body: String): MockResponse {
        return MockResponse()
            .setResponseCode(200)
            .setHeader("Content-Type", "application/json")
            .setBody(body)
    }
}
