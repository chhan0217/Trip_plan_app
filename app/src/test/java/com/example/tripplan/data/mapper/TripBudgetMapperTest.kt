package com.example.tripplan.data.mapper

import com.example.tripplan.data.local.BudgetEntryEntity
import com.example.tripplan.data.local.TripBudgetEntity
import com.example.tripplan.data.local.TripBudgetWithEntries
import com.example.tripplan.domain.model.BudgetEntryType
import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Test

class TripBudgetMapperTest {
    @Test
    fun toDomain_preservesLockedAmountsAndOrdersEntries() {
        val mapped = TripBudgetWithEntries(
            budget = TripBudgetEntity(3, "JPY", 100, 200),
            entries = listOf(
                entry(id = 2, type = "Additional", amount = "50000", converted = "485000", order = 1),
                entry(id = 1, type = "Initial", amount = "300000", converted = "2856000", order = 0),
            ),
        ).toDomain()

        assertEquals("JPY", mapped.currencyCode)
        assertEquals(listOf(BudgetEntryType.Initial, BudgetEntryType.Additional), mapped.entries.map { it.type })
        assertEquals(BigDecimal("350000"), mapped.totalAmount)
        assertEquals(BigDecimal("3341000"), mapped.totalConvertedKrwAmount)
        assertEquals("9.52", mapped.entries.first().krwPerUnit.toPlainString())
    }

    private fun entry(
        id: Long,
        type: String,
        amount: String,
        converted: String,
        order: Int,
    ) = BudgetEntryEntity(
        id = id,
        tripPlanId = 3,
        type = type,
        amount = amount,
        krwPerUnit = if (type == "Initial") "9.52" else "9.70",
        rateDate = "2026-11-11",
        convertedKrwAmount = converted,
        memo = "",
        sortOrder = order,
        createdAt = 100,
        updatedAt = 100,
    )
}
