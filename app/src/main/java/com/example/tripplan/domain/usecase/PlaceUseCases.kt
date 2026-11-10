package com.example.tripplan.domain.usecase

import com.example.tripplan.domain.model.Place
import com.example.tripplan.domain.model.PlaceInput
import com.example.tripplan.domain.repository.ItineraryRepository
import com.example.tripplan.domain.repository.PlaceRepository
import com.example.tripplan.domain.rule.PlaceValidationError
import com.example.tripplan.domain.rule.PlaceValidator
import java.time.Clock
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class ObservePlacesUseCase @Inject constructor(private val repository: PlaceRepository) {
    operator fun invoke(): Flow<List<Place>> = repository.observePlaces()
}
class CreatePlaceUseCase @Inject constructor(private val repository: PlaceRepository, private val clock: Clock) {
    suspend operator fun invoke(input: PlaceInput): PlaceMutationResult {
        val errors = PlaceValidator.validate(input); if (errors.isNotEmpty()) return PlaceMutationResult.Invalid(errors)
        val now = clock.instant()
        val id = repository.createPlace(Place(0, input.name.trim(), input.address.trim(), input.latitude, input.longitude,
            input.source, input.providerName?.trim(), input.externalId?.trim(), now, now))
        return PlaceMutationResult.Success(id)
    }
}
class UpdatePlaceUseCase @Inject constructor(private val repository: PlaceRepository, private val clock: Clock) {
    suspend operator fun invoke(id: Long, input: PlaceInput): PlaceMutationResult {
        val errors = PlaceValidator.validate(input); if (errors.isNotEmpty()) return PlaceMutationResult.Invalid(errors)
        val old = repository.getPlace(id) ?: return PlaceMutationResult.NotFound
        repository.updatePlace(old.copy(name=input.name.trim(), address=input.address.trim(), latitude=input.latitude,
            longitude=input.longitude, source=input.source, providerName=input.providerName, externalId=input.externalId,
            updatedAt=clock.instant()))
        return PlaceMutationResult.Success(id)
    }
}
class DeletePlaceUseCase @Inject constructor(private val repository: PlaceRepository) {
    suspend operator fun invoke(id: Long): PlaceMutationResult {
        if (repository.getPlace(id) == null) return PlaceMutationResult.NotFound
        repository.deletePlace(id); return PlaceMutationResult.Success(id)
    }
}
class LinkItineraryPlaceUseCase @Inject constructor(
    private val places: PlaceRepository, private val itineraries: ItineraryRepository,
) {
    suspend operator fun invoke(itemId: Long, placeId: Long?): PlaceMutationResult {
        val item = itineraries.getItem(itemId) ?: return PlaceMutationResult.NotFound
        val place = placeId?.let { places.getPlace(it) ?: return PlaceMutationResult.NotFound }
        itineraries.updateItem(item.copy(placeId=place?.id, place=place?.name ?: item.place))
        return PlaceMutationResult.Success(itemId)
    }
}
sealed interface PlaceMutationResult {
    data class Success(val id: Long): PlaceMutationResult
    data class Invalid(val errors: Set<PlaceValidationError>): PlaceMutationResult
    data object NotFound: PlaceMutationResult
}
