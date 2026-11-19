package com.example.tripplan.data.repository

import android.content.Context
import com.example.tripplan.BuildConfig
import com.example.tripplan.domain.model.ExternalPlaceDetails
import com.example.tripplan.domain.model.ExternalPlaceSuggestion
import com.example.tripplan.domain.repository.DeviceCapabilityRepository
import com.example.tripplan.domain.repository.ExternalPlaceSearchRepository
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.PlacesClient
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

@Singleton
class GoogleExternalPlaceSearchRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val deviceCapabilityRepository: DeviceCapabilityRepository,
) : ExternalPlaceSearchRepository {
    private val isConfigured = BuildConfig.PLACES_API_KEY.isNotBlank() &&
        BuildConfig.PLACES_API_KEY != DEFAULT_API_KEY

    private val client: PlacesClient? by lazy {
        if (!isConfigured) return@lazy null
        if (!Places.isInitialized()) {
            Places.initializeWithNewPlacesApiEnabled(context, BuildConfig.PLACES_API_KEY, Locale.KOREAN)
        }
        Places.createClient(context)
    }

    override suspend fun search(
        countryCode: String,
        query: String,
    ): List<ExternalPlaceSuggestion> {
        if (!deviceCapabilityRepository.isNetworkAvailable()) return emptyList()
        val placesClient = client ?: return emptyList()
        val request = FindAutocompletePredictionsRequest.builder()
            .setQuery(query)
            .setCountries(listOf(countryCode.lowercase()))
            .build()
        return suspendCancellableCoroutine { continuation ->
            placesClient.findAutocompletePredictions(request)
                .addOnSuccessListener { response ->
                    if (continuation.isActive) {
                        continuation.resume(
                            response.autocompletePredictions.map { prediction ->
                                ExternalPlaceSuggestion(
                                    externalId = prediction.placeId,
                                    name = prediction.getPrimaryText(null).toString(),
                                    addressHint = prediction.getSecondaryText(null).toString(),
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

    override suspend fun getDetails(externalId: String): ExternalPlaceDetails? {
        if (!deviceCapabilityRepository.isNetworkAvailable()) return null
        val placesClient = client ?: return null
        val fields = listOf(
            Place.Field.ID,
            Place.Field.DISPLAY_NAME,
            Place.Field.FORMATTED_ADDRESS,
            Place.Field.LOCATION,
        )
        val request = FetchPlaceRequest.builder(externalId, fields).build()
        return suspendCancellableCoroutine { continuation ->
            placesClient.fetchPlace(request)
                .addOnSuccessListener { response ->
                    val place = response.place
                    val location = place.location
                    if (continuation.isActive) {
                        continuation.resume(
                            ExternalPlaceDetails(
                                externalId = place.id ?: externalId,
                                name = place.displayName.orEmpty(),
                                address = place.formattedAddress.orEmpty(),
                                latitude = location?.latitude,
                                longitude = location?.longitude,
                            )
                        )
                    }
                }
                .addOnFailureListener {
                    if (continuation.isActive) continuation.resume(null)
                }
        }
    }

    private companion object {
        const val DEFAULT_API_KEY = "DEFAULT_API_KEY"
    }
}
