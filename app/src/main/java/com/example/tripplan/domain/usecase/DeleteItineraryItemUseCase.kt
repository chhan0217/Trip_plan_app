package com.example.tripplan.domain.usecase

import com.example.tripplan.domain.repository.ItineraryRepository
import com.example.tripplan.domain.rule.ItineraryTimeOrderNormalizer
import javax.inject.Inject
import kotlinx.coroutines.flow.first

class DeleteItineraryItemUseCase @Inject constructor(
    private val itineraryRepository: ItineraryRepository,
) {
    suspend operator fun invoke(itemId: Long): ItineraryMutationResult {
        val existing = itineraryRepository.getItem(itemId)
            ?: return ItineraryMutationResult.ItemNotFound
        val remaining = itineraryRepository.observeItems(existing.tripPlanId).first()
            .filter { it.date == existing.date && it.id != itemId }
        val normalized = ItineraryTimeOrderNormalizer.normalize(remaining)
        itineraryRepository.deleteItemAndUpdateItems(itemId, normalized)
        return ItineraryMutationResult.Success(itemId)
    }
}
