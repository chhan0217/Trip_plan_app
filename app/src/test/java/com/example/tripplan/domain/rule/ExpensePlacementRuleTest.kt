package com.example.tripplan.domain.rule

import com.example.tripplan.domain.model.ItineraryItem
import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ExpensePlacementRuleTest {
    private val date = LocalDate.of(2026, 11, 11)

    @Test
    fun timeInsideSchedule_returnsScheduleId() {
        val item = item(3, "09:00", "10:30")

        assertEquals(
            3L,
            ExpensePlacementRule.findItineraryItemId(
                listOf(item),
                date,
                LocalTime.of(9, 40),
            ),
        )
    }

    @Test
    fun timeBetweenSchedules_returnsNull() {
        val items = listOf(item(1, "09:00", "10:00"), item(2, "11:00", "12:00"))

        assertNull(
            ExpensePlacementRule.findItineraryItemId(
                items,
                date,
                LocalTime.of(10, 30),
            )
        )
    }

    @Test
    fun exactEndTime_isNotIncluded() {
        assertNull(
            ExpensePlacementRule.findItineraryItemId(
                listOf(item(1, "09:00", "10:00")),
                date,
                LocalTime.of(10, 0),
            )
        )
    }

    private fun item(id: Long, start: String, end: String) = ItineraryItem(
        id = id,
        tripPlanId = 1,
        date = date,
        title = "일정 $id",
        place = "",
        startTime = LocalTime.parse(start),
        endTime = LocalTime.parse(end),
        order = id.toInt(),
    )
}
