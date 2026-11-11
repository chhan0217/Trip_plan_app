package com.example.tripplan.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.tripplan.domain.model.TripMoneyDisplayCurrency
import com.example.tripplan.domain.repository.TripMoneyDisplayPreferenceRepository
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

class DefaultTripMoneyDisplayPreferenceRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : TripMoneyDisplayPreferenceRepository {
    override fun observeDisplayCurrency(
        tripPlanId: Long,
    ): Flow<TripMoneyDisplayCurrency?> = dataStore.data
        .catch { error ->
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .map { preferences ->
            preferences[displayCurrencyKey(tripPlanId)]?.let { stored ->
                TripMoneyDisplayCurrency.entries.firstOrNull { it.name == stored }
            }
        }

    override suspend fun updateDisplayCurrency(
        tripPlanId: Long,
        displayCurrency: TripMoneyDisplayCurrency,
    ) {
        dataStore.edit { preferences ->
            preferences[displayCurrencyKey(tripPlanId)] = displayCurrency.name
        }
    }

    private fun displayCurrencyKey(tripPlanId: Long) =
        stringPreferencesKey("trip_money_display_$tripPlanId")
}
