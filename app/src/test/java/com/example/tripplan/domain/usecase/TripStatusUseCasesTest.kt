package com.example.tripplan.domain.usecase

import com.example.tripplan.domain.model.TripDatePhase
import com.example.tripplan.domain.model.TripPlan
import com.example.tripplan.testutil.FakeTripPlanRepository
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.flow.first
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TripStatusUseCasesTest {
    @Test
    fun getStatus_usesClockAndCompletionState() {
        val trip = sampleTrip.copy(
            startDate = LocalDate.of(2026, 11, 10),
            endDate = LocalDate.of(2026, 11, 12),
            completedAt = Instant.parse("2026-11-08T00:00:00Z"),
        )

        val result = GetTripStatusUseCase(fixedClock)(trip)

        assertEquals(TripDatePhase.Upcoming, result.dateStatus.phase)
        assertEquals(1L, result.dateStatus.daysUntilStart)
        assertTrue(result.isCompleted)
    }

    @Test
    fun complete_existingTrip_savesSameCompletionAndUpdateInstant() = runTest {
        val repository = FakeTripPlanRepository().apply { setTrips(listOf(sampleTrip)) }

        val result = CompleteTripPlanUseCase(repository, fixedClock)(sampleTrip.id)
        val updated = repository.getTripPlan(sampleTrip.id)

        assertEquals(TripCompletionResult.Success, result)
        assertEquals(fixedClock.instant(), updated?.completedAt)
        assertEquals(fixedClock.instant(), updated?.updatedAt)
    }

    @Test
    fun undoCompletion_existingTrip_clearsCompletionAndUpdatesTimestamp() = runTest {
        val repository = FakeTripPlanRepository().apply {
            setTrips(listOf(sampleTrip.copy(completedAt = Instant.EPOCH)))
        }

        val result = UndoTripCompletionUseCase(repository, fixedClock)(sampleTrip.id)
        val updated = repository.getTripPlan(sampleTrip.id)

        assertEquals(TripCompletionResult.Success, result)
        assertNull(updated?.completedAt)
        assertEquals(fixedClock.instant(), updated?.updatedAt)
    }

    @Test
    fun completion_missingTrip_returnsNotFoundWithoutCreatingData() = runTest {
        val repository = FakeTripPlanRepository()

        val complete = CompleteTripPlanUseCase(repository, fixedClock)(99L)
        val undo = UndoTripCompletionUseCase(repository, fixedClock)(99L)

        assertEquals(TripCompletionResult.NotFound, complete)
        assertEquals(TripCompletionResult.NotFound, undo)
        assertTrue(repository.observeTripPlans().first().isEmpty())
    }

    private val sampleTrip = TripPlan(
        id = 7L,
        title = "제주 여행",
        destination = "제주",
        startDate = LocalDate.of(2026, 11, 9),
        endDate = LocalDate.of(2026, 11, 11),
        description = "",
        createdAt = Instant.EPOCH,
        updatedAt = Instant.EPOCH,
    )
    private val fixedClock = Clock.fixed(
        Instant.parse("2026-11-09T03:00:00Z"),
        ZoneOffset.UTC,
    )
}
