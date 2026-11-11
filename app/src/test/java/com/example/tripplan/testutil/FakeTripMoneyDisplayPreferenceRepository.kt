package com.example.tripplan.testutil

import com.example.tripplan.domain.model.TripMoneyDisplayCurrency
import com.example.tripplan.domain.repository.TripMoneyDisplayPreferenceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeTripMoneyDisplayPreferenceRepository(
    initial: TripMoneyDisplayCurrency? = null,
) : TripMoneyDisplayPreferenceRepository {
    private val displayCurrency = MutableStateFlow(initial)

    override fun observeDisplayCurrency(tripPlanId: Long): Flow<TripMoneyDisplayCurrency?> =
        displayCurrency

    override suspend fun updateDisplayCurrency(
        tripPlanId: Long,
        displayCurrency: TripMoneyDisplayCurrency,
    ) {
        this.displayCurrency.value = displayCurrency
    }
}
