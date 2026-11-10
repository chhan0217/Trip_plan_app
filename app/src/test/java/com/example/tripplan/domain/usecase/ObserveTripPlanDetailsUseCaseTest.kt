package com.example.tripplan.domain.usecase

import com.example.tripplan.domain.model.ItineraryItem
import com.example.tripplan.domain.model.TripPlan
import com.example.tripplan.testutil.FakeItineraryRepository
import com.example.tripplan.testutil.FakeTripPlanRepository
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ObserveTripPlanDetailsUseCaseTest {
    @Test
    fun existingTrip_combinesCalculatedDaysAndSortedItems() = runTest {
        val tripRepository = FakeTripPlanRepository().apply { setTrips(listOf(sampleTrip)) }
        val itineraryRepository = FakeItineraryRepository().apply {
            setItems(
                listOf(
                    item(2L, "2026-11-10", 0),
                    item(1L, "2026-11-09", 1),
                )
            )
        }

        val details = ObserveTripPlanDetailsUseCase(tripRepository, itineraryRepository)(7L)
            .first()

        requireNotNull(details)
        assertEquals(listOf(1, 2, 3), details.days.map { it.dayNumber })
        assertEquals(listOf(1L, 2L), details.itineraryItems.map { it.id })
    }

    @Test
    fun missingTrip_returnsNull() = runTest {
        val details = ObserveTripPlanDetailsUseCase(
            FakeTripPlanRepository(),
            FakeItineraryRepository(),
        )(99L).first()

        assertNull(details)
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

    private fun item(id: Long, date: String, order: Int) = ItineraryItem(
        id = id,
        tripPlanId = 7L,
        date = LocalDate.parse(date),
        title = "일정 $id",
        place = "제주",
        order = order,
    )
}
