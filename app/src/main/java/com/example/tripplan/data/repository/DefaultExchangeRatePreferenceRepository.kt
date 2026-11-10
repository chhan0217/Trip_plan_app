package com.example.tripplan.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.example.tripplan.domain.repository.ExchangeRatePreferenceRepository
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

class DefaultExchangeRatePreferenceRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : ExchangeRatePreferenceRepository {
    override fun observeSelectedCurrencyUnits(): Flow<Set<String>> {
        return dataStore.data
            .catch { error ->
                if (error is IOException) emit(emptyPreferences()) else throw error
            }
            .map { preferences ->
                preferences[SELECTED_CURRENCIES]
                    ?.mapTo(linkedSetOf(), ::normalizeCurrencyCode)
                    ?.filterTo(linkedSetOf()) { it.isNotBlank() }
                    ?.ifEmpty { DEFAULT_SELECTED_CURRENCIES }
                    ?: DEFAULT_SELECTED_CURRENCIES
            }
    }

    override suspend fun updateSelectedCurrencyUnits(currencyUnits: Set<String>) {
        val normalized = currencyUnits
            .mapTo(sortedSetOf(), ::normalizeCurrencyCode)
            .filter { it.isNotBlank() }
            .toSet()
        require(normalized.isNotEmpty()) { "At least one currency must be selected." }
        dataStore.edit { preferences ->
            preferences[SELECTED_CURRENCIES] = normalized
        }
    }

    private companion object {
        val SELECTED_CURRENCIES = stringSetPreferencesKey("selected_exchange_rate_currencies")
        val DEFAULT_SELECTED_CURRENCIES = linkedSetOf("USD", "JPY", "EUR")
    }
}

private fun normalizeCurrencyCode(value: String): String {
    return when (val normalized = value.trim().uppercase()) {
        "JPY(100)" -> "JPY"
        "IDR(100)" -> "IDR"
        else -> normalized
    }
}
