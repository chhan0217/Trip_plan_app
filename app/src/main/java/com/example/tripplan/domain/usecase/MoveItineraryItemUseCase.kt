package com.example.tripplan.domain.usecase

import com.example.tripplan.domain.repository.ItineraryRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.first

class MoveItineraryItemUseCase @Inject constructor(
    private val itineraryRepository: ItineraryRepository,
) {
    suspend operator fun invoke(
        itemId: Long,
        direction: ItineraryMoveDirection,
    ): ItineraryMutationResult {
        val existing = itineraryRepository.getItem(itemId)
            ?: return ItineraryMutationResult.ItemNotFound
        val sameDayItems = itineraryRepository.observeItems(existing.tripPlanId).first()
            .filter { it.date == existing.date }
            .sortedWith(compareBy({ it.order }, { it.id }))
        val currentIndex = sameDayItems.indexOfFirst { it.id == itemId }
        val targetIndex = when (direction) {
            ItineraryMoveDirection.Up -> currentIndex - 1
            ItineraryMoveDirection.Down -> currentIndex + 1
        }
        if (currentIndex < 0 || targetIndex !in sameDayItems.indices) {
            return ItineraryMutationResult.Success(itemId)
        }
        if (sameDayItems[targetIndex].startTime != existing.startTime) {
            return ItineraryMutationResult.Success(itemId)
        }

        val reordered = sameDayItems.toMutableList().apply {
            val moved = removeAt(currentIndex)
            add(targetIndex, moved)
        }.mapIndexed { index, item -> item.copy(order = index) }
        itineraryRepository.updateItems(reordered)
        return ItineraryMutationResult.Success(itemId)
    }
}
