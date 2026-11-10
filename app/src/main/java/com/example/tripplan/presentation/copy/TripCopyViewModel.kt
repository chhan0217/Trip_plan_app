package com.example.tripplan.presentation.copy

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tripplan.domain.model.TripPlanInput
import com.example.tripplan.domain.repository.TripPlanRepository
import com.example.tripplan.domain.repository.ItineraryRepository
import com.example.tripplan.domain.usecase.CopyTripPlanUseCase
import com.example.tripplan.domain.usecase.CopyTripResult
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import java.time.temporal.ChronoUnit

@HiltViewModel
class TripCopyViewModel @Inject constructor(
    private val tripRepository: TripPlanRepository,
    private val copyTrip: CopyTripPlanUseCase,
    private val itineraryRepository: ItineraryRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(TripCopyUiState())
    val state = _state.asStateFlow()
    private val eventsChannel = Channel<Long>(Channel.BUFFERED)
    val events = eventsChannel.receiveAsFlow()
    private var sourceId: Long? = null

    fun load(id: Long) {
        if (sourceId == id) return
        sourceId = id
        viewModelScope.launch {
            val trip = tripRepository.getTripPlan(id)
            _state.value = if (trip == null) TripCopyUiState(isLoading = false, isError = true) else {
                val count = java.time.temporal.ChronoUnit.DAYS.between(trip.startDate, trip.endDate).toInt() + 1
                val counts = itineraryRepository.observeItems(id).first().groupingBy {
                    ChronoUnit.DAYS.between(trip.startDate, it.date).toInt() + 1
                }.eachCount()
                TripCopyUiState(false, title = "${trip.title} 복사", destination = trip.destination,
                    startDate = trip.startDate, endDate = trip.endDate, description = trip.description,
                    originalDayCount = count, retainedDays = (1..count).toSet(), itemCountsByDay = counts)
            }
        }
    }
    fun updateTitle(value: String) { _state.value = _state.value.copy(title = value) }
    fun updateDates(start: LocalDate, end: LocalDate) { _state.value = _state.value.copy(startDate = start, endDate = end) }
    fun toggleInclude() { _state.value = _state.value.copy(includeItinerary = !_state.value.includeItinerary) }
    fun toggleDay(day: Int) { val days = _state.value.retainedDays; _state.value = _state.value.copy(retainedDays = if (day in days) days - day else days + day) }
    fun copy() {
        val id = sourceId ?: return
        val s = _state.value
        viewModelScope.launch {
            _state.value = s.copy(isSaving = true, isError = false)
            when (val result = copyTrip(id, TripPlanInput(s.title, s.destination, s.startDate, s.endDate, s.description), s.includeItinerary, s.retainedDays)) {
                is CopyTripResult.Success -> eventsChannel.send(result.tripPlanId)
                else -> _state.value = _state.value.copy(isSaving = false, isError = true)
            }
        }
    }
}

data class TripCopyUiState(
    val isLoading: Boolean = true, val isSaving: Boolean = false, val isError: Boolean = false,
    val title: String = "", val destination: String = "", val startDate: LocalDate = LocalDate.now(),
    val endDate: LocalDate = LocalDate.now(), val description: String = "", val includeItinerary: Boolean = true,
    val originalDayCount: Int = 0, val retainedDays: Set<Int> = emptySet(),
    val itemCountsByDay: Map<Int, Int> = emptyMap(),
)
