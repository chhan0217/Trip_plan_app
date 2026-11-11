package com.example.tripplan.domain.usecase

import com.example.tripplan.domain.model.AddBudgetEntryInput
import com.example.tripplan.domain.model.BudgetEntry
import com.example.tripplan.domain.model.BudgetEntryType
import com.example.tripplan.domain.model.CreateTripBudgetInput
import com.example.tripplan.domain.model.TripBudget
import com.example.tripplan.domain.repository.TripBudgetRepository
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TripBudgetUseCasesTest {
    private val instant = Instant.parse("2026-11-11T01:00:00Z")
    private val clock = Clock.fixed(instant, ZoneOffset.UTC)
    private val rateDate = LocalDate.of(2026, 11, 11)

    @Test
    fun createAndAddBudget_preservesEachInputRateAndSumsSnapshots() = runTest {
        val repository = FakeTripBudgetRepository()
        val create = CreateTripBudgetUseCase(repository, clock)
        val add = AddTripBudgetEntryUseCase(repository, clock)

        assertTrue(
            create(
                7,
                CreateTripBudgetInput("jpy", BigDecimal("300000"), BigDecimal("9.52"), rateDate),
            ) is BudgetMutationResult.Success
        )
        assertTrue(
            add(
                7,
                AddBudgetEntryInput(BigDecimal("50000"), BigDecimal("9.70"), rateDate.plusDays(1)),
            ) is BudgetMutationResult.Success
        )

        val budget = repository.getBudget(7)!!
        assertEquals("JPY", budget.currencyCode)
        assertEquals(listOf(BudgetEntryType.Initial, BudgetEntryType.Additional), budget.entries.map { it.type })
        assertEquals(BigDecimal("36667.25"), budget.totalAmount)
        assertEquals(BigDecimal("350000"), budget.totalConvertedKrwAmount)
        assertEquals(listOf("9.52", "9.70"), budget.entries.map { it.krwPerUnit.toPlainString() })
    }

    @Test
    fun createBudget_rejectsSecondInitialBudget() = runTest {
        val repository = FakeTripBudgetRepository()
        val useCase = CreateTripBudgetUseCase(repository, clock)
        val input = CreateTripBudgetInput("USD", BigDecimal("1000"), BigDecimal("1460"), rateDate)

        assertTrue(useCase(1, input) is BudgetMutationResult.Success)
        assertEquals(BudgetMutationResult.BudgetAlreadyExists, useCase(1, input))
    }

    private class FakeTripBudgetRepository : TripBudgetRepository {
        private val budgets = mutableMapOf<Long, MutableStateFlow<TripBudget?>>()
        private var nextId = 1L

        override fun observeBudget(tripPlanId: Long): Flow<TripBudget?> =
            budgets.getOrPut(tripPlanId) { MutableStateFlow(null) }

        override suspend fun getBudget(tripPlanId: Long): TripBudget? = budgets[tripPlanId]?.value

        override suspend fun createBudget(
            tripPlanId: Long,
            currencyCode: String,
            initialEntry: BudgetEntry,
        ): Long {
            val id = nextId++
            budgets.getOrPut(tripPlanId) { MutableStateFlow(null) }.value = TripBudget(
                tripPlanId = tripPlanId,
                currencyCode = currencyCode,
                entries = listOf(initialEntry.copy(id = id)),
                createdAt = initialEntry.createdAt,
                updatedAt = initialEntry.updatedAt,
            )
            return id
        }

        override suspend fun addEntry(entry: BudgetEntry): Long {
            val id = nextId++
            val flow = budgets.getValue(entry.tripPlanId)
            val current = flow.value!!
            flow.value = current.copy(
                entries = current.entries + entry.copy(id = id),
                updatedAt = entry.updatedAt,
            )
            return id
        }
    }
}
