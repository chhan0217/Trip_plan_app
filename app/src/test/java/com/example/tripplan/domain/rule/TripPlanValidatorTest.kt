package com.example.tripplan.domain.rule

import com.example.tripplan.domain.model.TripPlanInput
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TripPlanValidatorTest {
    @Test
    fun validate_validInput_returnsNoErrors() {
        val errors = TripPlanValidator.validate(validInput())

        assertTrue(errors.isEmpty())
    }

    @Test
    fun validate_blankTitle_returnsBlankTitleError() {
        val errors = TripPlanValidator.validate(validInput().copy(title = "   "))

        assertEquals(setOf(TripPlanValidationError.BlankTitle), errors)
    }

    @Test
    fun validate_endBeforeStart_returnsInvalidDateRangeError() {
        val errors = TripPlanValidator.validate(
            validInput().copy(
                startDate = LocalDate.of(2026, 11, 10),
                endDate = LocalDate.of(2026, 11, 9),
            )
        )

        assertEquals(setOf(TripPlanValidationError.InvalidDateRange), errors)
    }

    @Test
    fun validate_multipleInvalidFields_returnsAllErrors() {
        val errors = TripPlanValidator.validate(
            validInput().copy(
                title = "",
                startDate = LocalDate.of(2026, 11, 10),
                endDate = LocalDate.of(2026, 11, 9),
            )
        )

        assertEquals(
            setOf(
                TripPlanValidationError.BlankTitle,
                TripPlanValidationError.InvalidDateRange,
            ),
            errors,
        )
    }

    private fun validInput() = TripPlanInput(
        title = "제주 여행",
        destination = "제주",
        startDate = LocalDate.of(2026, 11, 9),
        endDate = LocalDate.of(2026, 11, 11),
    )
}
