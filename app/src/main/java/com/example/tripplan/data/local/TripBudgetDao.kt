package com.example.tripplan.data.local

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

data class TripBudgetWithEntries(
    @Embedded val budget: TripBudgetEntity,
    @Relation(
        parentColumn = "tripPlanId",
        entityColumn = "tripPlanId",
    )
    val entries: List<BudgetEntryEntity>,
)

@Dao
interface TripBudgetDao {
    @Transaction
    @Query("SELECT * FROM trip_budgets WHERE tripPlanId = :tripPlanId")
    fun observeBudget(tripPlanId: Long): Flow<TripBudgetWithEntries?>

    @Transaction
    @Query("SELECT * FROM trip_budgets WHERE tripPlanId = :tripPlanId")
    suspend fun getBudget(tripPlanId: Long): TripBudgetWithEntries?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertBudget(budget: TripBudgetEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertEntry(entry: BudgetEntryEntity): Long

    @Query("SELECT MAX(sortOrder) FROM budget_entries WHERE tripPlanId = :tripPlanId")
    suspend fun getMaximumOrder(tripPlanId: Long): Int?

    @Query("UPDATE trip_budgets SET updatedAt = :updatedAt WHERE tripPlanId = :tripPlanId")
    suspend fun updateBudgetTimestamp(tripPlanId: Long, updatedAt: Long)

    @Query("UPDATE trip_plans SET updatedAt = :updatedAt WHERE id = :tripPlanId")
    suspend fun updateTripTimestamp(tripPlanId: Long, updatedAt: Long)

    @Transaction
    suspend fun insertBudgetWithInitial(
        budget: TripBudgetEntity,
        initialEntry: BudgetEntryEntity,
    ): Long {
        insertBudget(budget)
        val entryId = insertEntry(initialEntry.copy(type = "Initial", sortOrder = 0))
        updateTripTimestamp(budget.tripPlanId, budget.updatedAt)
        return entryId
    }

    @Transaction
    suspend fun insertAdditionalEntry(entry: BudgetEntryEntity): Long {
        check(getBudget(entry.tripPlanId) != null) { "Trip budget does not exist" }
        val nextOrder = (getMaximumOrder(entry.tripPlanId) ?: 0) + 1
        val entryId = insertEntry(
            entry.copy(type = "Additional", sortOrder = nextOrder)
        )
        updateBudgetTimestamp(entry.tripPlanId, entry.updatedAt)
        updateTripTimestamp(entry.tripPlanId, entry.updatedAt)
        return entryId
    }
}
