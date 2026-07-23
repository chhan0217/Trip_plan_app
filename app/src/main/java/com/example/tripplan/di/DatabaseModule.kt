package com.example.tripplan.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.tripplan.data.local.RegionDao
import com.example.tripplan.data.local.TripPlanDao
import com.example.tripplan.data.local.TripPlanDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideTripPlanDatabase(
        @ApplicationContext context: Context,
    ): TripPlanDatabase {
        return Room.databaseBuilder(
            context,
            TripPlanDatabase::class.java,
            "trip_plan.db",
        ).addMigrations(MIGRATION_1_2)
            .build()
    }

    @Provides
    fun provideTripPlanDao(database: TripPlanDatabase): TripPlanDao {
        return database.tripPlanDao()
    }

    @Provides
    fun provideRegionDao(database: TripPlanDatabase): RegionDao {
        return database.regionDao()
    }

    private val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `regions` (
                    `id` TEXT NOT NULL,
                    `placeName` TEXT NOT NULL,
                    `provinceName` TEXT NOT NULL,
                    `provinceCode` TEXT NOT NULL,
                    `cityName` TEXT NOT NULL,
                    `cityCode` TEXT NOT NULL,
                    `districtName` TEXT NOT NULL,
                    `districtCode` TEXT NOT NULL,
                    `latitude` REAL NOT NULL,
                    `longitude` REAL NOT NULL,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent()
            )
        }
    }
}
