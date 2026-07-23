package com.example.tripplan.domain.usecase

import com.example.tripplan.domain.model.Region
import com.example.tripplan.domain.repository.RegionRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class SearchRegionsUseCaseTest {
    @Test
    fun invoke_returnsEmptyList_whenQueryIsShorterThanTwoCharacters() = runTest {
        val repository = FakeRegionRepository()
        val useCase = SearchRegionsUseCase(repository)

        val result = useCase("제")

        assertEquals(emptyList<Region>(), result)
        assertEquals(emptyList<String>(), repository.receivedQueries)
    }

    @Test
    fun invoke_trimsQueryAndSearchesRepository() = runTest {
        val repository = FakeRegionRepository()
        val useCase = SearchRegionsUseCase(repository)

        val result = useCase(" 제주 ")

        assertEquals(listOf(sampleRegion), result)
        assertEquals(listOf("제주"), repository.receivedQueries)
        assertEquals(listOf(10), repository.receivedLimits)
    }

    private class FakeRegionRepository : RegionRepository {
        val receivedQueries = mutableListOf<String>()
        val receivedLimits = mutableListOf<Int>()

        override suspend fun searchRegions(query: String, limit: Int): List<Region> {
            receivedQueries += query
            receivedLimits += limit
            return listOf(sampleRegion)
        }
    }

    private companion object {
        val sampleRegion = Region(
            id = "제주특별자치도|제주시",
            placeName = "제주시",
            provinceName = "제주특별자치도",
            cityName = "제주시",
            districtName = "",
            latitude = 33.4996,
            longitude = 126.5312,
        )
    }
}
