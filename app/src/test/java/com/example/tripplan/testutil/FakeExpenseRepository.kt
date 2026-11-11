package com.example.tripplan.testutil

import com.example.tripplan.domain.model.Expense
import com.example.tripplan.domain.repository.ExpenseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeExpenseRepository : ExpenseRepository {
    private val expenses = MutableStateFlow<List<Expense>>(emptyList())
    private var nextId = 1L

    fun setExpenses(values: List<Expense>) {
        expenses.value = values
    }

    fun values(): List<Expense> = expenses.value

    override fun observeExpenses(tripPlanId: Long): Flow<List<Expense>> = expenses

    override suspend fun getExpense(expenseId: Long): Expense? =
        expenses.value.firstOrNull { it.id == expenseId }

    override suspend fun addExpense(expense: Expense): Long {
        val id = nextId++
        expenses.value = expenses.value + expense.copy(id = id)
        return id
    }

    override suspend fun updateExpense(expense: Expense): Boolean {
        if (expenses.value.none { it.id == expense.id }) return false
        expenses.value = expenses.value.map { if (it.id == expense.id) expense else it }
        return true
    }

    override suspend fun deleteExpense(expenseId: Long): Boolean {
        if (expenses.value.none { it.id == expenseId }) return false
        expenses.value = expenses.value.filterNot { it.id == expenseId }
        return true
    }
}
