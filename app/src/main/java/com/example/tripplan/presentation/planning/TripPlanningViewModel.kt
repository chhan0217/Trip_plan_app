package com.example.tripplan.presentation.planning

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tripplan.domain.model.TripPlanDetails
import com.example.tripplan.domain.usecase.FinalizeTripPlanDraftUseCase
import com.example.tripplan.domain.usecase.GetExternalPlaceDetailsUseCase
import com.example.tripplan.domain.usecase.ManagePlaceCandidatesUseCase
import com.example.tripplan.domain.usecase.ObservePlacesUseCase
import com.example.tripplan.domain.usecase.SearchExternalPlacesUseCase
import com.example.tripplan.domain.usecase.ItineraryMutationResult
import com.example.tripplan.domain.usecase.ObserveTripPlanDetailsUseCase
import com.example.tripplan.domain.usecase.UpdateItineraryItemUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class TripPlanningViewModel @Inject constructor(
    private val observeTripPlanDetails: ObserveTripPlanDetailsUseCase,
    private val finalizeTripPlanDraft: FinalizeTripPlanDraftUseCase,
    private val updateItineraryItem: UpdateItineraryItemUseCase,
    private val managePlaceCandidates: ManagePlaceCandidatesUseCase,
    private val observePlaces: ObservePlacesUseCase,
    private val searchExternalPlaces: SearchExternalPlacesUseCase,
    private val getExternalPlaceDetails: GetExternalPlaceDetailsUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(TripPlanningUiState())
    val uiState: StateFlow<TripPlanningUiState> = _uiState.asStateFlow()

    private val eventChannel = Channel<TripPlanningEvent>(Channel.BUFFERED)
    val events = eventChannel.receiveAsFlow()

    private var loadedTripId: Long? = null
    private var source: TripPlanDetails? = null

    fun load(tripPlanId: Long) {
        if (loadedTripId == tripPlanId) return
        loadedTripId = tripPlanId
        _uiState.value = TripPlanningUiState(isLoading = true)
        viewModelScope.launch {
            try {
                managePlaceCandidates.observe(tripPlanId).collect { candidates ->
                    updateCandidates {
                        copy(items = candidates.map { PlaceCandidateUiModel(it.id, it.name, it.address, it.note, it.scheduledDate) })
                    }
                }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                updateCandidates { copy(error = "후보 장소를 불러오지 못했습니다.") }
            }
        }
        viewModelScope.launch {
            try {
                observePlaces().collect { places ->
                    updateCandidates { copy(savedPlaces = places.map { CandidateSavedPlaceUiModel(it.id, it.name, it.address) }) }
                }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                updateCandidates { copy(error = "저장된 장소를 불러오지 못했습니다.") }
            }
        }
        viewModelScope.launch {
            try {
                observeTripPlanDetails(tripPlanId).collect { details ->
                    source = details
                    _uiState.value = details?.toPlanningUiState(_uiState.value.selectedDate, _uiState.value)
                        ?: TripPlanningUiState(isNotFound = true)
                }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                _uiState.value = TripPlanningUiState(isLoadFailed = true)
            }
        }
    }

    fun searchCandidates(query: String) {
        val countryCode = source?.tripPlan?.countryCode ?: return
        if (query.trim().length < 2 || _uiState.value.candidates.isSearching) return
        updateCandidates { copy(isSearching = true, searchPerformed = false, suggestions = emptyList()) }
        viewModelScope.launch {
            try {
                val suggestions = searchExternalPlaces(countryCode, query)
                updateCandidates {
                    copy(suggestions = suggestions.map { CandidateSuggestionUiModel(it.externalId, it.name, it.addressHint) })
                }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                updateCandidates { copy(suggestions = emptyList()) }
            } finally {
                updateCandidates { copy(isSearching = false, searchPerformed = true) }
            }
        }
    }

    fun addCandidate(name: String, address: String, note: String, savedPlaceId: Long?, externalId: String?) {
        mutateCandidates("후보 장소를 저장하지 못했습니다.") { tripId ->
            val external = externalId?.let { getExternalPlaceDetails(it) ?: return@mutateCandidates false }
            managePlaceCandidates.add(tripId, name, address, note, savedPlaceId, external).also { added ->
                if (added) eventChannel.send(TripPlanningEvent.CandidateAdded)
            }
        }
    }

    fun scheduleCandidate(candidateId: Long, date: LocalDate) {
        mutateCandidates("후보 장소를 일정에 넣지 못했습니다.") { tripId ->
            managePlaceCandidates.schedule(tripId, candidateId, date).also { scheduled ->
                if (scheduled) {
                    selectDay(date)
                    eventChannel.send(TripPlanningEvent.CandidateScheduled)
                }
            }
        }
    }

    fun removeCandidate(candidateId: Long) {
        mutateCandidates("후보 장소를 삭제하지 못했습니다.") {
            managePlaceCandidates.remove(it, candidateId)
        }
    }

    private fun mutateCandidates(message: String, operation: suspend (Long) -> Boolean) {
        val tripId = loadedTripId ?: return
        if (_uiState.value.candidates.isBusy) return
        updateCandidates { copy(isBusy = true, error = null) }
        viewModelScope.launch {
            try {
                val succeeded = operation(tripId)
                updateCandidates { copy(isBusy = false, error = if (succeeded) null else message) }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                updateCandidates { copy(isBusy = false, error = message) }
            }
        }
    }

    private fun updateCandidates(transform: PlaceCandidateListUiState.() -> PlaceCandidateListUiState) {
        _uiState.update { it.copy(candidates = it.candidates.transform()) }
    }

    fun selectDay(date: LocalDate) {
        _uiState.value = source?.toPlanningUiState(date, _uiState.value) ?: return
    }

    fun moveItineraryToDay(itemId: Long, date: LocalDate) {
        val state = _uiState.value
        if (state.isMovingItinerary || state.isFinalizing || date == state.selectedDate ||
            state.days.none { it.date == date } || state.items.none { it.id == itemId }
        ) return
        _uiState.update { it.copy(isMovingItinerary = true, isItineraryMoveFailed = false) }
        viewModelScope.launch {
            val moved = try {
                updateItineraryItem.changeDate(itemId, date) is ItineraryMutationResult.Success
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                false
            }
            if (moved) selectDay(date)
            _uiState.update { it.copy(isMovingItinerary = false, isItineraryMoveFailed = !moved) }
        }
    }

    fun finalizePlan() {
        val tripId = loadedTripId ?: return
        val state = _uiState.value
        if (!state.isDraft || state.isFinalizing || state.isMovingItinerary) return
        viewModelScope.launch {
            _uiState.value = state.copy(isFinalizing = true, isMutationFailed = false)
            try {
                if (finalizeTripPlanDraft(tripId)) {
                    eventChannel.send(TripPlanningEvent.Finalized(tripId))
                } else {
                    _uiState.value = _uiState.value.copy(isFinalizing = false, isMutationFailed = true)
                }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                _uiState.value = _uiState.value.copy(isFinalizing = false, isMutationFailed = true)
            }
        }
    }
}

