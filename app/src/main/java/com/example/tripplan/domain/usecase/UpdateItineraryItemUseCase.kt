package com.example.tripplan.domain.usecase

import com.example.tripplan.domain.model.ItineraryItemInput
import com.example.tripplan.domain.repository.ItineraryRepository
import com.example.tripplan.domain.repository.TripPlanRepository
import com.example.tripplan.domain.rule.ItineraryItemValidator
import com.example.tripplan.domain.rule.ItineraryTimeOrderNormalizer
import javax.inject.Inject
import kotlinx.coroutines.flow.first

class UpdateItineraryItemUseCase @Inject constructor(
    private val tripPlanRepository: TripPlanRepository,
    private val itineraryRepository: ItineraryRepository,
) {
    suspend operator fun invoke(
        itemId: Long,
        input: ItineraryItemInput,
    ): ItineraryMutationResult {
        val existing = itineraryRepository.getItem(itemId)
            ?: return ItineraryMutationResult.ItemNotFound
        val tripPlan = tripPlanRepository.getTripPlan(existing.tripPlanId)
            ?: return ItineraryMutationResult.TripNotFound
        val errors = ItineraryItemValidator.validate(input, tripPlan.startDate, tripPlan.endDate)
        if (errors.isNotEmpty()) return ItineraryMutationResult.Invalid(errors)

        val allItems = itineraryRepository.observeItems(existing.tripPlanId).first()
        val newOrder = if (input.date == existing.date) {
            existing.order
        } else {
            allItems.filter { it.date == input.date }.maxOfOrNull { it.order }?.plus(1) ?: 0
        }
        val updated = existing.copy(
            date = input.date,
            title = input.title.trim(),
            place = input.place.trim(),
            startTime = input.startTime,
            endTime = input.endTime,
            note = input.note.trim(),
            order = newOrder,
        )
        val replacedItems = allItems.map { if (it.id == itemId) updated else it }
        val affectedDates = setOf(existing.date, updated.date)
        val normalized = affectedDates.flatMap { date ->
            ItineraryTimeOrderNormalizer.normalize(replacedItems.filter { it.date == date })
        }
        itineraryRepository.updateItems(normalized)
        return ItineraryMutationResult.Success(itemId)
    }
}
