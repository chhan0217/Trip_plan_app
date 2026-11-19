package com.example.tripplan.data.repository

import android.content.Context
import com.example.tripplan.BuildConfig
import com.example.tripplan.domain.model.TravelCity
import com.example.tripplan.domain.repository.DeviceCapabilityRepository
import com.example.tripplan.domain.repository.TravelCityRepository
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.PlacesClient
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

@Singleton
class GoogleTravelCityRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val deviceCapabilityRepository: DeviceCapabilityRepository,
) : TravelCityRepository {
    private val isConfigured = BuildConfig.PLACES_API_KEY.isNotBlank() &&
        BuildConfig.PLACES_API_KEY != DEFAULT_API_KEY

    private val placesClient: PlacesClient? by lazy {
        if (!isConfigured) return@lazy null
        if (!Places.isInitialized()) {
            Places.initializeWithNewPlacesApiEnabled(
                context,
                BuildConfig.PLACES_API_KEY,
                java.util.Locale.KOREAN,
            )
        }
        Places.createClient(context)
    }

    override suspend fun search(countryCode: String, query: String): List<TravelCity> {
        if (!deviceCapabilityRepository.isNetworkAvailable()) return emptyList()
        val client = placesClient ?: return emptyList()
        val request = FindAutocompletePredictionsRequest.builder()
            .setQuery(query)
            .setCountries(listOf(countryCode.lowercase()))
            .setTypesFilter(listOf("(cities)"))
            .build()

        return suspendCancellableCoroutine { continuation ->
            client.findAutocompletePredictions(request)
                .addOnSuccessListener { response ->
                    if (continuation.isActive) {
                        continuation.resume(
                            response.autocompletePredictions.map { prediction ->
                                TravelCity(
                                    id = "google:${prediction.placeId}",
                                    name = prediction.getPrimaryText(null).toString(),
                                    detail = prediction.getSecondaryText(null).toString(),
                                )
                            }
                        )
                    }
                }
                .addOnFailureListener {
                    if (continuation.isActive) continuation.resume(emptyList())
                }
        }
    }

    private companion object {
        const val DEFAULT_API_KEY = "DEFAULT_API_KEY"
    }
}