sealed interface TripPlanningEvent {
    data object CandidateAdded : TripPlanningEvent
    data object CandidateScheduled : TripPlanningEvent
    data class Finalized(val tripPlanId: Long) : TripPlanningEvent
}

data class TripPlanningUiState(
    val isLoading: Boolean = false,
    val isNotFound: Boolean = false,
    val isLoadFailed: Boolean = false,
    val isMutationFailed: Boolean = false,
    val isFinalizing: Boolean = false,
    val isMovingItinerary: Boolean = false,
    val isItineraryMoveFailed: Boolean = false,
    val isDraft: Boolean = false,
    val title: String = "",
    val destination: String = "",
    val countryCode: String = "KR",
    val dateText: String = "",
    val days: List<PlanningDayUiModel> = emptyList(),
    val selectedDate: LocalDate? = null,
    val items: List<PlanningItemUiModel> = emptyList(),
    val candidates: PlaceCandidateListUiState = PlaceCandidateListUiState(),
)

data class PlanningDayUiModel(
    val dayNumber: Int,
    val date: LocalDate,
    val dateText: String,
)

data class PlanningItemUiModel(
    val id: Long,
    val title: String,
    val place: String,
    val timeText: String,
)

private fun TripPlanDetails.toPlanningUiState(
    selectedDate: LocalDate?,
    currentState: TripPlanningUiState,
): TripPlanningUiState {
    val resolvedDate = selectedDate?.takeIf { selected -> days.any { it.date == selected } }
        ?: days.firstOrNull()?.date
    return currentState.copy(
        isLoading = false,
        isNotFound = false,
        isLoadFailed = false,
        isDraft = tripPlan.isDraft,
        title = tripPlan.title,
        destination = tripPlan.destination,
        countryCode = tripPlan.countryCode,
        dateText = "${tripPlan.startDate.format(dateFormatter)} - ${tripPlan.endDate.format(dateFormatter)}",
        days = days.map { PlanningDayUiModel(it.dayNumber, it.date, it.date.format(dayFormatter)) },
        selectedDate = resolvedDate,
        items = itineraryItems.filter { it.date == resolvedDate }.map { item ->
            PlanningItemUiModel(
                id = item.id,
                title = item.title,
                place = item.place,
                timeText = item.startTime?.format(timeFormatter).orEmpty(),
            )
        },
    )
}

private val dateFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd")
private val dayFormatter = DateTimeFormatter.ofPattern("MM.dd")
private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
