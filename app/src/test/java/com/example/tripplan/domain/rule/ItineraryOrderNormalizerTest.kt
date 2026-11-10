package com.example.tripplan.domain.rule

import com.example.tripplan.domain.model.ItineraryItem
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class ItineraryOrderNormalizerTest {
    @Test
    fun normalize_sortsItemsAndAssignsContinuousOrder() {
        val items = listOf(
            item(id = 30L, order = 7),
            item(id = 20L, order = 2),
            item(id = 10L, order = 2),
        )

        val result = ItineraryOrderNormalizer.normalize(items)

        assertEquals(listOf(10L, 20L, 30L), result.map { it.id })
        assertEquals(listOf(0, 1, 2), result.map { it.order })
    }

    private fun item(id: Long, order: Int) = ItineraryItem(
        id = id,
        tripPlanId = 1L,
        date = LocalDate.of(2026, 11, 9),
        title = "일정 $id",
        place = "장소",
        order = order,
    )
}
