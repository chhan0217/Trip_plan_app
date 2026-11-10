package com.example.tripplan.domain.usecase

import com.example.tripplan.domain.model.ItineraryItem
import com.example.tripplan.domain.model.ItineraryItemInput
import com.example.tripplan.domain.repository.ItineraryRepository
import com.example.tripplan.domain.repository.TripPlanRepository
import com.example.tripplan.domain.rule.ItineraryItemValidator
import com.example.tripplan.domain.rule.ItineraryTimeOrderNormalizer
import javax.inject.Inject
import kotlinx.coroutines.flow.first

class CreateItineraryItemUseCase @Inject constructor(
    private val tripPlanRepository: TripPlanRepository,
    private val itineraryRepository: ItineraryRepository,
) {
    suspend operator fun invoke(
        tripPlanId: Long,
        input: ItineraryItemInput,
    ): ItineraryMutationResult {
        val tripPlan = tripPlanRepository.getTripPlan(tripPlanId)
            ?: return ItineraryMutationResult.TripNotFound
        val errors = ItineraryItemValidator.validate(input, tripPlan.startDate, tripPlan.endDate)
        if (errors.isNotEmpty()) return ItineraryMutationResult.Invalid(errors)

        val nextOrder = itineraryRepository.observeItems(tripPlanId).first()
            .filter { it.date == input.date }
            .maxOfOrNull { it.order }
            ?.plus(1)
            ?: 0
        val itemId = itineraryRepository.createItem(
            ItineraryItem(
                id = 0L,
                tripPlanId = tripPlanId,
                date = input.date,
                title = input.title.trim(),
                place = input.place.trim(),
                startTime = input.startTime,
                endTime = input.endTime,
                note = input.note.trim(),
                order = nextOrder,
            )
        )
        val normalized = ItineraryTimeOrderNormalizer.normalize(
            itineraryRepository.observeItems(tripPlanId).first()
                .filter { it.date == input.date }
        )
        itineraryRepository.updateItems(normalized)
        return ItineraryMutationResult.Success(itemId)
    }
}
