package com.example.tripplan.presentation.itinerarymap

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class ItineraryMapUiStateTest {
    private val date = LocalDate.of(2026, 12, 1)

    @Test
    fun `missing coordinate splits planned line`() {
        val state = ItineraryMapUiState(
            items = listOf(item(1, 1.0, 1.0), item(2, 2.0, 2.0), item(3, null, null), item(4, 4.0, 4.0), item(5, 5.0, 5.0)),
        )
        assertEquals(listOf(listOf(1L, 2L), listOf(4L, 5L)), state.lineSegments.map { it.map(MapItemUiModel::id) })
    }

    @Test
    fun `lines never connect different days`() {
        val nextDate = date.plusDays(1)
        val state = ItineraryMapUiState(
            items = listOf(item(1, 1.0, 1.0), item(2, 2.0, 2.0), item(3, 3.0, 3.0, nextDate, 2), item(4, 4.0, 4.0, nextDate, 2)),
        )
        assertEquals(listOf(listOf(1L, 2L), listOf(3L, 4L)), state.lineSegments.map { it.map(MapItemUiModel::id) })
    }

    private fun item(id: Long, latitude: Double?, longitude: Double?, itemDate: LocalDate = date, dayNumber: Int = 1) =
        MapItemUiModel(id, itemDate, dayNumber, id.toString(), "일정 $id", "장소 $id", "", null, latitude, longitude)
}
