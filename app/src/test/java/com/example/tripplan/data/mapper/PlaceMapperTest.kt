package com.example.tripplan.data.mapper

import com.example.tripplan.data.local.PlaceEntity
import com.example.tripplan.domain.model.PlaceSource
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Test

class PlaceMapperTest {
    @Test
    fun entityAndDomain_roundTripEveryField() {
        val entity = PlaceEntity(
            id = 5L,
            name = "제주국제공항",
            address = "제주시 공항로 2",
            latitude = 33.5104,
            longitude = 126.4914,
            source = PlaceSource.ExternalProvider.name,
            providerName = "google",
            externalId = "provider-id",
            createdAt = 1000L,
            updatedAt = 2000L,
        )

        val domain = entity.toDomain()

        assertEquals(PlaceSource.ExternalProvider, domain.source)
        assertEquals(Instant.ofEpochMilli(1000L), domain.createdAt)
        assertEquals(entity, domain.toEntity())
    }
}
