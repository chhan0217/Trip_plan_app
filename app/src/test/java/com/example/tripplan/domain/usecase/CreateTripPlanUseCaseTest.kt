package com.example.tripplan.domain.usecase

import com.example.tripplan.domain.model.TripPlanInput
import com.example.tripplan.domain.rule.TripPlanValidationError
import com.example.tripplan.testutil.FakeTripPlanRepository
import java.time.LocalDate
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CreateTripPlanUseCaseTest {
    @Test
    fun invoke_validInput_createsTripPlan() = runTest {
        val repository = FakeTripPlanRepository().apply { nextCreatedId = 12L }
        val useCase = CreateTripPlanUseCase(repository)
        val input = validInput()

        val result = useCase(input)

        assertEquals(CreateTripPlanResult.Success(12L), result)
        assertEquals(listOf(input), repository.createdInputs)
    }

    @Test
    fun invoke_invalidInput_doesNotCreateTripPlan() = runTest {
        val repository = FakeTripPlanRepository()
        val useCase = CreateTripPlanUseCase(repository)

        val result = useCase(
            validInput().copy(
                title = " ",
                startDate = LocalDate.of(2026, 11, 11),
                endDate = LocalDate.of(2026, 11, 9),
            )
        )

        assertEquals(
            CreateTripPlanResult.Invalid(
                setOf(
                    TripPlanValidationError.BlankTitle,
                    TripPlanValidationError.InvalidDateRange,
                )
            ),
            result,
        )
        assertTrue(repository.createdInputs.isEmpty())
    }

    private fun validInput() = TripPlanInput(
        title = "제주 여행",
        destination = "제주",
        startDate = LocalDate.of(2026, 11, 9),
        endDate = LocalDate.of(2026, 11, 11),
    )
}
