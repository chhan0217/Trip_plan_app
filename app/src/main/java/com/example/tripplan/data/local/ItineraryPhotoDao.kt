package com.example.tripplan.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

data class ItineraryPhotoRow(val id: Long, val itineraryId: Long)

@Dao
interface ItineraryPhotoDao {
    @Query("""
        SELECT photo.id, photo.itineraryId
        FROM itinerary_photos photo
        INNER JOIN itinerary_items item ON item.id = photo.itineraryId
        WHERE item.tripPlanId = :tripId
        ORDER BY photo.id
    """)
    fun observe(tripId: Long): Flow<List<ItineraryPhotoRow>>

    @Query("""
        SELECT photo.jpeg FROM itinerary_photos photo
        INNER JOIN itinerary_items item ON item.id = photo.itineraryId
        WHERE photo.id = :id AND item.tripPlanId = :tripId
    """)
    suspend fun getJpeg(tripId: Long, id: Long): ByteArray?

    @Insert
    suspend fun insert(photo: ItineraryPhotoEntity)
}
