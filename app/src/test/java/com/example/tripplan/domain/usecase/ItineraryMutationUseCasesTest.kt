package com.example.tripplan.domain.usecase

import com.example.tripplan.domain.model.ItineraryItem
import com.example.tripplan.domain.model.ItineraryItemInput
import com.example.tripplan.domain.model.TripPlan
import com.example.tripplan.domain.rule.ItineraryItemValidationError
import com.example.tripplan.testutil.FakeItineraryRepository
import com.example.tripplan.testutil.FakeTripPlanRepository
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ItineraryMutationUseCasesTest {
    @Test
    fun create_appendsToSelectedDayAndTrimsText() = runTest {
        val trips = FakeTripPlanRepository().apply { setTrips(listOf(sampleTrip)) }
        val items = FakeItineraryRepository().apply {
            nextCreatedId = 9L
            setItems(listOf(item(1L, firstDate, 0)))
        }

        val result = CreateItineraryItemUseCase(trips, items)(
            sampleTrip.id,
            input(firstDate).copy(title = "  새 일정  ", place = "  제주  "),
        )

        assertEquals(ItineraryMutationResult.Success(9L), result)
        val created = items.getItem(9L)
        assertEquals("새 일정", created?.title)
        assertEquals("제주", created?.place)
        assertEquals(1, created?.order)
    }

    @Test
    fun create_invalidTitle_doesNotSave() = runTest {
        val trips = FakeTripPlanRepository().apply { setTrips(listOf(sampleTrip)) }
        val items = FakeItineraryRepository()

        val result = CreateItineraryItemUseCase(trips, items)(
            sampleTrip.id,
            input(firstDate).copy(title = " "),
        )

        assertEquals(
            ItineraryMutationResult.Invalid(setOf(ItineraryItemValidationError.BlankTitle)),
            result,
        )
        assertTrue(items.observeItems(sampleTrip.id).first().isEmpty())
    }

    @Test
    fun create_reordersDayByStartTimeAndPlacesUntimedItemLast() = runTest {
        val trips = FakeTripPlanRepository().apply { setTrips(listOf(sampleTrip)) }
        val items = FakeItineraryRepository().apply {
            setItems(
                listOf(
                    item(1L, firstDate, 0).copy(startTime = null),
                    item(2L, firstDate, 1).copy(startTime = LocalTime.of(14, 0)),
                )
            )
        }

        CreateItineraryItemUseCase(trips, items)(
            sampleTrip.id,
            input(firstDate).copy(startTime = LocalTime.of(9, 0)),
        )

        val ordered = items.observeItems(sampleTrip.id).first().sortedBy { it.order }
        assertEquals(listOf(100L, 2L, 1L), ordered.map { it.id })
    }

    @Test
    fun update_moveToAnotherDay_normalizesBothDays() = runTest {
        val trips = FakeTripPlanRepository().apply { setTrips(listOf(sampleTrip)) }
        val items = FakeItineraryRepository().apply {
            setItems(
                listOf(
                    item(1L, firstDate, 0),
                    item(2L, firstDate, 4),
                    item(3L, secondDate, 3),
                )
            )
        }

        UpdateItineraryItemUseCase(trips, items)(2L, input(secondDate).copy(title = "이동"))

        val saved = items.observeItems(sampleTrip.id).first()
        assertEquals(0, saved.first { it.id == 1L }.order)
        assertEquals(0, saved.first { it.id == 3L }.order)
        assertEquals(1, saved.first { it.id == 2L }.order)
        assertEquals(secondDate, saved.first { it.id == 2L }.date)
    }

    @Test
    fun delete_removesItemAndNormalizesRemainingOrders() = runTest {
        val items = FakeItineraryRepository().apply {
            setItems(listOf(item(1L, firstDate, 0), item(2L, firstDate, 5)))
        }

        val result = DeleteItineraryItemUseCase(items)(1L)

        assertEquals(ItineraryMutationResult.Success(1L), result)
        val remaining = items.observeItems(sampleTrip.id).first().single()
        assertEquals(2L, remaining.id)
        assertEquals(0, remaining.order)
    }

    @Test
    fun moveDown_swapsAndPersistsContinuousOrder() = runTest {
        val items = FakeItineraryRepository().apply {
            setItems(
                listOf(
                    item(1L, firstDate, 0),
                    item(2L, firstDate, 1),
                    item(3L, firstDate, 2),
                )
            )
        }

        MoveItineraryItemUseCase(items)(1L, ItineraryMoveDirection.Down)

        val reordered = items.observeItems(sampleTrip.id).first().sortedBy { it.order }
        assertEquals(listOf(2L, 1L, 3L), reordered.map { it.id })
        assertEquals(listOf(0, 1, 2), reordered.map { it.order })
    }

    private val firstDate = LocalDate.of(2026, 11, 9)
    private val secondDate = LocalDate.of(2026, 11, 10)
    private val sampleTrip = TripPlan(
        id = 7L,
        title = "제주 여행",
        destination = "제주",
        startDate = firstDate,
        endDate = LocalDate.of(2026, 11, 11),
        description = "",
        createdAt = Instant.EPOCH,
        updatedAt = Instant.EPOCH,
    )

    private fun input(date: LocalDate) = ItineraryItemInput(
        date = date,
        title = "일정",
        place = "장소",
    )

    private fun item(id: Long, date: LocalDate, order: Int) = ItineraryItem(
        id = id,
        tripPlanId = sampleTrip.id,
        date = date,
        title = "일정 $id",
        place = "장소",
        order = order,
    )
}
