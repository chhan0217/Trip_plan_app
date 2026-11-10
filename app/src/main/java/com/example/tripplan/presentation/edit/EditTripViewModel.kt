package com.example.tripplan.presentation.edit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tripplan.domain.model.TripPlanInput
import com.example.tripplan.domain.rule.TripPlanValidationError
import com.example.tripplan.domain.usecase.DeleteTripPlanResult
import com.example.tripplan.domain.usecase.DeleteTripPlanUseCase
import com.example.tripplan.domain.usecase.ObserveTripPlanDetailsUseCase
import com.example.tripplan.domain.usecase.UpdateTripPlanResult
import com.example.tripplan.domain.usecase.UpdateTripPlanUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class EditTripViewModel @Inject constructor(
    private val observeTripPlanDetails: ObserveTripPlanDetailsUseCase,
    private val updateTripPlan: UpdateTripPlanUseCase,
    private val deleteTripPlan: DeleteTripPlanUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(EditTripUiState())
    val uiState: StateFlow<EditTripUiState> = _uiState.asStateFlow()

    private val eventChannel = Channel<EditTripEvent>(Channel.BUFFERED)
    val events = eventChannel.receiveAsFlow()
    private var loadedTripPlanId: Long? = null

    fun load(tripPlanId: Long) {
        if (loadedTripPlanId == tripPlanId) return
        loadedTripPlanId = tripPlanId
        viewModelScope.launch {
            try {
                val details = observeTripPlanDetails(tripPlanId).first()
                val trip = details?.tripPlan
                _uiState.value = if (trip == null) {
                    EditTripUiState(isLoading = false, isNotFound = true)
                } else {
                    EditTripUiState(
                        isLoading = false,
                        tripPlanId = trip.id,
                        title = trip.title,
                        destination = trip.destination,
                        startDate = trip.startDate,
                        endDate = trip.endDate,
                        description = trip.description,
                    )
                }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                _uiState.value = EditTripUiState(isLoading = false, isLoadFailed = true)
            }
        }
    }

    fun onTitleChange(value: String) = updateInput { copy(title = value, isTitleError = false) }
    fun onDestinationChange(value: String) = updateInput { copy(destination = value) }
    fun onDateRangeChange(startDate: LocalDate?, endDate: LocalDate?) = updateInput {
        copy(startDate = startDate, endDate = endDate, dateError = null)
    }
    fun onDescriptionChange(value: String) = updateInput { copy(description = value) }

    fun onSaveClick() = save(deleteOutOfRangeItems = false, clearCompletion = false)

    fun onCompletionResetConfirm() {
        _uiState.update { it.copy(isCompletionResetConfirmationVisible = false) }
        save(deleteOutOfRangeItems = false, clearCompletion = true)
    }

    fun onCompletionResetDismiss() = _uiState.update {
        it.copy(isCompletionResetConfirmationVisible = false)
    }

    fun onOutOfRangeConfirm() {
        _uiState.update { it.copy(outOfRangeItemCount = null) }
        save(
            deleteOutOfRangeItems = true,
            clearCompletion = _uiState.value.clearCompletionAfterRangeConfirmation,
        )
    }

    fun onOutOfRangeDismiss() = _uiState.update { it.copy(outOfRangeItemCount = null) }
    fun onDeleteRequest() = _uiState.update { it.copy(isDeleteConfirmationVisible = true) }
    fun onDeleteDismiss() = _uiState.update { it.copy(isDeleteConfirmationVisible = false) }

    fun onDeleteConfirm() {
        val tripPlanId = _uiState.value.tripPlanId
        viewModelScope.launch {
            _uiState.update {
                it.copy(isSaving = true, isDeleteConfirmationVisible = false, isSaveFailed = false)
            }
            try {
                when (deleteTripPlan(tripPlanId)) {
                    DeleteTripPlanResult.Success -> {
                        _uiState.update { it.copy(isSaving = false) }
                        eventChannel.send(EditTripEvent.Deleted)
                    }
                    DeleteTripPlanResult.NotFound -> _uiState.update {
                        it.copy(isSaving = false, isNotFound = true)
                    }
                }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                _uiState.update { it.copy(isSaving = false, isSaveFailed = true) }
            }
        }
    }

    private fun save(
        deleteOutOfRangeItems: Boolean,
        clearCompletion: Boolean,
    ) {
        val state = _uiState.value
        if (state.isSaving) return
        val startDate = state.startDate
        val endDate = state.endDate
        val dateError = when {
            startDate == null || endDate == null -> EditTripDateError.MissingRange
            endDate.isBefore(startDate) -> EditTripDateError.InvalidRange
            else -> null
        }
        if (state.title.isBlank() || dateError != null) {
            _uiState.update {
                it.copy(isTitleError = state.title.isBlank(), dateError = dateError)
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, isSaveFailed = false) }
            try {
                val result = updateTripPlan(
                    tripPlanId = state.tripPlanId,
                    input = TripPlanInput(
                        title = state.title,
                        destination = state.destination,
                        startDate = checkNotNull(startDate),
                        endDate = checkNotNull(endDate),
                        description = state.description,
                    ),
                    deleteOutOfRangeItems = deleteOutOfRangeItems,
                    clearCompletion = clearCompletion,
                )
                when (result) {
                    is UpdateTripPlanResult.Success -> {
                        _uiState.update { it.copy(isSaving = false) }
                        eventChannel.send(EditTripEvent.Saved)
                    }
                    is UpdateTripPlanResult.Invalid -> _uiState.update {
                        it.copy(
                            isSaving = false,
                            isTitleError = TripPlanValidationError.BlankTitle in result.errors,
                            dateError = if (TripPlanValidationError.InvalidDateRange in result.errors) {
                                EditTripDateError.InvalidRange
                            } else null,
                        )
                    }
                    is UpdateTripPlanResult.RequiresOutOfRangeConfirmation -> _uiState.update {
                        it.copy(
                            isSaving = false,
                            outOfRangeItemCount = result.itemCount,
                            clearCompletionAfterRangeConfirmation = clearCompletion,
                        )
                    }
                    UpdateTripPlanResult.RequiresCompletionResetConfirmation -> _uiState.update {
                        it.copy(
                            isSaving = false,
                            isCompletionResetConfirmationVisible = true,
                        )
                    }
                    UpdateTripPlanResult.NotFound -> _uiState.update {
                        it.copy(isSaving = false, isNotFound = true)
                    }
                }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                _uiState.update { it.copy(isSaving = false, isSaveFailed = true) }
            }
        }
    }

    private fun updateInput(transform: EditTripUiState.() -> EditTripUiState) {
        _uiState.update { it.transform().copy(isSaveFailed = false) }
    }
}

data class EditTripUiState(
    val isLoading: Boolean = true,
    val isNotFound: Boolean = false,
    val isLoadFailed: Boolean = false,
    val tripPlanId: Long = 0L,
    val title: String = "",
    val destination: String = "",
    val startDate: LocalDate? = null,
    val endDate: LocalDate? = null,
    val description: String = "",
    val isTitleError: Boolean = false,
    val dateError: EditTripDateError? = null,
    val isSaving: Boolean = false,
    val isSaveFailed: Boolean = false,
    val outOfRangeItemCount: Int? = null,
    val isCompletionResetConfirmationVisible: Boolean = false,
    val clearCompletionAfterRangeConfirmation: Boolean = false,
    val isDeleteConfirmationVisible: Boolean = false,
)

enum class EditTripDateError { MissingRange, InvalidRange }

sealed interface EditTripEvent {
    data object Saved : EditTripEvent
    data object Deleted : EditTripEvent
}
