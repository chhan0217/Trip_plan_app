package com.example.tripplan.data.repository

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class DefaultExchangeRatePreferenceRepositoryTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun selectedCurrencies_useDefaultsThenPersistUserSelection() = runTest {
        val dataStoreFile = File(temporaryFolder.root, "exchange_rate.preferences_pb")
        val dataStore = PreferenceDataStoreFactory.create(
            scope = backgroundScope,
            produceFile = { dataStoreFile },
        )
        val repository = DefaultExchangeRatePreferenceRepository(dataStore)

        assertEquals(
            linkedSetOf("USD", "JPY", "EUR"),
            repository.observeSelectedCurrencyUnits().first(),
        )

        repository.updateSelectedCurrencyUnits(setOf("  CAD ", "EUR"))

        assertEquals(
            setOf("CAD", "EUR"),
            repository.observeSelectedCurrencyUnits().first(),
        )
    }
}
