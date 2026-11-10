package com.example.tripplan.di

import com.example.tripplan.data.network.NetworkClientFactory
import com.example.tripplan.data.network.exchange.ExchangeRateRemoteDataSource
import com.example.tripplan.data.network.exchange.FrankfurterExchangeRateApi
import com.example.tripplan.data.network.exchange.FrankfurterExchangeRateRemoteDataSource
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.HttpUrl.Companion.toHttpUrl

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides
    @Singleton
    fun provideNetworkJson(): Json {
        return Json {
            ignoreUnknownKeys = true
            explicitNulls = false
            isLenient = false
        }
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .callTimeout(CALL_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    @Provides
    @Singleton
    fun provideFrankfurterExchangeRateApi(
        networkClientFactory: NetworkClientFactory,
    ): FrankfurterExchangeRateApi {
        return networkClientFactory
            .create(FRANKFURTER_BASE_URL.toHttpUrl())
            .create(FrankfurterExchangeRateApi::class.java)
    }

    @Provides
    @Singleton
    fun provideExchangeRateRemoteDataSource(
        remoteDataSource: FrankfurterExchangeRateRemoteDataSource,
    ): ExchangeRateRemoteDataSource = remoteDataSource

    private const val CONNECT_TIMEOUT_SECONDS = 10L
    private const val READ_TIMEOUT_SECONDS = 15L
    private const val WRITE_TIMEOUT_SECONDS = 15L
    private const val CALL_TIMEOUT_SECONDS = 20L
    private const val FRANKFURTER_BASE_URL = "https://api.frankfurter.dev/"
}
