package com.example.tripplan.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PackingItemDao {
    @Query(
        """
        SELECT * FROM packing_items
        WHERE tripPlanId = :tripPlanId
        ORDER BY type ASC, sortOrder ASC, id ASC
        """
    )
    fun observeItems(tripPlanId: Long): Flow<List<PackingItemEntity>>

    @Query("SELECT * FROM packing_items WHERE id = :itemId")
    suspend fun getItem(itemId: Long): PackingItemEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertItem(item: PackingItemEntity): Long

    @Update
    suspend fun updateItem(item: PackingItemEntity)

    @Update
    suspend fun updateItems(items: List<PackingItemEntity>)

    @Query("DELETE FROM packing_items WHERE id = :itemId")
    suspend fun deleteItem(itemId: Long)
}
