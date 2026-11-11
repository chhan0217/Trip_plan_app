package com.example.tripplan.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {
    @Query(
        """
        SELECT * FROM expenses
        WHERE tripPlanId = :tripPlanId
        ORDER BY occurredDate ASC, occurredTimeMinutes ASC, id ASC
        """
    )
    fun observeExpenses(tripPlanId: Long): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses WHERE id = :expenseId LIMIT 1")
    suspend fun getExpense(expenseId: Long): ExpenseEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertExpense(expense: ExpenseEntity): Long

    @Update
    suspend fun updateExpense(expense: ExpenseEntity): Int

    @Query("DELETE FROM expenses WHERE id = :expenseId")
    suspend fun deleteExpense(expenseId: Long): Int
}
