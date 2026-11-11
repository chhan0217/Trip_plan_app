package com.example.tripplan.data.mapper

import com.example.tripplan.data.local.ItineraryItemEntity
import com.example.tripplan.domain.model.ItineraryItem
import com.example.tripplan.domain.model.ItineraryCost
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Test

class ItineraryItemMapperTest {
    @Test
    fun entityToDomain_mapsAllFields() {
        val entity = ItineraryItemEntity(
            id = 3L,
            tripPlanId = 2L,
            date = "2026-11-10",
            title = "성산일출봉",
            place = "제주 서귀포시",
            startTime = "09:30",
            endTime = "11:00",
            note = "운동화 준비",
            sortOrder = 1,
            placeId = 9L,
        )

        val result = entity.toDomain()

        assertEquals(
            ItineraryItem(
                id = 3L,
                tripPlanId = 2L,
                date = LocalDate.of(2026, 11, 10),
                title = "성산일출봉",
                place = "제주 서귀포시",
                startTime = LocalTime.of(9, 30),
                endTime = LocalTime.of(11, 0),
                note = "운동화 준비",
                order = 1,
                placeId = 9L,
            ),
            result,
        )
    }

    @Test
    fun domainToEntity_preservesNullTimes() {
        val item = ItineraryItem(
            id = 4L,
            tripPlanId = 2L,
            date = LocalDate.of(2026, 11, 11),
            title = "자유 시간",
            place = "",
            startTime = null,
            endTime = null,
            note = "",
            order = 2,
            placeId = 12L,
        )

        val result = item.toEntity()

        assertEquals(
            ItineraryItemEntity(
                id = 4L,
                tripPlanId = 2L,
                date = "2026-11-11",
                title = "자유 시간",
                place = "",
                startTime = null,
                endTime = null,
                note = "",
                sortOrder = 2,
                placeId = 12L,
            ),
            result,
        )
    }

    @Test
    fun costSnapshots_roundTripWithoutUsingLatestRate() {
        val cost = ItineraryCost(
            amount = BigDecimal("18000"),
            currencyCode = "JPY",
            krwPerUnit = BigDecimal("9.52"),
            rateDate = LocalDate.of(2026, 11, 11),
            convertedKrwAmount = BigDecimal("171360"),
        )
        val item = ItineraryItem(
            id = 5,
            tripPlanId = 2,
            date = LocalDate.of(2026, 11, 11),
            title = "식사",
            place = "도쿄",
            order = 0,
            estimatedCost = cost.copy(amount = BigDecimal("15000"), convertedKrwAmount = BigDecimal("142800")),
            actualCost = cost,
        )

        assertEquals(item, item.toEntity().toDomain())
    }
}
