package com.example.tripplan.presentation.planning

import java.time.LocalDate

data class PlaceCandidateListUiState(
    val items: List<PlaceCandidateUiModel> = emptyList(),
    val savedPlaces: List<CandidateSavedPlaceUiModel> = emptyList(),
    val suggestions: List<CandidateSuggestionUiModel> = emptyList(),
    val isBusy: Boolean = false,
    val isSearching: Boolean = false,
    val searchPerformed: Boolean = false,
    val error: String? = null,
)

data class PlaceCandidateUiModel(
    val id: Long,
    val name: String,
    val address: String,
    val note: String,
    val scheduledDate: LocalDate?,
)

data class CandidateSavedPlaceUiModel(val id: Long, val name: String, val address: String)
data class CandidateSuggestionUiModel(val id: String, val name: String, val address: String)
