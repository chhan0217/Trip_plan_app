package com.example.tripplan.data.repository

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.example.tripplan.domain.model.TripMoneyDisplayCurrency
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class DefaultTripMoneyDisplayPreferenceRepositoryTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun displayCurrency_isStoredIndependentlyForEachTrip() = runTest {
        val dataStore = PreferenceDataStoreFactory.create(
            scope = backgroundScope,
            produceFile = { File(temporaryFolder.root, "trip_money.preferences_pb") },
        )
        val repository = DefaultTripMoneyDisplayPreferenceRepository(dataStore)

        assertNull(repository.observeDisplayCurrency(7).first())
        repository.updateDisplayCurrency(7, TripMoneyDisplayCurrency.Local)
        repository.updateDisplayCurrency(8, TripMoneyDisplayCurrency.KRW)

        assertEquals(
            TripMoneyDisplayCurrency.Local,
            repository.observeDisplayCurrency(7).first(),
        )
        assertEquals(
            TripMoneyDisplayCurrency.KRW,
            repository.observeDisplayCurrency(8).first(),
        )
    }
}
