package com.example.tripplan.data.mapper

import com.example.tripplan.data.local.TripPlanEntity
import com.example.tripplan.domain.model.TripPlan
import org.junit.Assert.assertEquals
import org.junit.Test

class TripPlanMapperTest {
    @Test
    fun entityToDomain_mapsAllFields() {
        val entity = TripPlanEntity(
            id = 1L,
            title = "Seoul Trip",
            startDate = "2026-08-01",
            endDate = "2026-08-03",
        )

        val result = entity.toDomain()

        assertEquals(
            TripPlan(
                id = 1L,
                title = "Seoul Trip",
                startDate = "2026-08-01",
                endDate = "2026-08-03",
            ),
            result,
        )
    }

    @Test
    fun domainToEntity_mapsAllFields() {
        val tripPlan = TripPlan(
            id = 2L,
            title = "Busan Trip",
            startDate = "2026-09-10",
            endDate = "2026-09-12",
        )

        val result = tripPlan.toEntity()

        assertEquals(
            TripPlanEntity(
                id = 2L,
                title = "Busan Trip",
                startDate = "2026-09-10",
                endDate = "2026-09-12",
            ),
            result,
        )
    }
}
