package com.example.tripplan.domain.usecase

import com.example.tripplan.domain.model.TripPlanDetails
import com.example.tripplan.domain.repository.ItineraryRepository
import com.example.tripplan.domain.repository.TripPlanRepository
import com.example.tripplan.domain.rule.TripDateCalculator
import com.example.tripplan.domain.rule.ItineraryTimeOrderNormalizer
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class ObserveTripPlanDetailsUseCase @Inject constructor(
    private val tripPlanRepository: TripPlanRepository,
    private val itineraryRepository: ItineraryRepository,
) {
    operator fun invoke(tripPlanId: Long): Flow<TripPlanDetails?> {
        return combine(
            tripPlanRepository.observeTripPlan(tripPlanId),
            itineraryRepository.observeItems(tripPlanId),
        ) { tripPlan, items ->
            tripPlan?.let {
                TripPlanDetails(
                    tripPlan = it,
                    days = TripDateCalculator.createDays(it.startDate, it.endDate),
                    itineraryItems = items.groupBy { item -> item.date }
                        .toSortedMap()
                        .values
                        .flatMap(ItineraryTimeOrderNormalizer::normalize),
                )
            }
        }
    }
}
