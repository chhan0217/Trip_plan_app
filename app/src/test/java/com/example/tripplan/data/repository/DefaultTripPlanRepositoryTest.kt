package com.example.tripplan.data.repository

import com.example.tripplan.data.local.TripPlanDao
import com.example.tripplan.data.local.TripPlanEntity
import com.example.tripplan.domain.model.TripPlanInput
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.Assert.assertTrue

class DefaultTripPlanRepositoryTest {
    @Test
    fun createTripPlan_trimsTextAndUsesClockForTimestamps() = runTest {
        val dao = FakeTripPlanDao()
        val clock = Clock.fixed(Instant.parse("2026-11-09T01:00:00Z"), ZoneOffset.UTC)
        val repository = DefaultTripPlanRepository(dao, clock)

        val id = repository.createTripPlan(
            TripPlanInput(
                title = "  제주 여행  ",
                destination = "  제주  ",
                startDate = LocalDate.of(2026, 11, 9),
                endDate = LocalDate.of(2026, 11, 11),
                description = "  친구와 여행  ",
            )
        )

        assertEquals(42L, id)
        assertEquals(
            TripPlanEntity(
                title = "제주 여행",
                destination = "제주",
                startDate = "2026-11-09",
                endDate = "2026-11-11",
                description = "친구와 여행",
                createdAt = 1794186000000L,
                updatedAt = 1794186000000L,
            ),
            dao.insertedTripPlan,
        )
    }

    @Test
    fun updateTripCompletion_mapsInstantsAndReturnsDaoResult() = runTest {
        val dao = FakeTripPlanDao().apply { completionUpdateCount = 1 }
        val repository = DefaultTripPlanRepository(dao, Clock.systemUTC())
        val completedAt = Instant.parse("2026-11-09T03:00:00Z")
        val updatedAt = Instant.parse("2026-11-09T04:00:00Z")

        val result = repository.updateTripCompletion(7L, completedAt, updatedAt)

        assertTrue(result)
        assertEquals(7L, dao.completionTripId)
        assertEquals(completedAt.toEpochMilli(), dao.completionValue)
        assertEquals(updatedAt.toEpochMilli(), dao.completionUpdatedAt)
    }

    private class FakeTripPlanDao : TripPlanDao {
        private val trips = MutableStateFlow<List<TripPlanEntity>>(emptyList())
        var insertedTripPlan: TripPlanEntity? = null
        var completionUpdateCount = 0
        var completionTripId: Long? = null
        var completionValue: Long? = null
        var completionUpdatedAt: Long? = null

        override fun observeTripPlans(): Flow<List<TripPlanEntity>> = trips

        override fun observeTripPlan(tripPlanId: Long): Flow<TripPlanEntity?> {
            return MutableStateFlow(trips.value.firstOrNull { it.id == tripPlanId })
        }

        override suspend fun getTripPlan(tripPlanId: Long): TripPlanEntity? {
            return trips.value.firstOrNull { it.id == tripPlanId }
        }

        override suspend fun insertTripPlan(tripPlan: TripPlanEntity): Long {
            insertedTripPlan = tripPlan
            return 42L
        }

        override suspend fun updateTripPlan(tripPlan: TripPlanEntity) = Unit

        override suspend fun updateTripCompletion(
            tripPlanId: Long,
            completedAt: Long?,
            updatedAt: Long,
        ): Int {
            completionTripId = tripPlanId
            completionValue = completedAt
            completionUpdatedAt = updatedAt
            return completionUpdateCount
        }

        override suspend fun deleteTripPlan(tripPlanId: Long) = Unit

        override suspend fun deleteItineraryItemsByIds(itemIds: List<Long>) = Unit
        override suspend fun deleteTripBudget(tripPlanId: Long) = Unit
        override suspend fun deleteExpenses(tripPlanId: Long) = Unit
        override suspend fun hasExpenses(tripPlanId: Long): Boolean = false
        override suspend fun clearItineraryCosts(tripPlanId: Long) = Unit
    }
}
