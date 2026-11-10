package com.example.tripplan.domain.rule

import com.example.tripplan.domain.model.PackingItemInput
import com.example.tripplan.domain.model.PackingItemType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PackingItemValidatorTest {
    @Test
    fun validSharedItem_returnsNoErrors() {
        val errors = PackingItemValidator.validate(
            PackingItemInput("여권", "서류", 1, "유효기간 확인", PackingItemType.Shared)
        )

        assertTrue(errors.isEmpty())
    }

    @Test
    fun blankNameAndZeroQuantity_returnsBothErrors() {
        val errors = PackingItemValidator.validate(
            PackingItemInput("  ", quantity = 0, type = PackingItemType.Personal)
        )

        assertEquals(
            setOf(
                PackingItemValidationError.BlankName,
                PackingItemValidationError.InvalidQuantity,
            ),
            errors,
        )
    }
}
