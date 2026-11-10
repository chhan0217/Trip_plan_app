package com.example.tripplan.presentation.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tripplan.domain.model.ItineraryItem
import com.example.tripplan.domain.model.TripPlanDetails
import com.example.tripplan.domain.model.TripDatePhase
import com.example.tripplan.domain.usecase.ObserveTripPlanDetailsUseCase
import com.example.tripplan.domain.usecase.CompleteTripPlanUseCase
import com.example.tripplan.domain.usecase.GetTripStatusUseCase
import com.example.tripplan.domain.usecase.TripCompletionResult
import com.example.tripplan.domain.usecase.UndoTripCompletionUseCase
import com.example.tripplan.domain.usecase.ItineraryMoveDirection
import com.example.tripplan.domain.usecase.MoveItineraryItemUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class TripDetailViewModel @Inject constructor(
    private val observeTripPlanDetailsUseCase: ObserveTripPlanDetailsUseCase,
    private val moveItineraryItemUseCase: MoveItineraryItemUseCase,
    private val getTripStatusUseCase: GetTripStatusUseCase,
    private val completeTripPlanUseCase: CompleteTripPlanUseCase,
    private val undoTripCompletionUseCase: UndoTripCompletionUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(TripDetailUiState())
    val uiState: StateFlow<TripDetailUiState> = _uiState.asStateFlow()

    private var observationJob: Job? = null
    private var loadedTripPlanId: Long? = null
    private var currentDetails: TripPlanDetails? = null

    fun load(tripPlanId: Long) {
        if (loadedTripPlanId == tripPlanId) return
        loadedTripPlanId = tripPlanId
        observationJob?.cancel()
        observationJob = viewModelScope.launch {
            _uiState.value = TripDetailUiState(isLoading = true)
            try {
                observeTripPlanDetailsUseCase(tripPlanId).collect { details ->
                    currentDetails = details
                    _uiState.value = details?.toUiState(
                        _uiState.value.selectedDate,
                        getTripStatusUseCase,
                    )
                        ?: TripDetailUiState(isLoading = false, isNotFound = true)
                }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                _uiState.value = TripDetailUiState(isLoading = false, isLoadFailed = true)
            }
        }
    }

    fun onDaySelected(date: LocalDate) {
        val details = currentDetails ?: return
        if (details.days.none { it.date == date }) return
        _uiState.value = details.toUiState(date, getTripStatusUseCase)
    }

    fun onCompletionRequest() {
        _uiState.value = _uiState.value.copy(isCompletionConfirmationVisible = true)
    }

    fun onCompletionDismiss() {
        _uiState.value = _uiState.value.copy(isCompletionConfirmationVisible = false)
    }

    fun onCompletionConfirm() {
        val tripPlanId = loadedTripPlanId ?: return
        val wasCompleted = _uiState.value.isCompleted
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isCompletionConfirmationVisible = false,
                isCompletionSaving = true,
                isMutationFailed = false,
            )
            try {
                val result = if (wasCompleted) {
                    undoTripCompletionUseCase(tripPlanId)
                } else {
                    completeTripPlanUseCase(tripPlanId)
                }
                if (result == TripCompletionResult.NotFound) {
                    _uiState.value = _uiState.value.copy(isNotFound = true)
                } else {
                    _uiState.value = _uiState.value.copy(isCompletionSaving = false)
                }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                _uiState.value = _uiState.value.copy(
                    isCompletionSaving = false,
                    isMutationFailed = true,
                )
            }
        }
    }

    fun onMoveUp(itemId: Long) = moveItem(itemId, ItineraryMoveDirection.Up)

    fun onMoveDown(itemId: Long) = moveItem(itemId, ItineraryMoveDirection.Down)

    private fun moveItem(itemId: Long, direction: ItineraryMoveDirection) {
        viewModelScope.launch {
            try {
                moveItineraryItemUseCase(itemId, direction)
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                _uiState.value = _uiState.value.copy(isMutationFailed = true)
            }
        }
    }
}

data class TripDetailUiState(
    val isLoading: Boolean = true,
    val isNotFound: Boolean = false,
    val isLoadFailed: Boolean = false,
    val isMutationFailed: Boolean = false,
    val title: String = "",
    val destination: String = "",
    val dateText: String = "",
    val description: String = "",
    val days: List<TripDayUiModel> = emptyList(),
    val selectedDate: LocalDate? = null,
    val itineraryItems: List<ItineraryItemUiModel> = emptyList(),
    val statusText: String = "",
    val isCompleted: Boolean = false,
    val isCompletionSaving: Boolean = false,
    val isCompletionConfirmationVisible: Boolean = false,
)

data class TripDayUiModel(
    val dayNumber: Int,
    val date: LocalDate,
    val dateText: String,
)

data class ItineraryItemUiModel(
    val id: Long,
    val title: String,
    val place: String,
    val timeText: String,
    val note: String,
    val canMoveUp: Boolean,
    val canMoveDown: Boolean,
)

private fun TripPlanDetails.toUiState(
    previousSelectedDate: LocalDate?,
    getTripStatus: GetTripStatusUseCase,
): TripDetailUiState {
    val selectedDate = previousSelectedDate
        ?.takeIf { selected -> days.any { it.date == selected } }
        ?: days.firstOrNull()?.date

    val selectedItems = itineraryItems.filter { it.date == selectedDate }

    val status = getTripStatus(tripPlan)
    return TripDetailUiState(
        isLoading = false,
        title = tripPlan.title,
        destination = tripPlan.destination,
        dateText = "${tripPlan.startDate.format(detailDateFormatter)} - " +
            tripPlan.endDate.format(detailDateFormatter),
        description = tripPlan.description,
        isCompleted = status.isCompleted,
        statusText = if (status.isCompleted) "완료" else when (status.dateStatus.phase) {
            TripDatePhase.Upcoming -> "D-${status.dateStatus.daysUntilStart}"
            TripDatePhase.Ongoing -> if (status.dateStatus.currentDayNumber == 1) {
                "D-Day"
            } else {
                "여행 ${status.dateStatus.currentDayNumber}일차"
            }
            TripDatePhase.Ended -> "여행 종료"
        },
        days = days.map {
            TripDayUiModel(
                dayNumber = it.dayNumber,
                date = it.date,
                dateText = it.date.format(dayDateFormatter),
            )
        },
        selectedDate = selectedDate,
        itineraryItems = selectedItems.mapIndexed { index, item ->
                item.toUiModel(
                    canMoveUp = index > 0 && selectedItems[index - 1].startTime == item.startTime,
                    canMoveDown = index < selectedItems.lastIndex &&
                        selectedItems[index + 1].startTime == item.startTime,
                )
            },
    )
}

private fun ItineraryItem.toUiModel(
    canMoveUp: Boolean,
    canMoveDown: Boolean,
) = ItineraryItemUiModel(
    id = id,
    title = title,
    place = place,
    timeText = formatTimeRange(startTime, endTime),
    note = note,
    canMoveUp = canMoveUp,
    canMoveDown = canMoveDown,
)

private fun formatTimeRange(startTime: LocalTime?, endTime: LocalTime?): String = when {
    startTime != null && endTime != null ->
        "${startTime.format(timeFormatter)} - ${endTime.format(timeFormatter)}"
    startTime != null -> startTime.format(timeFormatter)
    endTime != null -> "~ ${endTime.format(timeFormatter)}"
    else -> ""
}

private val detailDateFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd")
private val dayDateFormatter = DateTimeFormatter.ofPattern("MM.dd")
private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
