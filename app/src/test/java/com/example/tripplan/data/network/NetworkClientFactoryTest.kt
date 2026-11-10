package com.example.tripplan.data.network

import java.net.SocketTimeoutException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.http.GET

class NetworkClientFactoryTest {
    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        server = MockWebServer().apply { start() }
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun retrofit_decodesJsonAndIgnoresUnknownFields() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("""{"base":"KRW","rates":{"USD":0.0007},"unknown":true}"""),
        )
        val api = factory().create(server.url("/")).create(TestExchangeRateApi::class.java)

        val result = executeNetworkRequest { api.latest() }

        assertEquals(
            NetworkResult.Success(TestRatesResponse(base = "KRW", rates = mapOf("USD" to 0.0007))),
            result,
        )
        assertEquals("/latest", server.takeRequest().path)
    }

    @Test
    fun executeNetworkRequest_mapsHttpError() = runTest {
        server.enqueue(MockResponse().setResponseCode(429))
        val api = factory().create(server.url("/")).create(TestExchangeRateApi::class.java)

        val result = executeNetworkRequest { api.latest() }

        assertEquals(
            NetworkResult.Failure(NetworkFailureReason.Http, httpCode = 429),
            result,
        )
    }

    @Test
    fun executeNetworkRequest_mapsInvalidJson() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("not-json"),
        )
        val api = factory().create(server.url("/")).create(TestExchangeRateApi::class.java)

        val result = executeNetworkRequest { api.latest() }

        assertEquals(NetworkResult.Failure(NetworkFailureReason.InvalidResponse), result)
    }

    @Test
    fun executeNetworkRequest_preservesCancellation() = runTest {
        val cancellation = CancellationException("cancelled")

        val thrown = runCatching {
            executeNetworkRequest<TestRatesResponse> { throw cancellation }
        }.exceptionOrNull()

        assertTrue(thrown === cancellation)
    }

    @Test
    fun executeNetworkRequest_mapsTimeout() = runTest {
        val result = executeNetworkRequest<TestRatesResponse> {
            throw SocketTimeoutException("timeout")
        }

        assertEquals(NetworkResult.Failure(NetworkFailureReason.Timeout), result)
    }

    private fun factory(): NetworkClientFactory {
        return NetworkClientFactory(
            okHttpClient = OkHttpClient(),
            json = Json { ignoreUnknownKeys = true },
        )
    }
}

private interface TestExchangeRateApi {
    @GET("latest")
    suspend fun latest(): TestRatesResponse
}

@Serializable
private data class TestRatesResponse(
    val base: String,
    val rates: Map<String, Double>,
)
