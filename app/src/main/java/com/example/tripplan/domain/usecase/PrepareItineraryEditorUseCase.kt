package com.example.tripplan.domain.usecase

import com.example.tripplan.domain.model.ItineraryItem
import com.example.tripplan.domain.model.TripDay
import com.example.tripplan.domain.repository.ItineraryRepository
import com.example.tripplan.domain.repository.TripPlanRepository
import com.example.tripplan.domain.rule.TripDateCalculator
import javax.inject.Inject

data class ItineraryEditorData(
    val tripPlanId: Long,
    val days: List<TripDay>,
    val item: ItineraryItem?,
)

class PrepareItineraryEditorUseCase @Inject constructor(
    private val tripPlanRepository: TripPlanRepository,
    private val itineraryRepository: ItineraryRepository,
) {
    suspend operator fun invoke(
        tripPlanId: Long,
        itemId: Long?,
    ): ItineraryEditorData? {
        val tripPlan = tripPlanRepository.getTripPlan(tripPlanId) ?: return null
        val item = itemId?.let { itineraryRepository.getItem(it) }
        if (itemId != null && (item == null || item.tripPlanId != tripPlanId)) return null
        return ItineraryEditorData(
            tripPlanId = tripPlanId,
            days = TripDateCalculator.createDays(tripPlan.startDate, tripPlan.endDate),
            item = item,
        )
    }
}
