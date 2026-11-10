package com.example.tripplan.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object DatabaseMigrations {
    val Migration1To2 = object : Migration(1, 2) {
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

    val Migration2To3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `trip_plans` ADD COLUMN `destination` TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE `trip_plans` ADD COLUMN `description` TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE `trip_plans` ADD COLUMN `createdAt` INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE `trip_plans` ADD COLUMN `updatedAt` INTEGER NOT NULL DEFAULT 0")
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `itinerary_items` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `tripPlanId` INTEGER NOT NULL,
                    `date` TEXT NOT NULL,
                    `title` TEXT NOT NULL,
                    `place` TEXT NOT NULL,
                    `startTime` TEXT,
                    `endTime` TEXT,
                    `note` TEXT NOT NULL,
                    `sortOrder` INTEGER NOT NULL,
                    FOREIGN KEY(`tripPlanId`) REFERENCES `trip_plans`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent()
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_itinerary_items_tripPlanId` " +
                    "ON `itinerary_items` (`tripPlanId`)"
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_itinerary_items_tripPlanId_date_sortOrder` " +
                    "ON `itinerary_items` (`tripPlanId`, `date`, `sortOrder`)"
            )
        }
    }

    val Migration3To4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `trip_plans` ADD COLUMN `completedAt` INTEGER")
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `packing_items` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `tripPlanId` INTEGER NOT NULL,
                    `name` TEXT NOT NULL,
                    `category` TEXT NOT NULL,
                    `quantity` INTEGER NOT NULL,
                    `memo` TEXT NOT NULL,
                    `type` TEXT NOT NULL,
                    `checkedAt` INTEGER,
                    `sortOrder` INTEGER NOT NULL,
                    FOREIGN KEY(`tripPlanId`) REFERENCES `trip_plans`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent()
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_packing_items_tripPlanId` " +
                    "ON `packing_items` (`tripPlanId`)"
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_packing_items_tripPlanId_type_sortOrder` " +
                    "ON `packing_items` (`tripPlanId`, `type`, `sortOrder`)"
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `places` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `name` TEXT NOT NULL,
                    `address` TEXT NOT NULL,
                    `latitude` REAL,
                    `longitude` REAL,
                    `source` TEXT NOT NULL,
                    `providerName` TEXT,
                    `externalId` TEXT,
                    `createdAt` INTEGER NOT NULL,
                    `updatedAt` INTEGER NOT NULL
                )
                """.trimIndent()
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_places_name` ON `places` (`name`)"
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_places_providerName_externalId` " +
                    "ON `places` (`providerName`, `externalId`)"
            )
            db.execSQL(
                "ALTER TABLE `itinerary_items` ADD COLUMN `placeId` INTEGER " +
                    "REFERENCES `places`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL"
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_itinerary_items_placeId` " +
                    "ON `itinerary_items` (`placeId`)"
            )
        }
    }

    val Migration4To5 = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `exchange_rates` (
                    `currencyUnit` TEXT NOT NULL,
                    `currencyName` TEXT NOT NULL,
                    `baseRate` TEXT NOT NULL,
                    `telegraphicTransferBuyingRate` TEXT NOT NULL,
                    `telegraphicTransferSellingRate` TEXT NOT NULL,
                    `rateDate` TEXT NOT NULL,
                    `fetchedAt` INTEGER NOT NULL,
                    PRIMARY KEY(`currencyUnit`)
                )
                """.trimIndent()
            )
        }
    }

    val Migration5To6 = object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // v5 values came from a different provider and used mixed display units.
            // Exchange-rate cache is disposable, so recreate only this table.
            db.execSQL("DROP TABLE IF EXISTS `exchange_rates`")
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `exchange_rates` (
                    `currencyCode` TEXT NOT NULL,
                    `currencyName` TEXT NOT NULL,
                    `krwPerUnit` TEXT NOT NULL,
                    `rateDate` TEXT NOT NULL,
                    `fetchedAt` INTEGER NOT NULL,
                    PRIMARY KEY(`currencyCode`)
                )
                """.trimIndent()
            )
        }
    }

    val All = arrayOf(
        Migration1To2,
        Migration2To3,
        Migration3To4,
        Migration4To5,
        Migration5To6,
    )
}
