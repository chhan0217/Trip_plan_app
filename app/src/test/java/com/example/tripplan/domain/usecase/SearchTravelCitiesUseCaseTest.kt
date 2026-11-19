package com.example.tripplan.domain.usecase

import com.example.tripplan.domain.model.TravelCity
import com.example.tripplan.domain.repository.TravelCityRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchTravelCitiesUseCaseTest {
    @Test
    fun validQuery_normalizesCountryAndQuery() = runTest {
        val repository = RecordingRepository()
        val useCase = SearchTravelCitiesUseCase(repository)

        useCase("th", "  방콕  ")

        assertEquals("TH", repository.countryCode)
        assertEquals("방콕", repository.query)
    }

    @Test
    fun shortQuery_doesNotCallRepository() = runTest {
        val repository = RecordingRepository()

        val result = SearchTravelCitiesUseCase(repository)("TH", "방")

        assertTrue(result.isEmpty())
        assertEquals(0, repository.callCount)
    }

    private class RecordingRepository : TravelCityRepository {
        var countryCode: String? = null
        var query: String? = null
        var callCount = 0

        override suspend fun search(countryCode: String, query: String): List<TravelCity> {
            this.countryCode = countryCode
            this.query = query
            callCount++
            return emptyList()
        }
    }
}
