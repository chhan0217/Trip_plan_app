package com.example.tripplan.presentation.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data object HomeRoute : NavKey

@Serializable
data class CreateTripRoute(val sessionId: String = java.util.UUID.randomUUID().toString()) : NavKey

@Serializable
data class TripDetailRoute(val tripPlanId: Long) : NavKey

@Serializable
data class TripPlanningRoute(val tripPlanId: Long) : NavKey

@Serializable
data class TripPlanExportRoute(val tripPlanId: Long) : NavKey

@Serializable
data class TripPlanImportRoute(val uri: String) : NavKey

@Serializable
data class EditTripRoute(val tripPlanId: Long) : NavKey

@Serializable
data class PackingRoute(val tripPlanId: Long) : NavKey

@Serializable
data class TripCopyRoute(val sourceTripId: Long) : NavKey

@Serializable data object PlaceRoute : NavKey

@Serializable data object SettingsRoute : NavKey

@Serializable
data class ItineraryEditorRoute(
    val tripPlanId: Long,
    val itemId: Long? = null,
    val initialDate: String? = null,
    val sessionId: String = java.util.UUID.randomUUID().toString(),
) : NavKey

@Serializable
data class ItineraryMapRoute(val tripPlanId: Long, val isReadOnly: Boolean = false) : NavKey
