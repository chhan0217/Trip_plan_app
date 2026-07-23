package com.example.tripplan.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        TripPlanEntity::class,
        RegionEntity::class,
    ],
    version = 2,
)
abstract class TripPlanDatabase : RoomDatabase() {
    abstract fun tripPlanDao(): TripPlanDao
    abstract fun regionDao(): RegionDao
}
