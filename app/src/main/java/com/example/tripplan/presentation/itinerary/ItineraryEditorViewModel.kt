package com.example.tripplan.presentation.itinerary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tripplan.domain.model.ItineraryItemInput
import com.example.tripplan.domain.rule.ItineraryItemValidationError
import com.example.tripplan.domain.usecase.CreateItineraryItemUseCase
import com.example.tripplan.domain.usecase.DeleteItineraryItemUseCase
import com.example.tripplan.domain.usecase.ItineraryMutationResult
import com.example.tripplan.domain.usecase.PrepareItineraryEditorUseCase
import com.example.tripplan.domain.usecase.UpdateItineraryItemUseCase
import com.example.tripplan.domain.usecase.ObservePlacesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
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
) : ViewModel() {
    private val _uiState = MutableStateFlow(ItineraryEditorUiState())
    val uiState: StateFlow<ItineraryEditorUiState> = _uiState.asStateFlow()

    private val eventChannel = Channel<ItineraryEditorEvent>(Channel.BUFFERED)
    val events = eventChannel.receiveAsFlow()
    private var loadedKey: Pair<Long, Long?>? = null

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
                val selectedDate = item?.date
                    ?: initialDate?.takeIf { date -> data.days.any { it.date == date } }
                    ?: data.days.firstOrNull()?.date
                _uiState.value = ItineraryEditorUiState(
                    isLoading = false,
                    tripPlanId = tripPlanId,
                    itemId = itemId,
                    isEdit = item != null,
                    days = data.days.map { EditorDayUiModel(it.dayNumber, it.date) },
                    selectedDate = selectedDate,
                    title = item?.title.orEmpty(),
                    place = item?.place.orEmpty(),
                    selectedPlaceId = item?.placeId,
                    savedPlaces = places.map { SavedPlaceUiModel(it.id, it.name, it.address) },
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
    fun onPlaceChange(value: String) = updateInput { copy(place = value, selectedPlaceId = null) }
    fun onSavedPlaceSelected(id: Long) = updateInput {
        val selected = savedPlaces.firstOrNull { it.id == id }
        copy(place = selected?.name.orEmpty(), selectedPlaceId = selected?.id)
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
            val input = ItineraryItemInput(
                date = selectedDate,
                title = state.title,
                place = state.place,
                startTime = startTime,
                endTime = endTime,
                note = state.note,
                placeId = state.selectedPlaceId,
            )
            try {
                val result = state.itemId?.let { updateItem(it, input) }
                    ?: createItem(state.tripPlanId, input)
                handleMutationResult(result, ItineraryEditorEvent.Saved)
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
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
}

data class ItineraryEditorUiState(
    val isLoading: Boolean = true,
    val isNotFound: Boolean = false,
    val isLoadFailed: Boolean = false,
    val tripPlanId: Long = 0L,
    val itemId: Long? = null,
    val isEdit: Boolean = false,
    val days: List<EditorDayUiModel> = emptyList(),
    val selectedDate: LocalDate? = null,
    val title: String = "",
    val place: String = "",
    val selectedPlaceId: Long? = null,
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
data class SavedPlaceUiModel(val id: Long, val name: String, val address: String)

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
