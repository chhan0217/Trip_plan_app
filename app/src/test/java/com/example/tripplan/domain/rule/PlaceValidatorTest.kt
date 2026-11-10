package com.example.tripplan.domain.rule

import com.example.tripplan.domain.model.PlaceInput
import com.example.tripplan.domain.model.PlaceSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaceValidatorTest {
    @Test
    fun manualNameOnly_isValid() {
        assertTrue(PlaceValidator.validate(PlaceInput(name = "친구가 추천한 카페")).isEmpty())
    }

    @Test
    fun onlyOneCoordinate_isInvalid() {
        val errors = PlaceValidator.validate(
            PlaceInput(name = "장소", latitude = 33.4)
        )

        assertEquals(setOf(PlaceValidationError.IncompleteCoordinates), errors)
    }

    @Test
    fun coordinatesOutsideEarthBounds_areInvalid() {
        val errors = PlaceValidator.validate(
            PlaceInput(name = "장소", latitude = 91.0, longitude = -181.0)
        )

        assertEquals(
            setOf(
                PlaceValidationError.LatitudeOutOfRange,
                PlaceValidationError.LongitudeOutOfRange,
            ),
            errors,
        )
    }

    @Test
    fun mapPinWithoutCoordinates_isInvalid() {
        val errors = PlaceValidator.validate(
            PlaceInput(name = "지도 핀", source = PlaceSource.MapPin)
        )

        assertEquals(setOf(PlaceValidationError.MissingMapPinCoordinates), errors)
    }

    @Test
    fun externalPlaceWithoutProviderName_isInvalid() {
        val errors = PlaceValidator.validate(
            PlaceInput(name = "외부 장소", source = PlaceSource.ExternalProvider)
        )

        assertEquals(setOf(PlaceValidationError.MissingProviderName), errors)
    }
}
