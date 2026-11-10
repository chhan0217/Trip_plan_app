package com.example.tripplan.data.network

import java.io.IOException
import java.net.SocketTimeoutException
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import retrofit2.HttpException

sealed interface NetworkResult<out T> {
    data class Success<T>(val value: T) : NetworkResult<T>

    data class Failure(
        val reason: NetworkFailureReason,
        val httpCode: Int? = null,
        val providerCode: Int? = null,
    ) : NetworkResult<Nothing>
}

enum class NetworkFailureReason {
    Http,
    NoConnection,
    Timeout,
    InvalidResponse,
    InvalidRequest,
    Authentication,
    RateLimit,
    MissingConfiguration,
    Unknown,
}

suspend inline fun <T> executeNetworkRequest(
    crossinline request: suspend () -> T,
): NetworkResult<T> {
    return try {
        NetworkResult.Success(request())
    } catch (error: CancellationException) {
        throw error
    } catch (error: HttpException) {
        NetworkResult.Failure(
            reason = NetworkFailureReason.Http,
            httpCode = error.code(),
        )
    } catch (_: SocketTimeoutException) {
        NetworkResult.Failure(NetworkFailureReason.Timeout)
    } catch (_: SerializationException) {
        NetworkResult.Failure(NetworkFailureReason.InvalidResponse)
    } catch (_: IOException) {
        NetworkResult.Failure(NetworkFailureReason.NoConnection)
    } catch (_: Exception) {
        NetworkResult.Failure(NetworkFailureReason.Unknown)
    }
}
