package com.example.tripplan.domain.usecase

import com.example.tripplan.domain.model.ItineraryItem
import com.example.tripplan.domain.model.TripPlan
import com.example.tripplan.domain.model.TripPlanInput
import com.example.tripplan.domain.rule.TripPlanValidationError
import com.example.tripplan.testutil.FakeItineraryRepository
import com.example.tripplan.testutil.FakeTripPlanRepository
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TripPlanMutationUseCasesTest {
    @Test
    fun update_withoutOutOfRangeItems_updatesTrimmedFieldsAndTimestamp() = runTest {
        val trips = FakeTripPlanRepository().apply { setTrips(listOf(sampleTrip)) }
        val useCase = UpdateTripPlanUseCase(trips, FakeItineraryRepository(), fixedClock)

        val result = useCase(
            sampleTrip.id,
            validInput.copy(title = "  수정 여행  ", destination = "  부산  "),
        )

        assertEquals(UpdateTripPlanResult.Success(sampleTrip.id), result)
        val updated = trips.getTripPlan(sampleTrip.id)
        assertEquals("수정 여행", updated?.title)
        assertEquals("부산", updated?.destination)
        assertEquals(fixedClock.instant(), updated?.updatedAt)
    }

    @Test
    fun shrinkPeriod_withOutOfRangeItems_requiresConfirmationWithoutChangingTrip() = runTest {
        val trips = FakeTripPlanRepository().apply { setTrips(listOf(sampleTrip)) }
        val items = FakeItineraryRepository().apply {
            setItems(listOf(item(1L, LocalDate.of(2026, 11, 11))))
        }
        val useCase = UpdateTripPlanUseCase(trips, items, fixedClock)

        val result = useCase(
            sampleTrip.id,
            validInput.copy(endDate = LocalDate.of(2026, 11, 10)),
        )

        assertEquals(UpdateTripPlanResult.RequiresOutOfRangeConfirmation(1), result)
        assertEquals(sampleTrip.endDate, trips.getTripPlan(sampleTrip.id)?.endDate)
    }

    @Test
    fun shrinkPeriod_afterConfirmation_updatesTripAndRequestsItemDeletion() = runTest {
        val trips = FakeTripPlanRepository().apply { setTrips(listOf(sampleTrip)) }
        val items = FakeItineraryRepository().apply {
            setItems(
                listOf(
                    item(1L, LocalDate.of(2026, 11, 9)),
                    item(2L, LocalDate.of(2026, 11, 11)),
                )
            )
        }
        val useCase = UpdateTripPlanUseCase(trips, items, fixedClock)

        val result = useCase(
            sampleTrip.id,
            validInput.copy(endDate = LocalDate.of(2026, 11, 10)),
            deleteOutOfRangeItems = true,
        )

        assertEquals(UpdateTripPlanResult.Success(sampleTrip.id), result)
        assertEquals(LocalDate.of(2026, 11, 10), trips.getTripPlan(sampleTrip.id)?.endDate)
        assertEquals(listOf(2L), trips.lastDeletedItineraryItemIds)
    }

    @Test
    fun update_invalidInput_returnsValidationErrors() = runTest {
        val useCase = UpdateTripPlanUseCase(
            FakeTripPlanRepository().apply { setTrips(listOf(sampleTrip)) },
            FakeItineraryRepository(),
            fixedClock,
        )

        val result = useCase(sampleTrip.id, validInput.copy(title = " "))

        assertEquals(
            UpdateTripPlanResult.Invalid(setOf(TripPlanValidationError.BlankTitle)),
            result,
        )
    }

    @Test
    fun completedTrip_movedToCurrentOrFuture_requiresCompletionResetConfirmation() = runTest {
        val completedTrip = sampleTrip.copy(
            startDate = LocalDate.of(2026, 10, 1),
            endDate = LocalDate.of(2026, 10, 3),
            completedAt = Instant.parse("2026-10-04T00:00:00Z"),
        )
        val trips = FakeTripPlanRepository().apply { setTrips(listOf(completedTrip)) }
        val useCase = UpdateTripPlanUseCase(trips, FakeItineraryRepository(), fixedClock)

        val result = useCase(completedTrip.id, validInput)

        assertEquals(UpdateTripPlanResult.RequiresCompletionResetConfirmation, result)
        assertEquals(completedTrip, trips.getTripPlan(completedTrip.id))
    }

    @Test
    fun completedTrip_afterResetConfirmation_movesDateAndClearsCompletion() = runTest {
        val completedTrip = sampleTrip.copy(
            startDate = LocalDate.of(2026, 10, 1),
            endDate = LocalDate.of(2026, 10, 3),
            completedAt = Instant.parse("2026-10-04T00:00:00Z"),
        )
        val trips = FakeTripPlanRepository().apply { setTrips(listOf(completedTrip)) }
        val useCase = UpdateTripPlanUseCase(trips, FakeItineraryRepository(), fixedClock)

        val result = useCase(completedTrip.id, validInput, clearCompletion = true)
        val updated = trips.getTripPlan(completedTrip.id)

        assertEquals(UpdateTripPlanResult.Success(completedTrip.id), result)
        assertNull(updated?.completedAt)
        assertEquals(validInput.startDate, updated?.startDate)
    }

    @Test
    fun delete_existingTrip_removesTrip() = runTest {
        val trips = FakeTripPlanRepository().apply { setTrips(listOf(sampleTrip)) }

        val result = DeleteTripPlanUseCase(trips)(sampleTrip.id)

        assertEquals(DeleteTripPlanResult.Success, result)
        assertNull(trips.getTripPlan(sampleTrip.id))
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
    private val validInput = TripPlanInput(
        title = sampleTrip.title,
        destination = sampleTrip.destination,
        startDate = sampleTrip.startDate,
        endDate = sampleTrip.endDate,
        description = sampleTrip.description,
    )
    private val fixedClock = Clock.fixed(
        Instant.parse("2026-11-09T03:00:00Z"),
        ZoneOffset.UTC,
    )

    private fun item(id: Long, date: LocalDate) = ItineraryItem(
        id = id,
        tripPlanId = sampleTrip.id,
        date = date,
        title = "일정 $id",
        place = "",
        order = 0,
    )
}
