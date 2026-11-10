package com.example.tripplan.domain.rule

import com.example.tripplan.domain.model.PlaceInput
import com.example.tripplan.domain.model.PlaceSource

enum class PlaceValidationError {
    BlankName,
    IncompleteCoordinates,
    LatitudeOutOfRange,
    LongitudeOutOfRange,
    MissingProviderName,
    MissingMapPinCoordinates,
}

object PlaceValidator {
    fun validate(input: PlaceInput): Set<PlaceValidationError> = buildSet {
        if (input.name.isBlank()) add(PlaceValidationError.BlankName)
        if ((input.latitude == null) != (input.longitude == null)) {
            add(PlaceValidationError.IncompleteCoordinates)
        }
        input.latitude?.let {
            if (it !in -90.0..90.0) add(PlaceValidationError.LatitudeOutOfRange)
        }
        input.longitude?.let {
            if (it !in -180.0..180.0) add(PlaceValidationError.LongitudeOutOfRange)
        }
        if (input.source == PlaceSource.MapPin &&
            (input.latitude == null || input.longitude == null)
        ) {
            add(PlaceValidationError.MissingMapPinCoordinates)
        }
        if (input.source == PlaceSource.ExternalProvider && input.providerName.isNullOrBlank()) {
            add(PlaceValidationError.MissingProviderName)
        }
    }
}
