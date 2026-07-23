package com.example.tripplan.presentation

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import com.example.tripplan.presentation.navigation.TripPlanNavHost

@Composable
fun TripPlanApp() {
    MaterialTheme {
        Surface {
            TripPlanNavHost()
        }
    }
}
