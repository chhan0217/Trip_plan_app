package com.example.tripplan.presentation.edit

import com.example.tripplan.domain.model.ItineraryItem
import com.example.tripplan.domain.model.TripPlan
import com.example.tripplan.domain.usecase.DeleteTripPlanUseCase
import com.example.tripplan.domain.usecase.ObserveTripPlanDetailsUseCase
import com.example.tripplan.domain.usecase.UpdateTripPlanUseCase
import com.example.tripplan.testutil.FakeItineraryRepository
import com.example.tripplan.testutil.FakeTripPlanRepository
import com.example.tripplan.testutil.MainDispatcherRule
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EditTripViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun load_existingTrip_populatesForm() = runTest {
        val viewModel = createViewModel()

        viewModel.load(sampleTrip.id)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals("제주 여행", state.title)
        assertEquals(sampleTrip.startDate, state.startDate)
    }

    @Test
    fun save_shrunkPeriod_requestsConfirmationThenSaves() = runTest {
        val trips = tripRepository()
        val items = FakeItineraryRepository().apply {
            setItems(listOf(outOfRangeItem))
        }
        val viewModel = createViewModel(trips, items)
        val event = async(UnconfinedTestDispatcher(testScheduler)) { viewModel.events.first() }
        viewModel.load(sampleTrip.id)
        advanceUntilIdle()
        viewModel.onDateRangeChange(sampleTrip.startDate, LocalDate.of(2026, 11, 10))

        viewModel.onSaveClick()
        advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.outOfRangeItemCount)
        assertEquals(sampleTrip.endDate, trips.getTripPlan(sampleTrip.id)?.endDate)

        viewModel.onOutOfRangeConfirm()
        advanceUntilIdle()

        assertEquals(EditTripEvent.Saved, event.await())
        assertEquals(LocalDate.of(2026, 11, 10), trips.getTripPlan(sampleTrip.id)?.endDate)
        assertEquals(listOf(outOfRangeItem.id), trips.lastDeletedItineraryItemIds)
    }

    @Test
    fun save_completedTripWithCurrentDates_requestsCompletionResetConfirmation() = runTest {
        val trips = FakeTripPlanRepository().apply {
            setTrips(listOf(completedTrip))
        }
        val viewModel = createViewModel(trips)
        viewModel.load(completedTrip.id)
        advanceUntilIdle()
        viewModel.onDateRangeChange(
            LocalDate.of(2026, 11, 10),
            LocalDate.of(2026, 11, 12),
        )

        viewModel.onSaveClick()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isCompletionResetConfirmationVisible)
        assertEquals(completedTrip.startDate, trips.getTripPlan(completedTrip.id)?.startDate)
        assertEquals(completedTrip.completedAt, trips.getTripPlan(completedTrip.id)?.completedAt)
    }

    @Test
    fun confirmCompletionReset_savesDatesAndClearsCompletion() = runTest {
        val trips = FakeTripPlanRepository().apply {
            setTrips(listOf(completedTrip))
        }
        val viewModel = createViewModel(trips)
        val event = async(UnconfinedTestDispatcher(testScheduler)) { viewModel.events.first() }
        viewModel.load(completedTrip.id)
        advanceUntilIdle()
        viewModel.onDateRangeChange(
            LocalDate.of(2026, 11, 10),
            LocalDate.of(2026, 11, 12),
        )
        viewModel.onSaveClick()
        advanceUntilIdle()

        viewModel.onCompletionResetConfirm()
        advanceUntilIdle()

        assertEquals(EditTripEvent.Saved, event.await())
        assertEquals(LocalDate.of(2026, 11, 10), trips.getTripPlan(completedTrip.id)?.startDate)
        assertNull(trips.getTripPlan(completedTrip.id)?.completedAt)
    }

    @Test
    fun deleteConfirmation_deletesTripAndEmitsEvent() = runTest {
        val trips = tripRepository()
        val viewModel = createViewModel(trips)
        val event = async(UnconfinedTestDispatcher(testScheduler)) { viewModel.events.first() }
        viewModel.load(sampleTrip.id)
        advanceUntilIdle()

        viewModel.onDeleteRequest()
        assertTrue(viewModel.uiState.value.isDeleteConfirmationVisible)
        viewModel.onDeleteConfirm()
        advanceUntilIdle()

        assertEquals(EditTripEvent.Deleted, event.await())
        assertNull(trips.getTripPlan(sampleTrip.id))
    }

    private fun createViewModel(
        trips: FakeTripPlanRepository = tripRepository(),
        items: FakeItineraryRepository = FakeItineraryRepository(),
    ) = EditTripViewModel(
        observeTripPlanDetails = ObserveTripPlanDetailsUseCase(trips, items),
        updateTripPlan = UpdateTripPlanUseCase(trips, items, fixedClock),
        deleteTripPlan = DeleteTripPlanUseCase(trips),
    )

    private fun tripRepository() = FakeTripPlanRepository().apply {
        setTrips(listOf(sampleTrip))
    }

    private val sampleTrip = TripPlan(
        id = 7L,
        title = "제주 여행",
        destination = "제주",
        startDate = LocalDate.of(2026, 11, 9),
        endDate = LocalDate.of(2026, 11, 11),
        description = "친구와 여행",
        createdAt = Instant.EPOCH,
        updatedAt = Instant.EPOCH,
    )
    private val completedTrip = sampleTrip.copy(
        completedAt = Instant.parse("2026-11-08T03:00:00Z"),
    )
    private val outOfRangeItem = ItineraryItem(
        id = 5L,
        tripPlanId = sampleTrip.id,
        date = sampleTrip.endDate,
        title = "마지막 날 일정",
        place = "제주",
        order = 0,
    )
    private val fixedClock = Clock.fixed(
        Instant.parse("2026-11-09T03:00:00Z"),
        ZoneOffset.UTC,
    )
}
