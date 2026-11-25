package com.example.tripplan.domain.usecase

import com.example.tripplan.domain.model.TripPlan
import com.example.tripplan.testutil.FakeTripPlanRepository
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FinalizeTripPlanDraftUseCaseTest {
    @Test
    fun `finalize moves draft to ready state`() = runTest {
        val repository = FakeTripPlanRepository().apply {
            setTrips(listOf(sampleTrip.copy(isDraft = true)))
        }
        val useCase = FinalizeTripPlanDraftUseCase(repository, fixedClock)

        assertTrue(useCase(sampleTrip.id))

        val saved = repository.observeTripPlans().first().single()
        assertFalse(saved.isDraft)
        assertTrue(saved.updatedAt == fixedClock.instant())
    }

    private val sampleTrip = TripPlan(
        id = 7L,
        title = "다낭 여행",
        destination = "다낭",
        startDate = LocalDate.of(2026, 11, 20),
        endDate = LocalDate.of(2026, 11, 23),
        description = "",
        createdAt = Instant.EPOCH,
        updatedAt = Instant.EPOCH,
    )

    private companion object {
        val fixedClock: Clock = Clock.fixed(
            Instant.parse("2026-11-20T01:00:00Z"),
            ZoneOffset.UTC,
        )
    }
}
