package com.example.tripplan.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tripplan.domain.model.TripPlan
import com.example.tripplan.domain.model.TripDatePhase
import com.example.tripplan.domain.model.ExchangeRateRefreshResult
import com.example.tripplan.domain.model.HomeExchangeRateData
import com.example.tripplan.domain.usecase.GetTripPlansUseCase
import com.example.tripplan.domain.usecase.GetTripStatusUseCase
import com.example.tripplan.domain.usecase.ObserveHomeExchangeRatesUseCase
import com.example.tripplan.domain.usecase.RefreshHomeExchangeRatesUseCase
import com.example.tripplan.domain.usecase.UpdateHomeExchangeRateSelectionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class HomeViewModel @Inject constructor(
    getTripPlansUseCase: GetTripPlansUseCase,
    private val getTripStatus: GetTripStatusUseCase,
    observeHomeExchangeRates: ObserveHomeExchangeRatesUseCase,
    private val refreshHomeExchangeRates: RefreshHomeExchangeRatesUseCase,
    private val updateHomeExchangeRateSelection: UpdateHomeExchangeRateSelectionUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            getTripPlansUseCase()
                .catch {
                    _uiState.update { state ->
                        state.copy(isLoading = false, isLoadFailed = true)
                    }
                }
                .collect { trips ->
                    val tripState = trips.toHomeUiState(getTripStatus)
                    _uiState.update { current ->
                        current.copy(
                            isLoading = tripState.isLoading,
                            trips = tripState.trips,
                            isLoadFailed = tripState.isLoadFailed,
                        )
                    }
                }
        }
        viewModelScope.launch {
            observeHomeExchangeRates()
                .catch {
                    _uiState.update { state ->
                        state.copy(
                            isExchangeRateRefreshing = false,
                            exchangeRateMessage = "저장된 환율 정보를 불러오지 못했습니다.",
                        )
                    }
                }
                .collect { data ->
                    _uiState.update { current -> current.withExchangeRateData(data) }
                }
        }
        refreshExchangeRates()
    }

    fun refreshExchangeRates() {
        viewModelScope.launch {
            _uiState.update { it.copy(isExchangeRateRefreshing = true, exchangeRateMessage = null) }
            val result = runCatching { refreshHomeExchangeRates() }.getOrElse {
                _uiState.update { current ->
                    current.copy(
                        isExchangeRateRefreshing = false,
                        exchangeRateMessage = "환율 정보를 가져오지 못했습니다.",
                    )
                }
                return@launch
            }
            _uiState.update { current ->
                current.copy(
                    isExchangeRateRefreshing = false,
                    exchangeRateMessage = when (result) {
                        is ExchangeRateRefreshResult.Updated -> null
                        ExchangeRateRefreshResult.KeptCached -> "최신 환율을 가져오지 못해 저장된 환율을 표시합니다."
                        ExchangeRateRefreshResult.Failed -> "환율 정보를 가져오지 못했습니다."
                    },
                )
            }
        }
    }

    fun showCurrencySelector() {
        _uiState.update { it.copy(isCurrencySelectorVisible = true) }
    }

    fun hideCurrencySelector() {
        _uiState.update { it.copy(isCurrencySelectorVisible = false) }
    }

    fun setCurrencySelected(currencyUnit: String, selected: Boolean) {
        val currentSelection = _uiState.value.selectedCurrencyUnits
        val updatedSelection = if (selected) {
            currentSelection + currencyUnit
        } else {
            currentSelection - currencyUnit
        }
        if (updatedSelection.isEmpty()) return
        viewModelScope.launch {
            updateHomeExchangeRateSelection(updatedSelection)
        }
    }
}

data class HomeUiState(
    val isLoading: Boolean = true,
    val trips: List<HomeTripUiModel> = emptyList(),
    val ongoingTrips: List<HomeTripUiModel> = emptyList(),
    val upcomingTrips: List<HomeTripUiModel> = emptyList(),
    val pastTrips: List<HomeTripUiModel> = emptyList(),
    val isLoadFailed: Boolean = false,
    val exchangeRates: List<HomeExchangeRateUiModel> = emptyList(),
    val availableCurrencies: List<HomeExchangeRateUiModel> = emptyList(),
    val selectedCurrencyUnits: Set<String> = emptySet(),
    val exchangeRateDateText: String = "",
    val isExchangeRateRefreshing: Boolean = false,
    val exchangeRateMessage: String? = null,
    val isCurrencySelectorVisible: Boolean = false,
)

data class HomeExchangeRateUiModel(
    val currencyCode: String,
    val currencyName: String,
    val displayUnit: Int,
    val displayRateText: String,
)

data class HomeTripUiModel(
    val id: Long,
    val title: String,
    val dateText: String,
    val placeText: String,
    val statusText: String = "",
)

private fun List<TripPlan>.toHomeUiState(getTripStatus: GetTripStatusUseCase): HomeUiState {
    return HomeUiState(
        isLoading = false,
        trips = sortedByDescending { it.updatedAt }.map { it.toHomeUiModel(getTripStatus) },
    )
}

private fun TripPlan.toHomeUiModel(getTripStatus: GetTripStatusUseCase): HomeTripUiModel = HomeTripUiModel(
    id = id,
    title = title,
    dateText = "${startDate.format(homeDateFormatter)} - ${endDate.format(homeDateFormatter)}",
    placeText = destination,
    statusText = getTripStatus(this).let { status ->
        if (status.isCompleted) "완료" else when (status.dateStatus.phase) {
            TripDatePhase.Upcoming -> "D-${status.dateStatus.daysUntilStart}"
            TripDatePhase.Ongoing -> if (status.dateStatus.currentDayNumber == 1) {
                "D-Day"
            } else {
                "여행 ${status.dateStatus.currentDayNumber}일차"
            }
            TripDatePhase.Ended -> "여행 종료"
        }
    },
)

private val homeDateFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd")

private fun HomeUiState.withExchangeRateData(data: HomeExchangeRateData): HomeUiState {
    val available = data.snapshot?.rates.orEmpty()
        .map { rate ->
            HomeExchangeRateUiModel(
                currencyCode = rate.currencyCode,
                currencyName = rate.currencyName,
                displayUnit = rate.displayUnit(),
                displayRateText = rate.displayRateText(),
            )
        }
        .sortedBy { it.currencyCode }
    return copy(
        availableCurrencies = available,
        selectedCurrencyUnits = data.selectedCurrencyUnits,
        exchangeRates = available.filter { it.currencyCode in data.selectedCurrencyUnits },
        exchangeRateDateText = data.snapshot?.rateDate?.format(homeDateFormatter)?.let { "$it 기준" }.orEmpty(),
    )
}

private fun com.example.tripplan.domain.model.ExchangeRate.displayUnit(): Int {
    return when {
        currencyCode == "JPY" -> 100
        krwPerUnit < BigDecimal("0.1") -> 1_000
        krwPerUnit < BigDecimal.TEN -> 100
        else -> 1
    }
}

private fun com.example.tripplan.domain.model.ExchangeRate.displayRateText(): String {
    val displayRate = krwPerUnit.multiply(displayUnit().toBigDecimal())
    return exchangeRateFormatter.format(displayRate)
}

private val exchangeRateFormatter = DecimalFormat(
    "#,##0.0",
    DecimalFormatSymbols(Locale.KOREA),
)
