package com.example.tripplan.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface LodgingDao {
    @Query("SELECT * FROM lodgings WHERE tripPlanId = :tripId ORDER BY checkIn, id")
    fun observe(tripId: Long): Flow<List<LodgingEntity>>

    @Query("SELECT * FROM lodgings WHERE tripPlanId = :tripId")
    suspend fun getAll(tripId: Long): List<LodgingEntity>

    @Insert
    suspend fun insert(lodging: LodgingEntity)

    @Update
    suspend fun update(lodging: LodgingEntity): Int

    @Query("DELETE FROM lodgings WHERE tripPlanId = :tripId AND id = :id")
    suspend fun delete(tripId: Long, id: Long): Int
}
