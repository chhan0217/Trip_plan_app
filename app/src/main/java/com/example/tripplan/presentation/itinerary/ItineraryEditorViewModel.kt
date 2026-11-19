package com.example.tripplan.presentation.itinerary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tripplan.domain.model.ItineraryItemInput
import com.example.tripplan.domain.model.PlaceInput
import com.example.tripplan.domain.model.PlaceSource
import com.example.tripplan.domain.rule.ItineraryItemValidationError
import com.example.tripplan.domain.usecase.CreateItineraryItemUseCase
import com.example.tripplan.domain.usecase.DeleteItineraryItemUseCase
import com.example.tripplan.domain.usecase.ItineraryMutationResult
import com.example.tripplan.domain.usecase.PrepareItineraryEditorUseCase
import com.example.tripplan.domain.usecase.UpdateItineraryItemUseCase
import com.example.tripplan.domain.usecase.ObservePlacesUseCase
import com.example.tripplan.domain.usecase.CreatePlaceUseCase
import com.example.tripplan.domain.usecase.DeletePlaceUseCase
import com.example.tripplan.domain.usecase.GetExternalPlaceDetailsUseCase
import com.example.tripplan.domain.usecase.PlaceMutationResult
import com.example.tripplan.domain.usecase.SearchExternalPlacesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class ItineraryEditorViewModel @Inject constructor(
    private val prepareEditor: PrepareItineraryEditorUseCase,
    private val createItem: CreateItineraryItemUseCase,
    private val updateItem: UpdateItineraryItemUseCase,
    private val deleteItem: DeleteItineraryItemUseCase,
    private val observePlaces: ObservePlacesUseCase,
    private val searchExternalPlaces: SearchExternalPlacesUseCase,
    private val getExternalPlaceDetails: GetExternalPlaceDetailsUseCase,
    private val createPlace: CreatePlaceUseCase,
    private val deletePlace: DeletePlaceUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ItineraryEditorUiState())
    val uiState: StateFlow<ItineraryEditorUiState> = _uiState.asStateFlow()

    private val eventChannel = Channel<ItineraryEditorEvent>(Channel.BUFFERED)
    val events = eventChannel.receiveAsFlow()
    private var loadedKey: Pair<Long, Long?>? = null
    private var placeSearchJob: Job? = null

    fun load(tripPlanId: Long, itemId: Long?, initialDate: LocalDate?) {
        val key = tripPlanId to itemId
        if (loadedKey == key) return
        loadedKey = key
        viewModelScope.launch {
            try {
                val data = prepareEditor(tripPlanId, itemId)
                if (data == null) {
                    _uiState.value = ItineraryEditorUiState(isLoading = false, isNotFound = true)
                    return@launch
                }
                val item = data.item
                val places = observePlaces().first()
                val selectedPlace = item?.placeId?.let { id -> places.firstOrNull { it.id == id } }
                val selectedDate = item?.date
                    ?: initialDate?.takeIf { date -> data.days.any { it.date == date } }
                    ?: data.days.firstOrNull()?.date
                _uiState.value = ItineraryEditorUiState(
                    isLoading = false,
                    tripPlanId = tripPlanId,
                    countryCode = data.countryCode,
                    itemId = itemId,
                    isEdit = item != null,
                    days = data.days.map { EditorDayUiModel(it.dayNumber, it.date) },
                    selectedDate = selectedDate,
                    title = item?.title.orEmpty(),
                    place = item?.place.orEmpty(),
                    selectedPlaceId = item?.placeId,
                    selectedPlaceAddress = selectedPlace?.address.orEmpty(),
                    selectedPlaceLatitude = selectedPlace?.latitude,
                    selectedPlaceLongitude = selectedPlace?.longitude,
                    selectedExternalPlaceId = selectedPlace?.externalId,
                    savedPlaces = places.map {
                        SavedPlaceUiModel(
                            it.id,
                            it.name,
                            it.address,
                            it.latitude,
                            it.longitude,
                            it.externalId,
                        )
                    },
                    startTime = item?.startTime?.format(timeFormatter).orEmpty(),
                    endTime = item?.endTime?.format(timeFormatter).orEmpty(),
                    note = item?.note.orEmpty(),
                )
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                _uiState.value = ItineraryEditorUiState(isLoading = false, isLoadFailed = true)
            }
        }
    }

    fun onTitleChange(value: String) = updateInput { copy(title = value, isTitleError = false) }
    fun onPlaceChange(value: String) {
        updateInput {
            copy(
                place = value,
                selectedPlaceId = null,
                selectedPlaceAddress = "",
                selectedPlaceLatitude = null,
                selectedPlaceLongitude = null,
                selectedExternalPlaceId = null,
                placeSuggestions = emptyList(),
                isPlaceSearchLoading = value.trim().length >= MIN_PLACE_QUERY_LENGTH,
            )
        }
        searchPlaces(value)
    }

    fun onPlaceSuggestionSelected(suggestion: PlaceSuggestionUiModel) {
        placeSearchJob?.cancel()
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    place = suggestion.name,
                    placeSuggestions = emptyList(),
                    isPlaceSearchLoading = true,
                )
            }
            val details = getExternalPlaceDetails(suggestion.externalId)
            _uiState.update {
                if (details == null) {
                    it.copy(isPlaceSearchLoading = false)
                } else {
                    it.copy(
                        place = details.name.ifBlank { suggestion.name },
                        selectedPlaceAddress = details.address,
                        selectedPlaceLatitude = details.latitude,
                        selectedPlaceLongitude = details.longitude,
                        selectedExternalPlaceId = details.externalId,
                        selectedPlaceId = null,
                        isPlaceSearchLoading = false,
                    )
                }
            }
        }
    }

    fun onSavedPlaceSelected(id: Long) = updateInput {
        val selected = savedPlaces.firstOrNull { it.id == id }
        copy(
            place = selected?.name.orEmpty(),
            selectedPlaceId = selected?.id,
            selectedPlaceAddress = selected?.address.orEmpty(),
            selectedPlaceLatitude = selected?.latitude,
            selectedPlaceLongitude = selected?.longitude,
            selectedExternalPlaceId = selected?.externalId,
            placeSuggestions = emptyList(),
        )
    }
    fun onDateSelected(value: LocalDate) = updateInput { copy(selectedDate = value) }
    fun onStartTimeChange(value: String) = updateInput { copy(startTime = value, isTimeError = false) }
    fun onEndTimeChange(value: String) = updateInput { copy(endTime = value, isTimeError = false) }
    fun onNoteChange(value: String) = updateInput { copy(note = value) }

    fun onSaveClick() {
        val state = _uiState.value
        if (state.isSaving) return
        val selectedDate = state.selectedDate ?: return
        val startTime = state.startTime.parseTime()
        val endTime = state.endTime.parseTime()
        val hasTimeParseError = (state.startTime.isNotBlank() && startTime == null) ||
            (state.endTime.isNotBlank() && endTime == null)
        if (state.title.isBlank() || hasTimeParseError) {
            _uiState.update {
                it.copy(isTitleError = state.title.isBlank(), isTimeError = hasTimeParseError)
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, isSaveFailed = false) }
            var createdPlaceId: Long? = null
            try {
                val resolvedPlaceId = if (
                    state.selectedPlaceId == null && state.selectedExternalPlaceId != null
                ) {
                    when (
                        val result = createPlace(
                            PlaceInput(
                                name = state.place,
                                address = state.selectedPlaceAddress,
                                latitude = state.selectedPlaceLatitude,
                                longitude = state.selectedPlaceLongitude,
                                source = PlaceSource.ExternalProvider,
                                providerName = GOOGLE_PROVIDER,
                                externalId = state.selectedExternalPlaceId,
                            )
                        )
                    ) {
                        is PlaceMutationResult.Success -> result.id.also { createdPlaceId = it }
                        else -> {
                            _uiState.update { it.copy(isSaving = false, isSaveFailed = true) }
                            return@launch
                        }
                    }
                } else {
                    state.selectedPlaceId
                }
                val input = ItineraryItemInput(
                    date = selectedDate,
                    title = state.title,
                    place = state.place,
                    startTime = startTime,
                    endTime = endTime,
                    note = state.note,
                    placeId = resolvedPlaceId,
                )
                val result = state.itemId?.let { updateItem(it, input) }
                    ?: createItem(state.tripPlanId, input)
                if (result !is ItineraryMutationResult.Success) {
                    createdPlaceId?.let { deletePlace(it) }
                }
                handleMutationResult(result, ItineraryEditorEvent.Saved)
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                createdPlaceId?.let { runCatching { deletePlace(it) } }
                _uiState.update { it.copy(isSaving = false, isSaveFailed = true) }
            }
        }
    }

    fun onDeleteRequest() = _uiState.update { it.copy(isDeleteConfirmationVisible = true) }
    fun onDeleteDismiss() = _uiState.update { it.copy(isDeleteConfirmationVisible = false) }

    fun onDeleteConfirm() {
        val itemId = _uiState.value.itemId ?: return
        viewModelScope.launch {
            _uiState.update {
                it.copy(isSaving = true, isDeleteConfirmationVisible = false, isSaveFailed = false)
            }
            try {
                handleMutationResult(deleteItem(itemId), ItineraryEditorEvent.Deleted)
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                _uiState.update { it.copy(isSaving = false, isSaveFailed = true) }
            }
        }
    }

    private suspend fun handleMutationResult(
        result: ItineraryMutationResult,
        successEvent: ItineraryEditorEvent,
    ) {
        when (result) {
            is ItineraryMutationResult.Success -> {
                _uiState.update { it.copy(isSaving = false) }
                eventChannel.send(successEvent)
            }
            is ItineraryMutationResult.Invalid -> _uiState.update {
                it.copy(
                    isSaving = false,
                    isTitleError = ItineraryItemValidationError.BlankTitle in result.errors,
                    isTimeError = ItineraryItemValidationError.InvalidTimeRange in result.errors,
                    isSaveFailed = ItineraryItemValidationError.DateOutsideTrip in result.errors,
                )
            }
            ItineraryMutationResult.ItemNotFound,
            ItineraryMutationResult.TripNotFound -> _uiState.update {
                it.copy(isSaving = false, isNotFound = true)
            }
        }
    }

    private fun updateInput(transform: ItineraryEditorUiState.() -> ItineraryEditorUiState) {
        _uiState.update { it.transform().copy(isSaveFailed = false) }
    }

    private fun searchPlaces(query: String) {
        placeSearchJob?.cancel()
        val normalized = query.trim()
        if (normalized.length < MIN_PLACE_QUERY_LENGTH) {
            _uiState.update {
                it.copy(placeSuggestions = emptyList(), isPlaceSearchLoading = false)
            }
            return
        }
        placeSearchJob = viewModelScope.launch {
            delay(PLACE_SEARCH_DEBOUNCE_MILLIS)
            val state = _uiState.value
            val results = searchExternalPlaces(state.countryCode, normalized)
            _uiState.update {
                it.copy(
                    placeSuggestions = results.map { result ->
                        PlaceSuggestionUiModel(
                            externalId = result.externalId,
                            name = result.name,
                            addressHint = result.addressHint,
                        )
                    },
                    isPlaceSearchLoading = false,
                )
            }
        }
    }

    private companion object {
        const val MIN_PLACE_QUERY_LENGTH = 2
        const val PLACE_SEARCH_DEBOUNCE_MILLIS = 400L
        const val GOOGLE_PROVIDER = "Google Places"
    }
}

