package com.example.tripplan.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ItineraryItemDao {
    @Query(
        """
        SELECT * FROM itinerary_items
        WHERE tripPlanId = :tripPlanId
        ORDER BY date ASC, sortOrder ASC, id ASC
        """
    )
    fun observeItems(tripPlanId: Long): Flow<List<ItineraryItemEntity>>

    @Query(
        """
        SELECT * FROM itinerary_items
        WHERE tripPlanId = :tripPlanId AND date = :date
        ORDER BY sortOrder ASC, id ASC
        """
    )
    fun observeItemsByDate(
        tripPlanId: Long,
        date: String,
    ): Flow<List<ItineraryItemEntity>>

    @Query("SELECT * FROM itinerary_items WHERE id = :itemId")
    suspend fun getItem(itemId: Long): ItineraryItemEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertItem(item: ItineraryItemEntity): Long

    @Update
    suspend fun updateItem(item: ItineraryItemEntity)

    @Update
    suspend fun updateItems(items: List<ItineraryItemEntity>)

    @Query("DELETE FROM itinerary_items WHERE id = :itemId")
    suspend fun deleteItem(itemId: Long)

    @Transaction
    suspend fun deleteItemAndUpdateItems(
        itemId: Long,
        items: List<ItineraryItemEntity>,
    ) {
        deleteItem(itemId)
        if (items.isNotEmpty()) updateItems(items)
    }
}
