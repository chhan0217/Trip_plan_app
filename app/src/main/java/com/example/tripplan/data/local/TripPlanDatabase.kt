package com.example.tripplan.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        TripPlanEntity::class,
        ItineraryItemEntity::class,
        PackingItemEntity::class,
        PlaceEntity::class,
        RegionEntity::class,
        ExchangeRateEntity::class,
        TripBudgetEntity::class,
        BudgetEntryEntity::class,
        ExpenseEntity::class,
    ],
    version = 10,
)
abstract class TripPlanDatabase : RoomDatabase() {
    abstract fun tripPlanDao(): TripPlanDao
    abstract fun itineraryItemDao(): ItineraryItemDao
    abstract fun packingItemDao(): PackingItemDao
    abstract fun placeDao(): PlaceDao
    abstract fun regionDao(): RegionDao
    abstract fun exchangeRateDao(): ExchangeRateDao
    abstract fun tripBudgetDao(): TripBudgetDao
    abstract fun expenseDao(): ExpenseDao
}
