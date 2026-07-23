package com.example.tripplan.presentation.create

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tripplan.domain.model.Region
import com.example.tripplan.domain.usecase.SearchRegionsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class CreateTripViewModel @Inject constructor(
    private val searchRegionsUseCase: SearchRegionsUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(CreateTripUiState())
    val uiState: StateFlow<CreateTripUiState> = _uiState.asStateFlow()

    private var regionSearchJob: Job? = null

    fun onTripTitleChange(value: String) {
        _uiState.update { it.copy(tripTitle = value) }
    }

    fun onTripPlaceChange(value: String) {
        _uiState.update {
            it.copy(
                tripPlace = value,
                selectedRegion = null,
                isRegionSearchExpanded = value.trim().length >= MIN_REGION_QUERY_LENGTH,
                regionSearchResults = emptyList(),
            )
        }
        searchRegions(value)
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
)

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
