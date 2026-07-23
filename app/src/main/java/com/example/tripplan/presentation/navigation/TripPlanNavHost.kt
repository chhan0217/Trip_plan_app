package com.example.tripplan.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.example.tripplan.presentation.create.CreateTripScreen
import com.example.tripplan.presentation.home.HomeScreen

@Composable
fun TripPlanNavHost() {
    val backStack = rememberNavBackStack(HomeRoute)

    NavDisplay(
        backStack = backStack,
        entryProvider = entryProvider {
            entry<HomeRoute> {
                HomeScreen(
                    onAddTripClick = {
                        backStack.add(CreateTripRoute)
                    }
                )
            }
            entry<CreateTripRoute> {
                CreateTripScreen(
                    onBackClick = {
                        backStack.removeLastOrNull()
                    }
                )
            }
        }
    )
}
