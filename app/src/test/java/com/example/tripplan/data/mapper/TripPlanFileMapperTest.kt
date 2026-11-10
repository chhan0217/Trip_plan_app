package com.example.tripplan.data.mapper

import com.example.tripplan.data.file.SharedItineraryItemDto
import com.example.tripplan.data.file.SharedTripDto
import com.example.tripplan.data.local.ItineraryItemEntity
import com.example.tripplan.data.local.TripPlanEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class TripPlanFileMapperTest {
    @Test
    fun toFileDto_excludesLocalIdentityAndKeepsOrder() {
        val trip = TripPlanEntity(
            id = 99L,
            title = "제주 여행",
            destination = "제주",
            startDate = "2026-11-09",
            endDate = "2026-11-11",
            description = "메모",
            createdAt = 1L,
            updatedAt = 2L,
        )
        val item = ItineraryItemEntity(
            id = 77L,
            tripPlanId = 99L,
            date = "2026-11-09",
            title = "성산일출봉",
            place = "제주",
            startTime = "09:00",
            endTime = "10:00",
            note = "입장권",
            sortOrder = 3,
        )

        val dto = TripPlanFileMapper.toFileDto(trip, listOf(item))

        assertEquals(1, dto.formatVersion)
        assertEquals("제주 여행", dto.trip.title)
        assertEquals(3, dto.itineraryItems.single().order)
        assertFalse(dto.toString().contains("99"))
        assertFalse(dto.toString().contains("77"))
    }

    @Test
    fun importMappers_assignNewTripIdentityAndTrimText() {
        val trip = TripPlanFileMapper.toTripEntity(
            SharedTripDto(" 제주 ", " 제주도 ", "2026-11-09", "2026-11-11", " 메모 "),
            now = 10L,
        )
        val item = TripPlanFileMapper.toItineraryEntity(
            SharedItineraryItemDto("2026-11-09", " 일정 ", " 장소 ", null, null, " 메모 ", 2),
            tripPlanId = 123L,
        )

        assertEquals(0L, trip.id)
        assertEquals("제주", trip.title)
        assertEquals(123L, item.tripPlanId)
        assertEquals("일정", item.title)
        assertEquals(2, item.sortOrder)
    }
}
