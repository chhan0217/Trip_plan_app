package com.example.tripplan.domain.repository

import com.example.tripplan.domain.model.BudgetEntry
import com.example.tripplan.domain.model.TripBudget
import kotlinx.coroutines.flow.Flow

interface TripBudgetRepository {
    fun observeBudget(tripPlanId: Long): Flow<TripBudget?>
    suspend fun getBudget(tripPlanId: Long): TripBudget?
    suspend fun createBudget(tripPlanId: Long, currencyCode: String, initialEntry: BudgetEntry): Long
    suspend fun addEntry(entry: BudgetEntry): Long
}
