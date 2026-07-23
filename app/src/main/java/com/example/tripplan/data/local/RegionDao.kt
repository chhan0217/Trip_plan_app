package com.example.tripplan.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface RegionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(regions: List<RegionEntity>)

    @Query("DELETE FROM regions")
    suspend fun clear()

    @Query("SELECT COUNT(*) FROM regions")
    suspend fun count(): Int

    @Query(
        """
        SELECT * FROM regions
        WHERE :query != ''
            AND (
                placeName LIKE '%' || :query || '%'
                OR provinceName LIKE '%' || :query || '%'
                OR cityName LIKE '%' || :query || '%'
                OR districtName LIKE '%' || :query || '%'
            )
        GROUP BY provinceName, cityName, districtName, placeName
        ORDER BY provinceName ASC, cityName ASC, districtName ASC, placeName ASC
        LIMIT :limit
        """
    )
    suspend fun searchRegions(query: String, limit: Int): List<RegionEntity>
}
