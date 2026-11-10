package com.example.tripplan.presentation.detail

import com.example.tripplan.domain.model.ItineraryItem
import com.example.tripplan.domain.model.TripPlan
import com.example.tripplan.domain.usecase.ObserveTripPlanDetailsUseCase
import com.example.tripplan.domain.usecase.CompleteTripPlanUseCase
import com.example.tripplan.domain.usecase.GetTripStatusUseCase
import com.example.tripplan.domain.usecase.UndoTripCompletionUseCase
import com.example.tripplan.domain.usecase.MoveItineraryItemUseCase
import com.example.tripplan.testutil.FakeItineraryRepository
import com.example.tripplan.testutil.FakeTripPlanRepository
import com.example.tripplan.testutil.MainDispatcherRule
import java.time.Instant
import java.time.Clock
import java.time.ZoneOffset
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
        val viewModel = TripDetailViewModel(
            ObserveTripPlanDetailsUseCase(tripRepository, itineraryRepository),
            MoveItineraryItemUseCase(itineraryRepository),
            GetTripStatusUseCase(fixedClock),
            CompleteTripPlanUseCase(tripRepository, fixedClock),
            UndoTripCompletionUseCase(tripRepository, fixedClock),
        )

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
        val viewModel = TripDetailViewModel(
            ObserveTripPlanDetailsUseCase(tripRepository, itineraryRepository),
            MoveItineraryItemUseCase(itineraryRepository),
            GetTripStatusUseCase(fixedClock),
            CompleteTripPlanUseCase(tripRepository, fixedClock),
            UndoTripCompletionUseCase(tripRepository, fixedClock),
        )
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
        val viewModel = TripDetailViewModel(
            ObserveTripPlanDetailsUseCase(tripRepository, itineraryRepository),
            MoveItineraryItemUseCase(itineraryRepository),
            GetTripStatusUseCase(fixedClock),
            CompleteTripPlanUseCase(tripRepository, fixedClock),
            UndoTripCompletionUseCase(tripRepository, fixedClock),
        )
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
        val viewModel = TripDetailViewModel(
            ObserveTripPlanDetailsUseCase(
                tripRepository,
                itineraryRepository,
            ),
            MoveItineraryItemUseCase(itineraryRepository),
            GetTripStatusUseCase(fixedClock),
            CompleteTripPlanUseCase(tripRepository, fixedClock),
            UndoTripCompletionUseCase(tripRepository, fixedClock),
        )

        viewModel.load(99L)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertTrue(viewModel.uiState.value.isNotFound)
    }

    @Test
    fun completionConfirm_updatesPersistedStateAndUi() = runTest {
        val tripRepository = FakeTripPlanRepository().apply { setTrips(listOf(sampleTrip)) }
        val itineraryRepository = FakeItineraryRepository()
        val viewModel = TripDetailViewModel(
            ObserveTripPlanDetailsUseCase(tripRepository, itineraryRepository),
            MoveItineraryItemUseCase(itineraryRepository),
            GetTripStatusUseCase(fixedClock),
            CompleteTripPlanUseCase(tripRepository, fixedClock),
            UndoTripCompletionUseCase(tripRepository, fixedClock),
        )
        viewModel.load(sampleTrip.id)
        advanceUntilIdle()

        viewModel.onCompletionRequest()
        viewModel.onCompletionConfirm()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isCompleted)
        assertEquals(fixedClock.instant(), tripRepository.getTripPlan(sampleTrip.id)?.completedAt)
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
}
