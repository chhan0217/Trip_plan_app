package com.example.tripplan.domain.rule

import com.example.tripplan.domain.model.ItineraryItem
import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Test

class ItineraryTimeOrderNormalizerTest {
    @Test
    fun normalize_sortsByStartTimeAndPlacesUntimedItemsLast() {
        val result = ItineraryTimeOrderNormalizer.normalize(
            listOf(
                item(1L, null, 0),
                item(2L, LocalTime.of(14, 0), 1),
                item(3L, LocalTime.of(9, 30), 2),
            )
        )

        assertEquals(listOf(3L, 2L, 1L), result.map { it.id })
        assertEquals(listOf(0, 1, 2), result.map { it.order })
    }

    @Test
    fun normalize_sameStartTime_preservesExistingUserOrder() {
        val time = LocalTime.of(10, 0)
        val result = ItineraryTimeOrderNormalizer.normalize(
            listOf(item(1L, time, 5), item(2L, time, 2))
        )

        assertEquals(listOf(2L, 1L), result.map { it.id })
    }

    private fun item(id: Long, startTime: LocalTime?, order: Int) = ItineraryItem(
        id = id,
        tripPlanId = 7L,
        date = LocalDate.of(2026, 11, 9),
        title = "일정 $id",
        place = "제주",
        startTime = startTime,
        order = order,
    )
}
