package com.example.tripplan.presentation.create

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tripplan.domain.model.Region
import com.example.tripplan.domain.model.TripPlanInput
import com.example.tripplan.domain.model.CountryCurrencyCatalog
import com.example.tripplan.domain.rule.TripPlanValidationError
import com.example.tripplan.domain.usecase.CreateTripPlanResult
import com.example.tripplan.domain.usecase.CreateTripPlanUseCase
import com.example.tripplan.domain.usecase.SearchRegionsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import java.time.LocalDate
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

@HiltViewModel
class CreateTripViewModel @Inject constructor(
    private val searchRegionsUseCase: SearchRegionsUseCase,
    private val createTripPlanUseCase: CreateTripPlanUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(CreateTripUiState())
    val uiState: StateFlow<CreateTripUiState> = _uiState.asStateFlow()

    private val eventChannel = Channel<CreateTripEvent>(Channel.BUFFERED)
    val events = eventChannel.receiveAsFlow()

    private var regionSearchJob: Job? = null

    fun onTripTitleChange(value: String) {
        _uiState.update {
            it.copy(
                tripTitle = value,
                isTitleError = false,
                isSaveFailed = false,
            )
        }
    }

    fun onTripPlaceChange(value: String) {
        _uiState.update {
            it.copy(
                tripPlace = value,
                selectedRegion = null,
                isRegionSearchExpanded = value.trim().length >= MIN_REGION_QUERY_LENGTH,
                regionSearchResults = emptyList(),
                isSaveFailed = false,
            )
        }
        searchRegions(value)
    }

    fun onCountrySelected(countryCode: String) {
        val country = CountryCurrencyCatalog.find(countryCode) ?: return
        _uiState.update {
            it.copy(
                countryCode = country.countryCode,
                countryName = country.countryName,
                currencyCode = country.currencyCode,
                isSaveFailed = false,
            )
        }
    }

    fun onRegionClick(region: RegionSearchResultUiModel) {
        _uiState.update {
            it.copy(
                tripPlace = region.displayName,
                selectedRegion = region,
                regionSearchResults = emptyList(),
                isRegionSearchExpanded = false,
            )
        }
    }

    fun onRegionSearchDismiss() {
        _uiState.update { it.copy(isRegionSearchExpanded = false) }
    }

    fun onTripDateRangeChange(
        startDate: LocalDate?,
        endDate: LocalDate?,
    ) {
        _uiState.update {
            it.copy(
                startDate = startDate,
                endDate = endDate,
                dateError = null,
                isSaveFailed = false,
            )
        }
    }

    fun onDescriptionChange(value: String) {
        _uiState.update {
            it.copy(
                description = value,
                isSaveFailed = false,
            )
        }
    }

    fun onSaveClick() {
        val state = _uiState.value
        if (state.isSaving) return

        val startDate = state.startDate
        val endDate = state.endDate
        val isTitleError = state.tripTitle.isBlank()
        val dateError = when {
            startDate == null || endDate == null -> CreateTripDateError.MissingRange
            endDate.isBefore(startDate) -> CreateTripDateError.InvalidRange
            else -> null
        }

        if (isTitleError || dateError != null) {
            _uiState.update {
                it.copy(
                    isTitleError = isTitleError,
                    dateError = dateError,
                    isSaveFailed = false,
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, isSaveFailed = false) }
            try {
                when (
                    val result = createTripPlanUseCase(
                        TripPlanInput(
                            title = state.tripTitle,
                            destination = state.tripPlace,
                            startDate = checkNotNull(startDate),
                            endDate = checkNotNull(endDate),
                            description = state.description,
                            countryCode = state.countryCode,
                            currencyCode = state.currencyCode,
                        )
                    )
                ) {
                    is CreateTripPlanResult.Success -> {
                        _uiState.update { it.copy(isSaving = false) }
                        eventChannel.send(CreateTripEvent.TripCreated(result.tripPlanId))
                    }

                    is CreateTripPlanResult.Invalid -> {
                        _uiState.update {
                            it.copy(
                                isSaving = false,
                                isTitleError = TripPlanValidationError.BlankTitle in result.errors,
                                dateError = if (
                                    TripPlanValidationError.InvalidDateRange in result.errors
                                ) {
                                    CreateTripDateError.InvalidRange
                                } else {
                                    null
                                },
                            )
                        }
                    }
                }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        isSaveFailed = true,
                    )
                }
            }
        }
    }

    private fun searchRegions(query: String) {
        regionSearchJob?.cancel()
        val normalizedQuery = query.trim()
        if (normalizedQuery.length < MIN_REGION_QUERY_LENGTH) return

        regionSearchJob = viewModelScope.launch {
            val results = searchRegionsUseCase(normalizedQuery)
                .map { it.toUiModel() }
            _uiState.update {
                it.copy(
                    regionSearchResults = results,
                    isRegionSearchExpanded = results.isNotEmpty(),
                )
            }
        }
    }

    private companion object {
        const val MIN_REGION_QUERY_LENGTH = 2
    }
}

data class CreateTripUiState(
    val tripTitle: String = "",
    val tripPlace: String = "",
    val selectedRegion: RegionSearchResultUiModel? = null,
    val regionSearchResults: List<RegionSearchResultUiModel> = emptyList(),
    val isRegionSearchExpanded: Boolean = false,
    val startDate: LocalDate? = null,
    val endDate: LocalDate? = null,
    val description: String = "",
    val countryCode: String = CountryCurrencyCatalog.default.countryCode,
    val countryName: String = CountryCurrencyCatalog.default.countryName,
    val currencyCode: String = CountryCurrencyCatalog.default.currencyCode,
    val isTitleError: Boolean = false,
    val dateError: CreateTripDateError? = null,
    val isSaving: Boolean = false,
    val isSaveFailed: Boolean = false,
)

enum class CreateTripDateError {
    MissingRange,
    InvalidRange,
}

sealed interface CreateTripEvent {
    data class TripCreated(val tripPlanId: Long) : CreateTripEvent
}

data class RegionSearchResultUiModel(
    val id: String,
    val displayName: String,
    val detailText: String,
)

private fun Region.toUiModel(): RegionSearchResultUiModel {
    val displayName = listOf(provinceName, cityName, districtName)
        .filter { it.isNotBlank() }
        .distinct()
        .joinToString(" ")
        .ifBlank { placeName }

    val detailText = placeName
        .takeIf { it.isNotBlank() && it != districtName && it != cityName }
        .orEmpty()

    return RegionSearchResultUiModel(
        id = id,
        displayName = displayName,
        detailText = detailText,
    )
}
