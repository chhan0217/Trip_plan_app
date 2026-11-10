package com.example.tripplan.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TripPlanDao {
    @Query("SELECT * FROM trip_plans ORDER BY startDate ASC, id ASC")
    fun observeTripPlans(): Flow<List<TripPlanEntity>>

    @Query("SELECT * FROM trip_plans WHERE id = :tripPlanId")
    fun observeTripPlan(tripPlanId: Long): Flow<TripPlanEntity?>

    @Query("SELECT * FROM trip_plans WHERE id = :tripPlanId")
    suspend fun getTripPlan(tripPlanId: Long): TripPlanEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertTripPlan(tripPlan: TripPlanEntity): Long

    @Update
    suspend fun updateTripPlan(tripPlan: TripPlanEntity)

    @Query(
        """
        UPDATE trip_plans
        SET completedAt = :completedAt, updatedAt = :updatedAt
        WHERE id = :tripPlanId
        """
    )
    suspend fun updateTripCompletion(
        tripPlanId: Long,
        completedAt: Long?,
        updatedAt: Long,
    ): Int

    @Query("DELETE FROM trip_plans WHERE id = :tripPlanId")
    suspend fun deleteTripPlan(tripPlanId: Long)

    @Query("DELETE FROM itinerary_items WHERE id IN (:itemIds)")
    suspend fun deleteItineraryItemsByIds(itemIds: List<Long>)

    @Transaction
    suspend fun updateTripPlanAndDeleteItineraryItems(
        tripPlan: TripPlanEntity,
        itemIds: List<Long>,
    ) {
        updateTripPlan(tripPlan)
        if (itemIds.isNotEmpty()) deleteItineraryItemsByIds(itemIds)
    }
}
