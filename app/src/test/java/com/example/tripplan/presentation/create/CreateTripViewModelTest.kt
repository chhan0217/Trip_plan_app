package com.example.tripplan.presentation.create

import com.example.tripplan.domain.model.Region
import com.example.tripplan.domain.repository.RegionRepository
import com.example.tripplan.domain.usecase.CreateTripPlanUseCase
import com.example.tripplan.domain.usecase.SearchRegionsUseCase
import com.example.tripplan.testutil.FakeTripPlanRepository
import com.example.tripplan.testutil.MainDispatcherRule
import java.time.LocalDate
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
class CreateTripViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun initialState_isEmptyAndNotSaving() {
        val viewModel = createViewModel()

        assertEquals("", viewModel.uiState.value.tripTitle)
        assertNull(viewModel.uiState.value.startDate)
        assertFalse(viewModel.uiState.value.isSaving)
        assertFalse(viewModel.uiState.value.isSaveFailed)
    }

    @Test
    fun saveClick_missingRequiredFields_showsFieldErrors() {
        val viewModel = createViewModel()

        viewModel.onSaveClick()

        assertTrue(viewModel.uiState.value.isTitleError)
        assertEquals(CreateTripDateError.MissingRange, viewModel.uiState.value.dateError)
    }

    @Test
    fun saveClick_invalidDateRange_showsDateError() {
        val viewModel = createViewModel()
        viewModel.onTripTitleChange("제주 여행")
        viewModel.onTripDateRangeChange(
            startDate = LocalDate.of(2026, 11, 11),
            endDate = LocalDate.of(2026, 11, 9),
        )

        viewModel.onSaveClick()

        assertEquals(CreateTripDateError.InvalidRange, viewModel.uiState.value.dateError)
    }

    @Test
    fun saveClick_validInput_createsTripAndEmitsNavigationEvent() = runTest {
        val repository = FakeTripPlanRepository().apply { nextCreatedId = 7L }
        val viewModel = createViewModel(repository)
        val event = async(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.events.first()
        }
        viewModel.onTripTitleChange("제주 여행")
        viewModel.onTripPlaceChange("제주")
        viewModel.onCountrySelected("JP")
        viewModel.onTripDateRangeChange(
            startDate = LocalDate.of(2026, 11, 9),
            endDate = LocalDate.of(2026, 11, 11),
        )
        viewModel.onDescriptionChange("친구와 여행")

        viewModel.onSaveClick()
        advanceUntilIdle()

        assertEquals(CreateTripEvent.TripCreated(7L), event.await())
        assertEquals(1, repository.createdInputs.size)
        assertEquals("제주 여행", repository.createdInputs.single().title)
        assertEquals("제주", repository.createdInputs.single().destination)
        assertEquals("JP", repository.createdInputs.single().countryCode)
        assertEquals("JPY", repository.createdInputs.single().currencyCode)
        assertFalse(viewModel.uiState.value.isSaving)
        assertFalse(viewModel.uiState.value.isSaveFailed)
    }

    @Test
    fun saveClick_repositoryFailure_showsRetryableError() = runTest {
        val repository = FakeTripPlanRepository().apply {
            createFailure = IllegalStateException("database failure")
        }
        val viewModel = createViewModel(repository)
        viewModel.onTripTitleChange("제주 여행")
        viewModel.onTripDateRangeChange(
            startDate = LocalDate.of(2026, 11, 9),
            endDate = LocalDate.of(2026, 11, 11),
        )

        viewModel.onSaveClick()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isSaveFailed)
        assertFalse(viewModel.uiState.value.isSaving)
    }

    private fun createViewModel(
        repository: FakeTripPlanRepository = FakeTripPlanRepository(),
    ): CreateTripViewModel {
        return CreateTripViewModel(
            searchRegionsUseCase = SearchRegionsUseCase(EmptyRegionRepository),
            createTripPlanUseCase = CreateTripPlanUseCase(repository),
        )
    }

    private object EmptyRegionRepository : RegionRepository {
        override suspend fun searchRegions(query: String, limit: Int): List<Region> = emptyList()
    }
}
