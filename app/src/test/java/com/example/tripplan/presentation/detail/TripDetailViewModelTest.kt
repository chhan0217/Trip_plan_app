package com.example.tripplan.presentation.detail

import com.example.tripplan.domain.model.ItineraryItem
import com.example.tripplan.domain.model.BudgetEntry
import com.example.tripplan.domain.model.BudgetEntryType
import com.example.tripplan.domain.model.TripBudget
import com.example.tripplan.domain.model.TripPlan
import com.example.tripplan.domain.model.ExchangeRate
import com.example.tripplan.domain.model.ExchangeRateRefreshResult
import com.example.tripplan.domain.model.ExchangeRateSnapshot
import com.example.tripplan.domain.model.ExpenseCategory
import com.example.tripplan.domain.model.Expense
import com.example.tripplan.domain.model.TripMoneyDisplayCurrency
import com.example.tripplan.domain.repository.ExchangeRateRepository
import com.example.tripplan.domain.usecase.ObserveTripPlanDetailsUseCase
import com.example.tripplan.domain.usecase.CompleteTripPlanUseCase
import com.example.tripplan.domain.usecase.GetTripStatusUseCase
import com.example.tripplan.domain.usecase.UndoTripCompletionUseCase
import com.example.tripplan.domain.usecase.MoveItineraryItemUseCase
import com.example.tripplan.domain.usecase.AddTripBudgetEntryUseCase
import com.example.tripplan.domain.usecase.CreateTripBudgetUseCase
import com.example.tripplan.domain.usecase.ObserveTripBudgetUseCase
import com.example.tripplan.domain.usecase.UpdateItineraryCostsUseCase
import com.example.tripplan.domain.usecase.ObserveLatestExchangeRatesUseCase
import com.example.tripplan.domain.usecase.ObserveExpensesUseCase
import com.example.tripplan.domain.usecase.RecordExpenseUseCase
import com.example.tripplan.domain.usecase.UpdateExpenseUseCase
import com.example.tripplan.domain.usecase.DeleteExpenseUseCase
import com.example.tripplan.domain.usecase.ObserveTripMoneyDisplayCurrencyUseCase
import com.example.tripplan.domain.usecase.UpdateTripMoneyDisplayCurrencyUseCase
import com.example.tripplan.testutil.FakeItineraryRepository
import com.example.tripplan.testutil.FakeTripPlanRepository
import com.example.tripplan.testutil.FakeTripBudgetRepository
import com.example.tripplan.testutil.FakeExpenseRepository
import com.example.tripplan.testutil.FakeTripMoneyDisplayPreferenceRepository
import com.example.tripplan.testutil.MainDispatcherRule
import java.time.Instant
import java.time.Clock
import java.time.ZoneOffset
import java.time.LocalDate
import java.time.LocalTime
import java.math.BigDecimal
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TripDetailViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun load_existingTrip_selectsFirstDayAndShowsItsItems() = runTest {
        val tripRepository = FakeTripPlanRepository().apply { setTrips(listOf(sampleTrip)) }
        val itineraryRepository = FakeItineraryRepository().apply {
            setItems(listOf(item(1L, "2026-11-09"), item(2L, "2026-11-10")))
        }
        val viewModel = createViewModel(tripRepository, itineraryRepository)

        viewModel.load(7L)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals("제주 여행", state.title)
        assertEquals(LocalDate.of(2026, 11, 9), state.selectedDate)
        assertEquals(listOf(1L), state.itineraryItems.map { it.id })
        assertEquals("09:00 - 10:00", state.itineraryItems.single().timeText)
    }

    @Test
    fun daySelection_updatesVisibleItems() = runTest {
        val tripRepository = FakeTripPlanRepository().apply { setTrips(listOf(sampleTrip)) }
        val itineraryRepository = FakeItineraryRepository().apply {
            setItems(listOf(item(1L, "2026-11-09"), item(2L, "2026-11-10")))
        }
        val viewModel = createViewModel(tripRepository, itineraryRepository)
        viewModel.load(7L)
        advanceUntilIdle()

        viewModel.onDaySelected(LocalDate.of(2026, 11, 10))

        assertEquals(listOf(2L), viewModel.uiState.value.itineraryItems.map { it.id })
    }

    @Test
    fun moveDown_updatesDisplayedOrder() = runTest {
        val tripRepository = FakeTripPlanRepository().apply { setTrips(listOf(sampleTrip)) }
        val itineraryRepository = FakeItineraryRepository().apply {
            setItems(listOf(item(1L, "2026-11-09"), item(2L, "2026-11-09").copy(order = 1)))
        }
        val viewModel = createViewModel(tripRepository, itineraryRepository)
        viewModel.load(7L)
        advanceUntilIdle()

        viewModel.onMoveDown(1L)
        advanceUntilIdle()

        assertEquals(listOf(2L, 1L), viewModel.uiState.value.itineraryItems.map { it.id })
    }

    @Test
    fun load_missingTrip_exposesNotFoundState() = runTest {
        val itineraryRepository = FakeItineraryRepository()
        val tripRepository = FakeTripPlanRepository()
        val viewModel = createViewModel(tripRepository, itineraryRepository)

        viewModel.load(99L)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertTrue(viewModel.uiState.value.isNotFound)
    }

    @Test
    fun completionConfirm_updatesPersistedStateAndUi() = runTest {
        val tripRepository = FakeTripPlanRepository().apply { setTrips(listOf(sampleTrip)) }
        val itineraryRepository = FakeItineraryRepository()
        val viewModel = createViewModel(tripRepository, itineraryRepository)
        viewModel.load(sampleTrip.id)
        advanceUntilIdle()

        viewModel.onCompletionRequest()
        viewModel.onCompletionConfirm()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isCompleted)
        assertEquals(fixedClock.instant(), tripRepository.getTripPlan(sampleTrip.id)?.completedAt)
    }

    @Test
    fun budgetAndActualCosts_showTripAndSelectedDayTotals() = runTest {
        val tripRepository = FakeTripPlanRepository().apply { setTrips(listOf(sampleTrip)) }
        val itineraryRepository = FakeItineraryRepository().apply {
            setItems(
                listOf(
                    item(1, "2026-11-09"),
                    item(2, "2026-11-10"),
                )
            )
        }
        val expenses = FakeExpenseRepository().apply {
            setExpenses(
                listOf(
                    expense(1, 1, "2026-11-09", "171360"),
                    expense(2, 2, "2026-11-10", "100000"),
                )
            )
        }
        val entry = BudgetEntry(
            1, sampleTrip.id, BudgetEntryType.Initial, BigDecimal("300000"), BigDecimal("9.52"),
            LocalDate.of(2026, 11, 9), BigDecimal("2856000"), "", 0,
            fixedClock.instant(), fixedClock.instant(),
        )
        val budgetRepository = FakeTripBudgetRepository().apply {
            setBudget(TripBudget(sampleTrip.id, "JPY", listOf(entry), fixedClock.instant(), fixedClock.instant()))
        }
        val viewModel = createViewModel(
            tripRepository,
            itineraryRepository,
            budgetRepository,
            expenseRepository = expenses,
        )

        viewModel.load(sampleTrip.id)
        advanceUntilIdle()

        assertEquals("271,360원", viewModel.uiState.value.budget?.actualKrwText)
        assertEquals("171,360원", viewModel.uiState.value.selectedDayActualKrwText)
        assertEquals("2,584,640원", viewModel.uiState.value.budget?.remainingKrwText)
        assertEquals("271,360원", viewModel.uiState.value.expenseSummary.totalKrwText)
        assertEquals("식비", viewModel.uiState.value.expenseSummary.categories.single().categoryText)
        assertEquals(100f, viewModel.uiState.value.expenseSummary.categories.single().percentage)
    }

    @Test
    fun createBudget_acceptsKrwAndAutomaticallyConvertsToTripCurrency() = runTest {
        val japanTrip = sampleTrip.copy(countryCode = "JP", currencyCode = "JPY")
        val tripRepository = FakeTripPlanRepository().apply { setTrips(listOf(japanTrip)) }
        val budgetRepository = FakeTripBudgetRepository()
        val rates = ExchangeRateSnapshot(
            rateDate = LocalDate.of(2026, 11, 8),
            fetchedAt = fixedClock.instant(),
            rates = listOf(ExchangeRate("JPY", "일본 엔", BigDecimal("9.52"))),
        )
        val viewModel = createViewModel(
            tripRepository,
            FakeItineraryRepository(),
            budgetRepository,
            rates,
        )

        viewModel.load(japanTrip.id)
        advanceUntilIdle()
        viewModel.createBudget("1,000,000")
        advanceUntilIdle()

        val entry = budgetRepository.getBudget(japanTrip.id)!!.entries.single()
        assertEquals(BigDecimal("105042.02"), entry.amount)
        assertEquals(BigDecimal("1000000"), entry.convertedKrwAmount)
        assertEquals(BigDecimal("9.52"), entry.krwPerUnit)
        assertEquals(LocalDate.of(2026, 11, 8), entry.rateDate)
    }

    @Test
    fun recordExpense_betweenSchedules_createsStandaloneTimelineItemAndUpdatesTotal() = runTest {
        val tripRepository = FakeTripPlanRepository().apply { setTrips(listOf(sampleTrip)) }
        val expenses = FakeExpenseRepository()
        val viewModel = createViewModel(
            tripRepository,
            FakeItineraryRepository().apply {
                setItems(listOf(item(1, "2026-11-09")))
            },
            expenseRepository = expenses,
        )

        viewModel.load(sampleTrip.id)
        advanceUntilIdle()
        viewModel.onExpenseRequest()
        viewModel.recordExpense("12000", ExpenseCategory.Transportation, "KRW")
        advanceUntilIdle()

        assertEquals(null, expenses.values().single().itineraryItemId)
        assertEquals("교통", viewModel.uiState.value.standaloneExpenses.single().categoryText)
        assertEquals("12,000원", viewModel.uiState.value.selectedDayActualKrwText)
    }

    @Test
    fun recordExpense_fromScheduleCard_attachesExpenseToThatSchedule() = runTest {
        val tripRepository = FakeTripPlanRepository().apply { setTrips(listOf(sampleTrip)) }
        val itineraryRepository = FakeItineraryRepository().apply {
            setItems(
                listOf(
                    item(1, "2026-11-09")
                )
            )
        }
        val expenses = FakeExpenseRepository()
        val viewModel = createViewModel(
            tripRepository,
            itineraryRepository,
            expenseRepository = expenses,
        )

        viewModel.load(sampleTrip.id)
        advanceUntilIdle()
        viewModel.onItineraryExpenseRequest(1)
        viewModel.recordExpense("8000", ExpenseCategory.Food, "KRW")
        advanceUntilIdle()

        assertEquals(1L, expenses.values().single().itineraryItemId)
        assertEquals("식비", viewModel.uiState.value.itineraryItems.single().expenses.single().categoryText)
        assertTrue(viewModel.uiState.value.standaloneExpenses.isEmpty())
    }

    @Test
    fun updateExpense_recalculatesAmountAndPlacementUsingOriginalRate() = runTest {
        val japanTrip = sampleTrip.copy(countryCode = "JP", currencyCode = "JPY")
        val tripRepository = FakeTripPlanRepository().apply { setTrips(listOf(japanTrip)) }
        val itineraryRepository = FakeItineraryRepository().apply {
            setItems(listOf(item(1, "2026-11-09")))
        }
        val expenses = FakeExpenseRepository().apply {
            setExpenses(listOf(expense(8, 1, "2026-11-09", "23800")))
        }
        val viewModel = createViewModel(
            tripRepository,
            itineraryRepository,
            expenseRepository = expenses,
        )
        viewModel.load(japanTrip.id)
        advanceUntilIdle()

        viewModel.onExpenseEditRequest(8)
        viewModel.updateExpense("3,000", ExpenseCategory.Transportation, "JPY", "11:30")
        advanceUntilIdle()

        val updated = expenses.values().single()
        assertEquals(BigDecimal("28560"), updated.convertedKrwAmount)
        assertEquals(BigDecimal("9.52"), updated.krwPerUnit)
        assertEquals(ExpenseCategory.Transportation, updated.category)
        assertEquals(null, updated.itineraryItemId)
        assertEquals(null, viewModel.uiState.value.editingExpense)
    }

    @Test
    fun deleteExpense_removesExpenseAndClosesEditor() = runTest {
        val tripRepository = FakeTripPlanRepository().apply { setTrips(listOf(sampleTrip)) }
        val expenses = FakeExpenseRepository().apply {
            setExpenses(listOf(expense(8, null, "2026-11-09", "23800")))
        }
        val viewModel = createViewModel(
            tripRepository,
            FakeItineraryRepository(),
            expenseRepository = expenses,
        )
        viewModel.load(sampleTrip.id)
        advanceUntilIdle()

        viewModel.onExpenseEditRequest(8)
        viewModel.onExpenseDeleteRequest()
        viewModel.deleteExpense()
        advanceUntilIdle()

        assertTrue(expenses.values().isEmpty())
        assertEquals(null, viewModel.uiState.value.editingExpense)
        assertFalse(viewModel.uiState.value.isExpenseDeleteConfirmationVisible)
    }

    @Test
    fun ongoingTrip_defaultsToLocalCurrencyAndPersistsUserChange() = runTest {
        val japanTrip = sampleTrip.copy(countryCode = "JP", currencyCode = "JPY")
        val tripRepository = FakeTripPlanRepository().apply { setTrips(listOf(japanTrip)) }
        val preferences = FakeTripMoneyDisplayPreferenceRepository()
        val viewModel = createViewModel(
            tripRepository,
            FakeItineraryRepository(),
            moneyDisplayPreferences = preferences,
        )

        viewModel.load(japanTrip.id)
        advanceUntilIdle()
        assertEquals(TripMoneyDisplayCurrency.Local, viewModel.uiState.value.moneyDisplayCurrency)

        viewModel.setMoneyDisplayCurrency(TripMoneyDisplayCurrency.KRW)
        advanceUntilIdle()
        assertEquals(TripMoneyDisplayCurrency.KRW, viewModel.uiState.value.moneyDisplayCurrency)
    }

    @Test
    fun upcomingTrip_defaultsToKrwCurrency() = runTest {
        val upcoming = sampleTrip.copy(
            countryCode = "JP",
            currencyCode = "JPY",
            startDate = LocalDate.of(2026, 11, 10),
            endDate = LocalDate.of(2026, 11, 12),
        )
        val tripRepository = FakeTripPlanRepository().apply { setTrips(listOf(upcoming)) }
        val viewModel = createViewModel(tripRepository, FakeItineraryRepository())

        viewModel.load(upcoming.id)
        advanceUntilIdle()

        assertEquals(TripMoneyDisplayCurrency.KRW, viewModel.uiState.value.moneyDisplayCurrency)
    }

    private val sampleTrip = TripPlan(
        id = 7L,
        title = "제주 여행",
        destination = "제주",
        startDate = LocalDate.of(2026, 11, 9),
        endDate = LocalDate.of(2026, 11, 10),
        description = "친구와 여행",
        createdAt = Instant.EPOCH,
        updatedAt = Instant.EPOCH,
    )

    private val fixedClock = Clock.fixed(
        Instant.parse("2026-11-09T01:00:00Z"),
        ZoneOffset.UTC,
    )

    private fun createViewModel(
        tripRepository: FakeTripPlanRepository,
        itineraryRepository: FakeItineraryRepository,
        budgetRepository: FakeTripBudgetRepository = FakeTripBudgetRepository(),
        exchangeRates: ExchangeRateSnapshot? = null,
        expenseRepository: FakeExpenseRepository = FakeExpenseRepository(),
        moneyDisplayPreferences: FakeTripMoneyDisplayPreferenceRepository =
            FakeTripMoneyDisplayPreferenceRepository(),
    ) = TripDetailViewModel(
        ObserveTripPlanDetailsUseCase(tripRepository, itineraryRepository),
        MoveItineraryItemUseCase(itineraryRepository),
        GetTripStatusUseCase(fixedClock),
        CompleteTripPlanUseCase(tripRepository, fixedClock),
        UndoTripCompletionUseCase(tripRepository, fixedClock),
        ObserveTripBudgetUseCase(budgetRepository),
        CreateTripBudgetUseCase(budgetRepository, fixedClock),
        AddTripBudgetEntryUseCase(budgetRepository, fixedClock),
        UpdateItineraryCostsUseCase(itineraryRepository, budgetRepository, fixedClock),
        ObserveLatestExchangeRatesUseCase(FakeExchangeRateRepository(exchangeRates)),
        ObserveExpensesUseCase(expenseRepository),
        RecordExpenseUseCase(expenseRepository, fixedClock),
        UpdateExpenseUseCase(expenseRepository),
        DeleteExpenseUseCase(expenseRepository),
        ObserveTripMoneyDisplayCurrencyUseCase(moneyDisplayPreferences),
        UpdateTripMoneyDisplayCurrencyUseCase(moneyDisplayPreferences),
        fixedClock,
    )

    private class FakeExchangeRateRepository(
        snapshot: ExchangeRateSnapshot?,
    ) : ExchangeRateRepository {
        private val rates = MutableStateFlow(snapshot)
        override fun observeLatestRates(): Flow<ExchangeRateSnapshot?> = rates
        override suspend fun refreshLatestRates(today: LocalDate): ExchangeRateRefreshResult =
            ExchangeRateRefreshResult.KeptCached
    }

    private fun item(id: Long, date: String) = ItineraryItem(
        id = id,
        tripPlanId = 7L,
        date = LocalDate.parse(date),
        title = "일정 $id",
        place = "제주",
        startTime = LocalTime.of(9, 0),
        endTime = LocalTime.of(10, 0),
        order = 0,
    )

    private fun expense(
        id: Long,
        itineraryItemId: Long?,
        date: String,
        convertedKrwAmount: String,
        category: ExpenseCategory = ExpenseCategory.Food,
    ) = Expense(
        id = id,
        tripPlanId = sampleTrip.id,
        itineraryItemId = itineraryItemId,
        occurredDate = LocalDate.parse(date),
        occurredTime = LocalTime.of(9, 30),
        amount = BigDecimal(convertedKrwAmount).divide(BigDecimal("9.52"), 2, java.math.RoundingMode.HALF_UP),
        currencyCode = "JPY",
        krwPerUnit = BigDecimal("9.52"),
        rateDate = LocalDate.parse(date),
        convertedKrwAmount = BigDecimal(convertedKrwAmount),
        category = category,
        createdAt = fixedClock.instant(),
    )
}
