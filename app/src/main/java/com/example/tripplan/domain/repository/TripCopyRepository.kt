package com.example.tripplan.domain.repository

import com.example.tripplan.domain.model.ItineraryItem
import com.example.tripplan.domain.model.TripPlanInput

interface TripCopyRepository {
    suspend fun copyTrip(input: TripPlanInput, itineraryItems: List<ItineraryItem>): Long
}
