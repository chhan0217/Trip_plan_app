package com.example.tripplan.presentation.itinerary

import com.example.tripplan.domain.model.ItineraryItem
import com.example.tripplan.domain.model.TripPlan
import com.example.tripplan.domain.usecase.CreateItineraryItemUseCase
import com.example.tripplan.domain.usecase.DeleteItineraryItemUseCase
import com.example.tripplan.domain.usecase.PrepareItineraryEditorUseCase
import com.example.tripplan.domain.usecase.UpdateItineraryItemUseCase
import com.example.tripplan.domain.usecase.ObservePlacesUseCase
import com.example.tripplan.testutil.FakeItineraryRepository
import com.example.tripplan.testutil.FakeTripPlanRepository
import com.example.tripplan.testutil.MainDispatcherRule
import com.example.tripplan.testutil.FakePlaceRepository
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ItineraryEditorViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun load_createMode_usesRequestedInitialDate() = runTest {
        val viewModel = createViewModel()

        viewModel.load(sampleTrip.id, null, secondDate)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertFalse(state.isEdit)
        assertEquals(secondDate, state.selectedDate)
        assertEquals(3, state.days.size)
    }

    @Test
    fun save_validCreateInput_persistsAndEmitsSaved() = runTest {
        val items = FakeItineraryRepository().apply { nextCreatedId = 20L }
        val viewModel = createViewModel(items = items)
        val event = async(UnconfinedTestDispatcher(testScheduler)) { viewModel.events.first() }
        viewModel.load(sampleTrip.id, null, firstDate)
        advanceUntilIdle()
        viewModel.onTitleChange("성산일출봉")
        viewModel.onPlaceChange("제주")
        viewModel.onStartTimeChange("09:00")
        viewModel.onEndTimeChange("11:00")

        viewModel.onSaveClick()
        advanceUntilIdle()

        assertEquals(ItineraryEditorEvent.Saved, event.await())
        assertEquals("성산일출봉", items.getItem(20L)?.title)
        assertFalse(viewModel.uiState.value.isSaveFailed)
    }

    @Test
    fun save_invalidTimeText_showsTimeErrorWithoutSaving() = runTest {
        val items = FakeItineraryRepository()
        val viewModel = createViewModel(items = items)
        viewModel.load(sampleTrip.id, null, firstDate)
        advanceUntilIdle()
        viewModel.onTitleChange("일정")
        viewModel.onStartTimeChange("9시")

        viewModel.onSaveClick()

        assertTrue(viewModel.uiState.value.isTimeError)
        assertTrue(items.observeItems(sampleTrip.id).first().isEmpty())
    }

    @Test
    fun load_editAndDelete_removesItemAndEmitsDeleted() = runTest {
        val items = FakeItineraryRepository().apply { setItems(listOf(sampleItem)) }
        val viewModel = createViewModel(items = items)
        val event = async(UnconfinedTestDispatcher(testScheduler)) { viewModel.events.first() }
        viewModel.load(sampleTrip.id, sampleItem.id, null)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isEdit)
        assertEquals("기존 일정", viewModel.uiState.value.title)

        viewModel.onDeleteRequest()
        assertTrue(viewModel.uiState.value.isDeleteConfirmationVisible)
        viewModel.onDeleteConfirm()
        advanceUntilIdle()

        assertEquals(ItineraryEditorEvent.Deleted, event.await())
        assertTrue(items.observeItems(sampleTrip.id).first().isEmpty())
    }

    private fun createViewModel(
        trips: FakeTripPlanRepository = FakeTripPlanRepository().apply {
            setTrips(listOf(sampleTrip))
        },
        items: FakeItineraryRepository = FakeItineraryRepository(),
    ) = ItineraryEditorViewModel(
        prepareEditor = PrepareItineraryEditorUseCase(trips, items),
        createItem = CreateItineraryItemUseCase(trips, items),
        updateItem = UpdateItineraryItemUseCase(trips, items),
        deleteItem = DeleteItineraryItemUseCase(items),
        observePlaces = ObservePlacesUseCase(FakePlaceRepository()),
    )

    private val firstDate = LocalDate.of(2026, 11, 9)
    private val secondDate = LocalDate.of(2026, 11, 10)
    private val sampleTrip = TripPlan(
        id = 7L,
        title = "제주 여행",
        destination = "제주",
        startDate = firstDate,
        endDate = LocalDate.of(2026, 11, 11),
        description = "",
        createdAt = Instant.EPOCH,
        updatedAt = Instant.EPOCH,
    )
    private val sampleItem = ItineraryItem(
        id = 3L,
        tripPlanId = sampleTrip.id,
        date = firstDate,
        title = "기존 일정",
        place = "제주",
        order = 0,
    )
}
