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

    @Test
    fun `google place opens by place id`() {
        val item = item(1, 16.0678, 108.2241).copy(
            placeName = "한시장",
            address = "119 Tran Phu",
            providerName = "Google Places",
            externalId = "ChIJ-place-id",
        )

        assertEquals(
            "https://www.google.com/maps/search/?api=1&query=119%20Tran%20Phu&query_place_id=ChIJ-place-id",
            item.googleMapsUrl(),
        )
    }

    @Test
    fun `place without google place id opens by coordinates`() {
        val item = item(1, 16.0678, 108.2241)

        assertEquals(
            "https://www.google.com/maps/search/?api=1&query=16.0678,108.2241",
            item.googleMapsUrl(),
        )
    }

    private fun item(id: Long, latitude: Double?, longitude: Double?, itemDate: LocalDate = date, dayNumber: Int = 1) =
        MapItemUiModel(id, itemDate, dayNumber, id.toString(), "일정 $id", "장소 $id", "", null, latitude, longitude)
}
