package com.example.tripplan.data.repository

import com.example.tripplan.data.local.ExpenseDao
import com.example.tripplan.data.mapper.toDomain
import com.example.tripplan.data.mapper.toEntity
import com.example.tripplan.domain.model.Expense
import com.example.tripplan.domain.repository.ExpenseRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class DefaultExpenseRepository @Inject constructor(
    private val dao: ExpenseDao,
) : ExpenseRepository {
    override fun observeExpenses(tripPlanId: Long): Flow<List<Expense>> =
        dao.observeExpenses(tripPlanId).map { entities -> entities.map { it.toDomain() } }

    override suspend fun getExpense(expenseId: Long): Expense? =
        dao.getExpense(expenseId)?.toDomain()

    override suspend fun addExpense(expense: Expense): Long = dao.insertExpense(expense.toEntity())

    override suspend fun updateExpense(expense: Expense): Boolean =
        dao.updateExpense(expense.toEntity()) == 1

    override suspend fun deleteExpense(expenseId: Long): Boolean =
        dao.deleteExpense(expenseId) == 1
}
