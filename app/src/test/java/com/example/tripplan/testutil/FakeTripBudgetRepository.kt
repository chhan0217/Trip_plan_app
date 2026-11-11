package com.example.tripplan.testutil

import com.example.tripplan.domain.model.BudgetEntry
import com.example.tripplan.domain.model.TripBudget
import com.example.tripplan.domain.repository.TripBudgetRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeTripBudgetRepository : TripBudgetRepository {
    private val budgets = mutableMapOf<Long, MutableStateFlow<TripBudget?>>()
    private var nextId = 1L

    fun setBudget(value: TripBudget?) {
        if (value != null) budgets.getOrPut(value.tripPlanId) { MutableStateFlow(null) }.value = value
    }

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
            tripPlanId,
            currencyCode,
            listOf(initialEntry.copy(id = id)),
            initialEntry.createdAt,
            initialEntry.updatedAt,
        )
        return id
    }

    override suspend fun addEntry(entry: BudgetEntry): Long {
        val id = nextId++
        val flow = budgets.getValue(entry.tripPlanId)
        val current = flow.value ?: error("Budget missing")
        flow.value = current.copy(
            entries = current.entries + entry.copy(id = id),
            updatedAt = entry.updatedAt,
        )
        return id
    }
}
