package com.example.tripplan.data.local

import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TripPlanDao {
    @Query("SELECT * FROM trip_plans ORDER BY startDate ASC, id ASC")
    fun observeTripPlans(): Flow<List<TripPlanEntity>>
}