data class ItineraryEditorUiState(
    val isLoading: Boolean = true,
    val isNotFound: Boolean = false,
    val isLoadFailed: Boolean = false,
    val tripPlanId: Long = 0L,
    val countryCode: String = "KR",
    val itemId: Long? = null,
    val isEdit: Boolean = false,
    val days: List<EditorDayUiModel> = emptyList(),
    val selectedDate: LocalDate? = null,
    val title: String = "",
    val place: String = "",
    val selectedPlaceId: Long? = null,
    val selectedPlaceAddress: String = "",
    val selectedPlaceLatitude: Double? = null,
    val selectedPlaceLongitude: Double? = null,
    val selectedExternalPlaceId: String? = null,
    val placeSuggestions: List<PlaceSuggestionUiModel> = emptyList(),
    val isPlaceSearchLoading: Boolean = false,
    val savedPlaces: List<SavedPlaceUiModel> = emptyList(),
    val startTime: String = "",
    val endTime: String = "",
    val note: String = "",
    val isTitleError: Boolean = false,
    val isTimeError: Boolean = false,
    val isSaving: Boolean = false,
    val isSaveFailed: Boolean = false,
    val isDeleteConfirmationVisible: Boolean = false,
)

data class EditorDayUiModel(val dayNumber: Int, val date: LocalDate)
data class SavedPlaceUiModel(
    val id: Long,
    val name: String,
    val address: String,
    val latitude: Double?,
    val longitude: Double?,
    val externalId: String?,
)

data class PlaceSuggestionUiModel(
    val externalId: String,
    val name: String,
    val addressHint: String,
)

sealed interface ItineraryEditorEvent {
    data object Saved : ItineraryEditorEvent
    data object Deleted : ItineraryEditorEvent
}

private fun String.parseTime(): LocalTime? {
    if (isBlank()) return null
    return try {
        LocalTime.parse(trim(), timeFormatter)
    } catch (_: DateTimeParseException) {
        null
    }
}

private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
