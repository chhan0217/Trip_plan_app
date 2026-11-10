package com.example.tripplan.domain.model

data class TripPlanDetails(
    val tripPlan: TripPlan,
    val days: List<TripDay>,
    val itineraryItems: List<ItineraryItem>,
)
