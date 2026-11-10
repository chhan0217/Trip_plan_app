package com.example.tripplan.data.mapper

import com.example.tripplan.data.local.PackingItemEntity
import com.example.tripplan.domain.model.PackingItemType
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Test

class PackingItemMapperTest {
    @Test
    fun entityAndDomain_roundTripEveryField() {
        val entity = PackingItemEntity(
            id = 3L,
            tripPlanId = 7L,
            name = "여권",
            category = "서류",
            quantity = 1,
            memo = "유효기간 확인",
            type = PackingItemType.Shared.name,
            checkedAt = 1000L,
            sortOrder = 2,
        )

        val domain = entity.toDomain()

        assertEquals(PackingItemType.Shared, domain.type)
        assertEquals(Instant.ofEpochMilli(1000L), domain.checkedAt)
        assertEquals(entity, domain.toEntity())
    }

    @Test(expected = IllegalArgumentException::class)
    fun unknownStoredType_isRejected() {
        PackingItemEntity(0, 1, "항목", "", 1, "", "Unknown", null, 0).toDomain()
    }
}
