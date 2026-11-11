package com.example.tripplan.domain.rule

import com.example.tripplan.domain.model.TripPlanInput
import com.example.tripplan.domain.model.CountryCurrencyCatalog
import java.util.Currency

enum class TripPlanValidationError {
    BlankTitle,
    InvalidDateRange,
    InvalidCountryOrCurrency,
}

object TripPlanValidator {
    fun validate(input: TripPlanInput): Set<TripPlanValidationError> = buildSet {
        if (input.title.isBlank()) {
            add(TripPlanValidationError.BlankTitle)
        }
        if (input.endDate.isBefore(input.startDate)) {
            add(TripPlanValidationError.InvalidDateRange)
        }
        if (
            CountryCurrencyCatalog.find(input.countryCode) == null ||
            runCatching { Currency.getInstance(input.currencyCode) }.isFailure
        ) {
            add(TripPlanValidationError.InvalidCountryOrCurrency)
        }
    }
}
