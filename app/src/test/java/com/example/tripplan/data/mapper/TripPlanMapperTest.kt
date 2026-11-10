package com.example.tripplan.data.mapper

import com.example.tripplan.data.local.TripPlanEntity
import com.example.tripplan.domain.model.TripPlan
import java.time.Instant
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class TripPlanMapperTest {
    @Test
    fun entityToDomain_mapsAllFields() {
        val entity = TripPlanEntity(
            id = 1L,
            title = "Seoul Trip",
            destination = "Seoul",
            startDate = "2026-08-01",
            endDate = "2026-08-03",
            description = "Summer vacation",
            createdAt = 1000L,
            updatedAt = 2000L,
            completedAt = 3000L,
        )

        val result = entity.toDomain()

        assertEquals(
            TripPlan(
                id = 1L,
                title = "Seoul Trip",
                destination = "Seoul",
                startDate = LocalDate.of(2026, 8, 1),
                endDate = LocalDate.of(2026, 8, 3),
                description = "Summer vacation",
                createdAt = Instant.ofEpochMilli(1000L),
                updatedAt = Instant.ofEpochMilli(2000L),
                completedAt = Instant.ofEpochMilli(3000L),
            ),
            result,
        )
    }

    @Test
    fun domainToEntity_mapsAllFields() {
        val tripPlan = TripPlan(
            id = 2L,
            title = "Busan Trip",
            destination = "Busan",
            startDate = LocalDate.of(2026, 9, 10),
            endDate = LocalDate.of(2026, 9, 12),
            description = "Weekend",
            createdAt = Instant.ofEpochMilli(3000L),
            updatedAt = Instant.ofEpochMilli(4000L),
            completedAt = Instant.ofEpochMilli(5000L),
        )

        val result = tripPlan.toEntity()

        assertEquals(
            TripPlanEntity(
                id = 2L,
                title = "Busan Trip",
                destination = "Busan",
                startDate = "2026-09-10",
                endDate = "2026-09-12",
                description = "Weekend",
                createdAt = 3000L,
                updatedAt = 4000L,
                completedAt = 5000L,
            ),
            result,
        )
    }
}
