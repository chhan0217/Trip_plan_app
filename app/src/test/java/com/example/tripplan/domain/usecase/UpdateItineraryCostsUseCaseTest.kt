package com.example.tripplan.domain.usecase

import com.example.tripplan.domain.model.BudgetEntry
import com.example.tripplan.domain.model.BudgetEntryType
import com.example.tripplan.domain.model.ItineraryCostInput
import com.example.tripplan.domain.model.ItineraryItem
import com.example.tripplan.domain.model.TripBudget
import com.example.tripplan.testutil.FakeItineraryRepository
import com.example.tripplan.testutil.FakeTripBudgetRepository
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class UpdateItineraryCostsUseCaseTest {
    private val instant = Instant.parse("2026-11-11T01:00:00Z")
    private val clock = Clock.fixed(instant, ZoneOffset.UTC)

    @Test
    fun saveEstimatedCost_storesSnapshotAndKeepsActualSpendOutOfItinerary() = runTest {
        val items = FakeItineraryRepository().apply {
            setItems(listOf(ItineraryItem(1, 7, LocalDate.of(2026, 11, 12), "식사", "도쿄", order = 0)))
        }
        val budgets = FakeTripBudgetRepository().apply { setBudget(sampleBudget()) }
        val useCase = UpdateItineraryCostsUseCase(items, budgets, clock)

        val result = useCase(
            1,
            ItineraryCostInput(BigDecimal("15000"), "JPY", BigDecimal("9.52")),
        )

        assertEquals(ItineraryCostMutationResult.Success, result)
        val saved = items.getItem(1)!!
        assertEquals(BigDecimal("142800"), saved.estimatedCost?.convertedKrwAmount)
        assertEquals(null, saved.actualCost)
    }

    @Test
    fun saveEstimatedCost_allowsKrwAndRejectsUnrelatedCurrency() = runTest {
        val items = FakeItineraryRepository().apply {
            setItems(listOf(ItineraryItem(1, 7, LocalDate.of(2026, 11, 12), "숙소", "도쿄", order = 0)))
        }
        val budgets = FakeTripBudgetRepository().apply { setBudget(sampleBudget()) }
        val useCase = UpdateItineraryCostsUseCase(items, budgets, clock)

        assertEquals(
            ItineraryCostMutationResult.Success,
            useCase(1, ItineraryCostInput(BigDecimal("300000"), "KRW", BigDecimal.ONE)),
        )
        assertEquals(
            ItineraryCostMutationResult.InvalidCurrency,
            useCase(1, ItineraryCostInput(BigDecimal("100"), "USD", BigDecimal("1460"))),
        )
    }

    private fun sampleBudget(): TripBudget {
        val entry = BudgetEntry(
            id = 1,
            tripPlanId = 7,
            type = BudgetEntryType.Initial,
            amount = BigDecimal("300000"),
            krwPerUnit = BigDecimal("9.52"),
            rateDate = LocalDate.of(2026, 11, 11),
            convertedKrwAmount = BigDecimal("2856000"),
            memo = "",
            order = 0,
            createdAt = instant,
            updatedAt = instant,
        )
        return TripBudget(7, "JPY", listOf(entry), instant, instant)
    }
}
