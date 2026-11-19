package com.example.tripplan.domain.usecase

import com.example.tripplan.domain.model.ItineraryMapData
import com.example.tripplan.domain.model.ItineraryMapItem
import com.example.tripplan.domain.repository.PlaceRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class ObserveItineraryMapUseCase @Inject constructor(
    private val observeTripPlanDetails: ObserveTripPlanDetailsUseCase,
    private val placeRepository: PlaceRepository,
) {
    operator fun invoke(tripPlanId: Long): Flow<ItineraryMapData?> = combine(
        observeTripPlanDetails(tripPlanId),
        placeRepository.observePlaces(),
    ) { details, places ->
        details?.let {
            val placesById = places.associateBy { place -> place.id }
            val uniquePlacesByName = places
                .groupBy { place -> place.name.trim() }
                .mapNotNull { (name, matches) -> matches.singleOrNull()?.let { name to it } }
                .toMap()
            val orderByDate = mutableMapOf<java.time.LocalDate, Int>()
            ItineraryMapData(
                tripTitle = it.tripPlan.title,
                days = it.days,
                items = it.itineraryItems.map { item ->
                    val place = item.placeId?.let(placesById::get)
                        ?: uniquePlacesByName[item.place.trim()]
                    val order = (orderByDate[item.date] ?: 0) + 1
                    orderByDate[item.date] = order
                    ItineraryMapItem(
                        id = item.id,
                        date = item.date,
                        title = item.title,
                        placeName = item.place.ifBlank { place?.name.orEmpty() },
                        address = place?.address.orEmpty(),
                        startTime = item.startTime,
                        orderInDay = order,
                        latitude = place?.latitude,
                        longitude = place?.longitude,
                    )
                },
            )
        }
    }
}
