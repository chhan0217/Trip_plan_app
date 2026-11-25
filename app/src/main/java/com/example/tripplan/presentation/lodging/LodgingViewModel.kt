package com.example.tripplan.presentation.lodging

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tripplan.domain.model.Lodging
import com.example.tripplan.domain.repository.LodgingSaveResult
import com.example.tripplan.domain.usecase.GetExternalPlaceDetailsUseCase
import com.example.tripplan.domain.usecase.ManageLodgingsUseCase
import com.example.tripplan.domain.usecase.ObservePlacesUseCase
import com.example.tripplan.domain.usecase.SearchExternalPlacesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class LodgingViewModel @Inject constructor(
    private val manageLodgings: ManageLodgingsUseCase,
    private val observePlaces: ObservePlacesUseCase,
    private val searchExternalPlaces: SearchExternalPlacesUseCase,
    private val getExternalPlaceDetails: GetExternalPlaceDetailsUseCase,
) : ViewModel() {
    private val state = MutableStateFlow(LodgingUiState())
    val uiState = state.asStateFlow()
    private var observation: Job? = null
    private var tripId: Long? = null
    private var searchJob: Job? = null

    fun load(id: Long, editable: Boolean) {
        if (tripId == id) return
        tripId = id
        observation?.cancel()
        state.value = LodgingUiState()
        observation = viewModelScope.launch {
            if (editable) launch {
                observePlaces().collect { places ->
                    val savedPlaces = places.map { place ->
                        LodgingPlaceUiModel(
                            name = place.name,
                            address = place.address,
                            latitude = place.latitude,
                            longitude = place.longitude,
                            googlePlaceId = place.externalId.takeIf { place.providerName == "Google Places" },
                        )
                    }
                    state.update { it.copy(savedPlaces = savedPlaces) }
                }
            }
            try {
                manageLodgings.observe(id).collect { lodgings ->
                    state.update { it.copy(lodgings = lodgings.map(Lodging::toUiModel)) }
                }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                state.update { it.copy(error = "숙소를 불러오지 못했습니다.") }
            }
        }
    }

    fun beginEdit() {
        clearSearch()
        state.update { it.copy(error = null) }
    }

    fun clearSearch() {
        searchJob?.cancel()
        state.update { it.copy(isSearching = false, searchCompleted = false, suggestions = emptyList()) }
    }

    fun search(countryCode: String, query: String) {
        if (query.trim().length < 2) return
        clearSearch()
        state.update { it.copy(isSearching = true) }
        searchJob = viewModelScope.launch {
            val suggestions = try {
                searchExternalPlaces(countryCode, query).map {
                    LodgingPlaceUiModel(it.name, it.addressHint, googlePlaceId = it.externalId)
                }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                emptyList()
            }
            state.update { it.copy(isSearching = false, searchCompleted = true, suggestions = suggestions) }
        }
    }

    fun save(lodging: LodgingUiModel, resolvePlace: Boolean, onSaved: () -> Unit) = mutate {
        val place = if (resolvePlace) {
            val id = lodging.googlePlaceId ?: return@mutate "숙소 위치를 확인하지 못했습니다."
            getExternalPlaceDetails(id)
        } else {
            null
        }
        val resolved = if (place == null) lodging else lodging.copy(
            address = place.address.ifBlank { lodging.address },
            latitude = place.latitude,
            longitude = place.longitude,
            googlePlaceId = place.externalId,
        )
        when (manageLodgings.save(it, resolved.toDomain())) {
            LodgingSaveResult.Saved -> { onSaved(); null }
            LodgingSaveResult.InvalidDates -> "여행 기간 안에서 체크아웃은 체크인 다음 날부터 선택해 주세요."
            LodgingSaveResult.Overlap -> "다른 숙소와 같은 날 숙박할 수 없습니다."
            LodgingSaveResult.Unavailable -> "숙소를 저장하지 못했습니다."
        }
    }

    fun remove(id: Long, onRemoved: () -> Unit) = mutate {
        if (manageLodgings.remove(it, id)) {
            onRemoved()
            null
        } else {
            "숙소를 삭제하지 못했습니다."
        }
    }

    private fun mutate(operation: suspend (Long) -> String?) {
        val id = tripId ?: return
        if (state.value.isSaving) return
        state.update { it.copy(isSaving = true, error = null) }
        viewModelScope.launch {
            val error = try {
                operation(id)
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                "숙소 변경을 저장하지 못했습니다."
            }
            state.update { it.copy(isSaving = false, error = error) }
        }
    }
}

data class LodgingUiState(
    val lodgings: List<LodgingUiModel> = emptyList(),
    val savedPlaces: List<LodgingPlaceUiModel> = emptyList(),
    val isSaving: Boolean = false,
    val error: String? = null,
    val suggestions: List<LodgingPlaceUiModel> = emptyList(),
    val isSearching: Boolean = false,
    val searchCompleted: Boolean = false,
)

data class LodgingPlaceUiModel(
    val name: String,
    val address: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val googlePlaceId: String? = null,
)

data class LodgingUiModel(
    val id: Long,
    val name: String,
    val checkIn: LocalDate,
    val checkOut: LocalDate,
    val nights: Long,
    val address: String = "",
    val checkInTime: LocalTime? = null,
    val note: String = "",
    val checkOutTime: LocalTime? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val googlePlaceId: String? = null,
)

private fun Lodging.toUiModel() = LodgingUiModel(
    id, name, checkIn, checkOut, nights, address, checkInTime, note,
    checkOutTime, latitude, longitude, googlePlaceId,
)

private fun LodgingUiModel.toDomain() = Lodging(
    id, name, checkIn, checkOut, address, checkInTime, note,
    checkOutTime, latitude, longitude, googlePlaceId,
)
