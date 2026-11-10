package com.example.tripplan.domain.rule

import com.example.tripplan.domain.model.ItineraryItemInput
import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ItineraryItemValidatorTest {
    private val tripStartDate = LocalDate.of(2026, 11, 9)
    private val tripEndDate = LocalDate.of(2026, 11, 11)

    @Test
    fun validate_validInput_returnsNoErrors() {
        val errors = ItineraryItemValidator.validate(
            input = validInput(),
            tripStartDate = tripStartDate,
            tripEndDate = tripEndDate,
        )

        assertTrue(errors.isEmpty())
    }

    @Test
    fun validate_blankTitle_returnsBlankTitleError() {
        val errors = ItineraryItemValidator.validate(
            input = validInput().copy(title = " "),
            tripStartDate = tripStartDate,
            tripEndDate = tripEndDate,
        )

        assertEquals(setOf(ItineraryItemValidationError.BlankTitle), errors)
    }

    @Test
    fun validate_dateOutsideTrip_returnsDateOutsideTripError() {
        val errors = ItineraryItemValidator.validate(
            input = validInput().copy(date = tripEndDate.plusDays(1)),
            tripStartDate = tripStartDate,
            tripEndDate = tripEndDate,
        )

        assertEquals(setOf(ItineraryItemValidationError.DateOutsideTrip), errors)
    }

    @Test
    fun validate_endTimeBeforeStartTime_returnsInvalidTimeRangeError() {
        val errors = ItineraryItemValidator.validate(
            input = validInput().copy(
                startTime = LocalTime.of(15, 0),
                endTime = LocalTime.of(14, 59),
            ),
            tripStartDate = tripStartDate,
            tripEndDate = tripEndDate,
        )

        assertEquals(setOf(ItineraryItemValidationError.InvalidTimeRange), errors)
    }

    @Test
    fun validate_onlyOneTimeProvided_isValid() {
        val errors = ItineraryItemValidator.validate(
            input = validInput().copy(
                startTime = LocalTime.of(15, 0),
                endTime = null,
            ),
            tripStartDate = tripStartDate,
            tripEndDate = tripEndDate,
        )

        assertTrue(errors.isEmpty())
    }

    private fun validInput() = ItineraryItemInput(
        date = LocalDate.of(2026, 11, 10),
        title = "성산일출봉 방문",
        place = "성산일출봉",
        startTime = LocalTime.of(10, 0),
        endTime = LocalTime.of(12, 0),
    )
}
