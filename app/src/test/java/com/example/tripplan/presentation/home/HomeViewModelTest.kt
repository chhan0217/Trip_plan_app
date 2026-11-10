package com.example.tripplan.presentation.home

import com.example.tripplan.domain.model.TripPlan
import com.example.tripplan.domain.model.TripPlanInput
import com.example.tripplan.domain.model.ExchangeRate
import com.example.tripplan.domain.model.ExchangeRateRefreshResult
import com.example.tripplan.domain.model.ExchangeRateSnapshot
import com.example.tripplan.domain.repository.ExchangeRatePreferenceRepository
import com.example.tripplan.domain.repository.ExchangeRateRepository
import com.example.tripplan.domain.repository.TripPlanRepository
import com.example.tripplan.domain.usecase.GetTripPlansUseCase
import com.example.tripplan.domain.usecase.GetTripStatusUseCase
import com.example.tripplan.domain.usecase.ObserveHomeExchangeRatesUseCase
import com.example.tripplan.domain.usecase.RefreshHomeExchangeRatesUseCase
import com.example.tripplan.domain.usecase.UpdateHomeExchangeRateSelectionUseCase
import com.example.tripplan.testutil.FakeTripPlanRepository
import com.example.tripplan.testutil.MainDispatcherRule
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.math.BigDecimal
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun trips_areGroupedAndSortedAroundToday() = runTest {
        val repository = FakeTripPlanRepository().apply {
            setTrips(
                listOf(
                    trip(1L, "진행 중", "2026-11-08", "2026-11-10"),
                    trip(2L, "다음 여행", "2026-12-01", "2026-12-03"),
                    trip(3L, "가까운 여행", "2026-11-20", "2026-11-21"),
                    trip(4L, "지난 여행", "2026-10-01", "2026-10-03"),
                )
            )
        }

        val viewModel = viewModel(repository)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(listOf("진행 중", "다음 여행", "가까운 여행", "지난 여행"), state.trips.map { it.title })
        assertEquals("여행 2일차", state.trips.first().statusText)
        assertEquals("2026.11.08 - 2026.11.10", state.trips.first().dateText)
    }

    @Test
    fun repositoryFailure_exposesErrorState() = runTest {
        val viewModel = viewModel(FailingRepository)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertTrue(viewModel.uiState.value.isLoadFailed)
    }

    @Test
    fun cachedExchangeRates_areShownAndRefreshedOnStart() = runTest {
        val exchangeRateRepository = FakeExchangeRateRepository(
            snapshot = ExchangeRateSnapshot(
                rateDate = LocalDate.of(2026, 11, 7),
                fetchedAt = Instant.parse("2026-11-07T01:00:00Z"),
                rates = listOf(
                    ExchangeRate("USD", "미국 달러", BigDecimal("1466.80")),
                    ExchangeRate("EUR", "유로", BigDecimal("1692.40")),
                ),
            ),
        )
        val preferences = FakeExchangeRatePreferenceRepository(setOf("USD"))

        val viewModel = viewModel(
            tripRepository = FakeTripPlanRepository(),
            exchangeRateRepository = exchangeRateRepository,
            preferenceRepository = preferences,
        )
        advanceUntilIdle()

        assertEquals(listOf("USD"), viewModel.uiState.value.exchangeRates.map { it.currencyCode })
        assertEquals("2026.11.07 기준", viewModel.uiState.value.exchangeRateDateText)
        assertEquals(listOf(LocalDate.of(2026, 11, 9)), exchangeRateRepository.refreshDates)
    }

    @Test
    fun selectingCurrency_persistsSelection() = runTest {
        val preferences = FakeExchangeRatePreferenceRepository(setOf("USD"))
        val viewModel = viewModel(
            tripRepository = FakeTripPlanRepository(),
            preferenceRepository = preferences,
        )
        advanceUntilIdle()

        viewModel.setCurrencySelected("EUR", true)
        advanceUntilIdle()

        assertEquals(setOf("USD", "EUR"), preferences.selected.value)
    }

    @Test
    fun vietnameseDong_isDisplayedPerThousandUnits() = runTest {
        val exchangeRateRepository = FakeExchangeRateRepository(
            snapshot = ExchangeRateSnapshot(
                rateDate = LocalDate.of(2026, 11, 10),
                fetchedAt = Instant.parse("2026-11-10T01:00:00Z"),
                rates = listOf(
                    ExchangeRate("VND", "베트남 동", BigDecimal("0.05271759186")),
                ),
            ),
        )

        val viewModel = viewModel(
            tripRepository = FakeTripPlanRepository(),
            exchangeRateRepository = exchangeRateRepository,
            preferenceRepository = FakeExchangeRatePreferenceRepository(setOf("VND")),
        )
        advanceUntilIdle()

        val vnd = viewModel.uiState.value.exchangeRates.single()
        assertEquals(1_000, vnd.displayUnit)
        assertEquals("52.7", vnd.displayRateText)
    }

    private fun viewModel(
        tripRepository: TripPlanRepository,
        exchangeRateRepository: FakeExchangeRateRepository = FakeExchangeRateRepository(),
        preferenceRepository: FakeExchangeRatePreferenceRepository = FakeExchangeRatePreferenceRepository(),
    ): HomeViewModel {
        return HomeViewModel(
            getTripPlansUseCase = GetTripPlansUseCase(tripRepository),
            getTripStatus = GetTripStatusUseCase(fixedClock),
            observeHomeExchangeRates = ObserveHomeExchangeRatesUseCase(
                exchangeRateRepository,
                preferenceRepository,
            ),
            refreshHomeExchangeRates = RefreshHomeExchangeRatesUseCase(
                exchangeRateRepository,
                fixedClock,
            ),
            updateHomeExchangeRateSelection = UpdateHomeExchangeRateSelectionUseCase(
                preferenceRepository,
            ),
        )
    }

    private fun trip(id: Long, title: String, start: String, end: String) = TripPlan(
        id = id,
        title = title,
        destination = "제주",
        startDate = LocalDate.parse(start),
        endDate = LocalDate.parse(end),
        description = "",
        createdAt = Instant.EPOCH,
        updatedAt = Instant.EPOCH,
    )

    private companion object {
        val fixedClock: Clock = Clock.fixed(
            Instant.parse("2026-11-09T01:00:00Z"),
            ZoneOffset.UTC,
        )

        object FailingRepository : TripPlanRepository {
            override fun observeTripPlans(): Flow<List<TripPlan>> = flow {
                throw IllegalStateException("database failure")
            }

            override fun observeTripPlan(tripPlanId: Long): Flow<TripPlan?> = flow {
                throw IllegalStateException("database failure")
            }

            override suspend fun getTripPlan(tripPlanId: Long): TripPlan? = null
            override suspend fun createTripPlan(input: TripPlanInput): Long = 0L
            override suspend fun updateTripPlan(tripPlan: TripPlan) = Unit
            override suspend fun updateTripCompletion(
                tripPlanId: Long,
                completedAt: Instant?,
                updatedAt: Instant,
            ): Boolean = false
            override suspend fun updateTripPlanAndDeleteItineraryItems(
                tripPlan: TripPlan,
                itemIds: List<Long>,
            ) = Unit
            override suspend fun deleteTripPlan(tripPlanId: Long) = Unit
        }
    }
}

private class FakeExchangeRateRepository(
    snapshot: ExchangeRateSnapshot? = null,
) : ExchangeRateRepository {
    private val snapshots = MutableStateFlow(snapshot)
    val refreshDates = mutableListOf<LocalDate>()
    var refreshResult: ExchangeRateRefreshResult = ExchangeRateRefreshResult.Updated(
        snapshot?.rateDate ?: LocalDate.of(2026, 11, 9),
    )

    override fun observeLatestRates(): Flow<ExchangeRateSnapshot?> = snapshots

    override suspend fun refreshLatestRates(today: LocalDate): ExchangeRateRefreshResult {
        refreshDates += today
        return refreshResult
    }
}

private class FakeExchangeRatePreferenceRepository(
    initialSelection: Set<String> = setOf("USD", "JPY", "EUR"),
) : ExchangeRatePreferenceRepository {
    val selected = MutableStateFlow(initialSelection)

    override fun observeSelectedCurrencyUnits(): Flow<Set<String>> = selected

    override suspend fun updateSelectedCurrencyUnits(currencyUnits: Set<String>) {
        selected.value = currencyUnits
    }
}
