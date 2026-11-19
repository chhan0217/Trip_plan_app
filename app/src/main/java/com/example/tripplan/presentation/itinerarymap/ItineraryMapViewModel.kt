package com.example.tripplan.presentation.itinerarymap

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tripplan.domain.model.ItineraryMapData
import com.example.tripplan.domain.model.ItineraryMapItem
import com.example.tripplan.domain.model.TripDay
import com.example.tripplan.domain.usecase.ObserveItineraryMapUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class ItineraryMapViewModel @Inject constructor(
    private val observeItineraryMap: ObserveItineraryMapUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ItineraryMapUiState())
    val uiState: StateFlow<ItineraryMapUiState> = _uiState.asStateFlow()

    private var source: ItineraryMapData? = null
    private var loadJob: Job? = null
    private var loadedTripId: Long? = null

    fun load(tripPlanId: Long) {
        if (loadedTripId == tripPlanId) return
        loadedTripId = tripPlanId
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.value = ItineraryMapUiState(isLoading = true)
            try {
                observeItineraryMap(tripPlanId).collect { data ->
                    source = data
                    _uiState.value = data?.toUiState(
                        selectedDate = _uiState.value.selectedDate,
                        selectedItemId = _uiState.value.selectedItemId,
                    ) ?: ItineraryMapUiState(isNotFound = true)
                }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                _uiState.value = ItineraryMapUiState(isLoadFailed = true)
            }
        }
    }

    fun selectDay(date: LocalDate?) {
        _uiState.value = source?.toUiState(date, null) ?: return
    }

    fun selectItem(itemId: Long?) {
        val current = _uiState.value
        if (itemId != null && current.items.none { it.id == itemId }) return
        _uiState.value = current.copy(selectedItemId = itemId)
    }
}

data class ItineraryMapUiState(
    val isLoading: Boolean = false,
    val isNotFound: Boolean = false,
    val isLoadFailed: Boolean = false,
    val tripTitle: String = "",
    val days: List<MapDayUiModel> = emptyList(),
    val selectedDate: LocalDate? = null,
    val items: List<MapItemUiModel> = emptyList(),
    val selectedItemId: Long? = null,
) {
    val selectedItem: MapItemUiModel? get() = items.firstOrNull { it.id == selectedItemId }
    val markers: List<MapItemUiModel> get() = items.filter { it.hasCoordinates }
    val lineSegments: List<List<MapItemUiModel>> get() = buildList {
        val dates = items.map { it.date }.distinct()
        dates.forEach { date ->
            var current = mutableListOf<MapItemUiModel>()
            items.filter { it.date == date }.forEach { item ->
                if (item.hasCoordinates) {
                    current += item
                } else {
                    if (current.size >= 2) add(current)
                    current = mutableListOf()
                }
            }
            if (current.size >= 2) add(current)
        }
    }
}

data class MapDayUiModel(val date: LocalDate, val dayNumber: Int, val dateText: String)

data class MapItemUiModel(
    val id: Long,
    val date: LocalDate,
    val dayNumber: Int,
    val markerLabel: String,
    val title: String,
    val placeName: String,
    val address: String,
    val startTime: LocalTime?,
    val latitude: Double?,
    val longitude: Double?,
) {
    val hasCoordinates: Boolean get() = latitude != null && longitude != null
}

private fun ItineraryMapData.toUiState(
    selectedDate: LocalDate?,
    selectedItemId: Long?,
): ItineraryMapUiState {
    val dayNumberByDate = days.associate { it.date to it.dayNumber }
    val visible = items.filter { selectedDate == null || it.date == selectedDate }
    return ItineraryMapUiState(
        tripTitle = tripTitle,
        days = days.map { MapDayUiModel(it.date, it.dayNumber, it.date.toString().substring(5)) },
        selectedDate = selectedDate,
        items = visible.map { it.toUiModel(dayNumberByDate.getValue(it.date), selectedDate == null) },
        selectedItemId = selectedItemId?.takeIf { id -> visible.any { it.id == id } },
    )
}

private fun ItineraryMapItem.toUiModel(dayNumber: Int, isAllDays: Boolean) = MapItemUiModel(
    id = id,
    date = date,
    dayNumber = dayNumber,
    markerLabel = if (isAllDays) "$dayNumber-$orderInDay" else orderInDay.toString(),
    title = title,
    placeName = placeName,
    address = address,
    startTime = startTime,
    latitude = latitude,
    longitude = longitude,
)
