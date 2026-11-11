package com.example.tripplan.domain.repository

import com.example.tripplan.domain.model.TripMoneyDisplayCurrency
import kotlinx.coroutines.flow.Flow

interface TripMoneyDisplayPreferenceRepository {
    fun observeDisplayCurrency(tripPlanId: Long): Flow<TripMoneyDisplayCurrency?>
    suspend fun updateDisplayCurrency(
        tripPlanId: Long,
        displayCurrency: TripMoneyDisplayCurrency,
    )
}
