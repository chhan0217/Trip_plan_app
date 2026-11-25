package com.example.tripplan.data.local

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

data class PlaceCandidateWithDate(
    @Embedded val candidate: PlaceCandidateEntity,
    val scheduledDate: String?,
)

@Dao
interface PlaceCandidateDao {
    @Query("""
        SELECT candidate.*, itinerary.date AS scheduledDate
        FROM place_candidates AS candidate
        LEFT JOIN itinerary_items AS itinerary ON candidate.itineraryId = itinerary.id
        WHERE candidate.tripPlanId = :tripId
        ORDER BY candidate.id DESC
    """)
    fun observe(tripId: Long): Flow<List<PlaceCandidateWithDate>>

    @Query("SELECT * FROM place_candidates WHERE tripPlanId = :tripId AND id = :id")
    suspend fun get(tripId: Long, id: Long): PlaceCandidateEntity?

    @Insert
    suspend fun insert(candidate: PlaceCandidateEntity)

    @Query("UPDATE place_candidates SET itineraryId = :itineraryId WHERE id = :id")
    suspend fun link(id: Long, itineraryId: Long)

    @Query("DELETE FROM place_candidates WHERE tripPlanId = :tripId AND id = :id")
    suspend fun delete(tripId: Long, id: Long): Int
}
