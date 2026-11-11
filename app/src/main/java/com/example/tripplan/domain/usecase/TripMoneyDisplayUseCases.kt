package com.example.tripplan.domain.usecase

import com.example.tripplan.domain.model.TripMoneyDisplayCurrency
import com.example.tripplan.domain.repository.TripMoneyDisplayPreferenceRepository
import javax.inject.Inject

class ObserveTripMoneyDisplayCurrencyUseCase @Inject constructor(
    private val repository: TripMoneyDisplayPreferenceRepository,
) {
    operator fun invoke(tripPlanId: Long) = repository.observeDisplayCurrency(tripPlanId)
}

class UpdateTripMoneyDisplayCurrencyUseCase @Inject constructor(
    private val repository: TripMoneyDisplayPreferenceRepository,
) {
    suspend operator fun invoke(
        tripPlanId: Long,
        displayCurrency: TripMoneyDisplayCurrency,
    ) = repository.updateDisplayCurrency(tripPlanId, displayCurrency)
}
