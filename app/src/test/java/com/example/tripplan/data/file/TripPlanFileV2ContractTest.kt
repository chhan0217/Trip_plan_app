package com.example.tripplan.data.file

import kotlinx.serialization.json.Json
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TripPlanFileV2ContractTest {
    @Test
    fun serializedV2_containsSharedContentWithoutLocalOrCheckState() {
        val dto = TripPlanFileV2Dto(
            formatVersion = TRIP_PLAN_FILE_VERSION_2,
            trip = SharedTripDto(
                title = "제주 여행",
                destination = "제주",
                startDate = "2026-11-09",
                endDate = "2026-11-11",
                description = "",
            ),
            itineraryItems = listOf(
                SharedItineraryItemV2Dto(
                    date = "2026-11-09",
                    title = "공항 도착",
                    place = "제주국제공항",
                    placeDetails = SharedPlaceDetailsDto(
                        address = "제주시 공항로 2",
                        latitude = 33.5104,
                        longitude = 126.4914,
                    ),
                    note = "",
                    order = 0,
                )
            ),
            sharedPackingItems = listOf(
                SharedPackingItemDto("여권", "서류", 1, "유효기간 확인", 0)
            ),
        )

        val content = Json.encodeToString(TripPlanFileV2Dto.serializer(), dto)

        assertTrue(content.contains("\"formatVersion\":2"))
        assertTrue(content.contains("\"sharedPackingItems\""))
        assertTrue(content.contains("\"placeDetails\""))
        assertFalse(content.contains("tripPlanId"))
        assertFalse(content.contains("placeId"))
        assertFalse(content.contains("checked"))
        assertFalse(content.contains("completed"))
    }
}
