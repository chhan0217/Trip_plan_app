package com.example.tripplan.di

import android.content.Context
import androidx.room.Room
import com.example.tripplan.data.local.DatabaseMigrations
import com.example.tripplan.data.local.ExchangeRateDao
import com.example.tripplan.data.local.ItineraryItemDao
import com.example.tripplan.data.local.PackingItemDao
import com.example.tripplan.data.local.PlaceDao
import com.example.tripplan.data.local.RegionDao
import com.example.tripplan.data.local.TripPlanDao
import com.example.tripplan.data.local.TripPlanDatabase
import com.example.tripplan.data.local.TripBudgetDao
import com.example.tripplan.data.local.ExpenseDao
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
        ).addMigrations(*DatabaseMigrations.All)
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

    @Provides
    fun provideItineraryItemDao(database: TripPlanDatabase): ItineraryItemDao {
        return database.itineraryItemDao()
    }

    @Provides
    fun providePackingItemDao(database: TripPlanDatabase): PackingItemDao = database.packingItemDao()

    @Provides
    fun providePlaceDao(database: TripPlanDatabase): PlaceDao = database.placeDao()

    @Provides
    fun provideExchangeRateDao(database: TripPlanDatabase): ExchangeRateDao = database.exchangeRateDao()

    @Provides
    fun provideTripBudgetDao(database: TripPlanDatabase): TripBudgetDao = database.tripBudgetDao()

    @Provides
    fun provideExpenseDao(database: TripPlanDatabase): ExpenseDao = database.expenseDao()
}
