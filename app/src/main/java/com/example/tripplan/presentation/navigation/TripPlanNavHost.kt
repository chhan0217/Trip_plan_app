package com.example.tripplan.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.example.tripplan.presentation.create.CreateTripScreen
import com.example.tripplan.presentation.detail.TripDetailScreen
import com.example.tripplan.presentation.edit.EditTripScreen
import com.example.tripplan.presentation.home.HomeScreen
import com.example.tripplan.presentation.itinerary.ItineraryEditorScreen
import com.example.tripplan.presentation.itinerarymap.ItineraryMapScreen
import com.example.tripplan.presentation.planning.TripPlanningScreen
import com.example.tripplan.presentation.file.TripPlanExportScreen
import com.example.tripplan.presentation.file.TripPlanImportScreen
import com.example.tripplan.presentation.packing.PackingScreen
import com.example.tripplan.presentation.copy.TripCopyScreen
import com.example.tripplan.presentation.place.PlaceScreen
import com.example.tripplan.presentation.settings.SettingsScreen
import java.time.LocalDate

@Composable
fun TripPlanNavHost(initialImportUri: String? = null) {
    val backStack = rememberNavBackStack(HomeRoute)

    LaunchedEffect(initialImportUri) {
        if (initialImportUri != null && backStack.lastOrNull() != TripPlanImportRoute(initialImportUri)) {
            backStack.add(TripPlanImportRoute(initialImportUri))
        }
    }

    NavDisplay(
        backStack = backStack,
        entryProvider = entryProvider {
            entry<HomeRoute> {
                HomeScreen(
                    onAddTripClick = {
                        backStack.add(CreateTripRoute())
                    },
                    onTripClick = { tripPlanId ->
                        backStack.add(TripDetailRoute(tripPlanId))
                    },
                    onDraftTripClick = { tripPlanId ->
                        backStack.add(TripPlanningRoute(tripPlanId))
                    },
                    onImportFileSelected = { uri ->
                        backStack.add(TripPlanImportRoute(uri))
                    },
                    onSettingsClick = { backStack.add(SettingsRoute) },
                )
            }
            entry<CreateTripRoute> { route ->
                CreateTripScreen(
                    sessionId = route.sessionId,
                    onBackClick = {
                        backStack.removeLastOrNull()
                    },
                    onTripCreated = { tripPlanId ->
                        backStack.removeLastOrNull()
                        backStack.add(TripPlanningRoute(tripPlanId))
                    }
                )
            }
            entry<TripDetailRoute> { route ->
                TripDetailScreen(
                    tripPlanId = route.tripPlanId,
                    onBackClick = {
                        backStack.removeLastOrNull()
                    },
                    onEditTrip = {
                        backStack.add(TripPlanningRoute(route.tripPlanId))
                    },
                    onAddItinerary = { date ->
                        backStack.add(
                            ItineraryEditorRoute(
                                tripPlanId = route.tripPlanId,
                                initialDate = date.toString(),
                            )
                        )
                    },
                    onEditItinerary = { itemId ->
                        backStack.add(
                            ItineraryEditorRoute(
                                tripPlanId = route.tripPlanId,
                                itemId = itemId,
                            )
                        )
                    },
                    onExportTrip = {
                        backStack.add(TripPlanExportRoute(route.tripPlanId))
                    },
                    onPackingClick = { backStack.add(PackingRoute(route.tripPlanId)) },
                    onItineraryMapClick = { isReadOnly ->
                        backStack.add(ItineraryMapRoute(route.tripPlanId, isReadOnly))
                    },
                )
            }
            entry<TripPlanningRoute> { route ->
                TripPlanningScreen(
                    tripPlanId = route.tripPlanId,
                    onSaveDraft = { backStack.removeLastOrNull() },
                    onFinalized = { tripPlanId ->
                        backStack.removeLastOrNull()
                        backStack.add(TripDetailRoute(tripPlanId))
                    },
                    onEditTripInfo = { backStack.add(EditTripRoute(route.tripPlanId)) },
                    onAddItinerary = { date ->
                        backStack.add(
                            ItineraryEditorRoute(
                                tripPlanId = route.tripPlanId,
                                initialDate = date.toString(),
                            )
                        )
                    },
                    onEditItinerary = { itemId ->
                        backStack.add(ItineraryEditorRoute(route.tripPlanId, itemId = itemId))
                    },
                    onMapClick = { backStack.add(ItineraryMapRoute(route.tripPlanId)) },
                )
            }
            entry<TripPlanExportRoute> { route ->
                TripPlanExportScreen(
                    tripPlanId = route.tripPlanId,
                    onBackClick = { backStack.removeLastOrNull() },
                )
            }
            entry<TripPlanImportRoute> { route ->
                TripPlanImportScreen(
                    uri = route.uri,
                    onBackClick = { backStack.removeLastOrNull() },
                    onImported = { tripPlanId ->
                        backStack.removeLastOrNull()
                        backStack.add(TripDetailRoute(tripPlanId))
                    },
                )
            }
            entry<EditTripRoute> { route ->
                EditTripScreen(
                    tripPlanId = route.tripPlanId,
                    onBackClick = { backStack.removeLastOrNull() },
                    onSaved = { backStack.removeLastOrNull() },
                    onDeleted = {
                        backStack.clear()
                        backStack.add(HomeRoute)
                    },
                )
            }
            entry<PackingRoute> { route ->
                PackingScreen(route.tripPlanId, onBackClick = { backStack.removeLastOrNull() })
            }
            entry<TripCopyRoute> { route ->
                TripCopyScreen(
                    sourceTripId = route.sourceTripId,
                    onBack = { backStack.removeLastOrNull() },
                    onCopied = { newId ->
                        backStack.removeLastOrNull()
                        backStack.add(TripDetailRoute(newId))
                    },
                )
            }
            entry<PlaceRoute> { PlaceScreen(onBack = { backStack.removeLastOrNull() }) }
            entry<SettingsRoute> { SettingsScreen(onBack = { backStack.removeLastOrNull() }) }
            entry<ItineraryEditorRoute> { route ->
                ItineraryEditorScreen(
                    tripPlanId = route.tripPlanId,
                    itemId = route.itemId,
                    initialDate = route.initialDate?.let(LocalDate::parse),
                    sessionId = route.sessionId,
                    onBackClick = { backStack.removeLastOrNull() },
                    onCompleted = { backStack.removeLastOrNull() },
                )
            }
            entry<ItineraryMapRoute> { route ->
                ItineraryMapScreen(
                    tripPlanId = route.tripPlanId,
                    onBackClick = { backStack.removeLastOrNull() },
                    onEditItinerary = if (route.isReadOnly) null else { itemId ->
                        backStack.add(ItineraryEditorRoute(route.tripPlanId, itemId = itemId))
                    },
                )
            }
        }
    )
}
