package com.example.tripplan.domain.repository

import com.example.tripplan.domain.model.Expense
import kotlinx.coroutines.flow.Flow

interface ExpenseRepository {
    fun observeExpenses(tripPlanId: Long): Flow<List<Expense>>
    suspend fun getExpense(expenseId: Long): Expense?
    suspend fun addExpense(expense: Expense): Long
    suspend fun updateExpense(expense: Expense): Boolean
    suspend fun deleteExpense(expenseId: Long): Boolean
}
