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

    val Migration6To7 = object : Migration(6, 7) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `trip_plans` ADD COLUMN `countryCode` TEXT NOT NULL DEFAULT 'KR'")
            db.execSQL("ALTER TABLE `trip_plans` ADD COLUMN `currencyCode` TEXT NOT NULL DEFAULT 'KRW'")
            db.execSQL("ALTER TABLE `itinerary_items` ADD COLUMN `estimatedAmount` TEXT")
            db.execSQL("ALTER TABLE `itinerary_items` ADD COLUMN `estimatedCurrencyCode` TEXT")
            db.execSQL("ALTER TABLE `itinerary_items` ADD COLUMN `estimatedKrwPerUnit` TEXT")
            db.execSQL("ALTER TABLE `itinerary_items` ADD COLUMN `estimatedRateDate` TEXT")
            db.execSQL("ALTER TABLE `itinerary_items` ADD COLUMN `estimatedConvertedKrwAmount` TEXT")
            db.execSQL("ALTER TABLE `itinerary_items` ADD COLUMN `actualAmount` TEXT")
            db.execSQL("ALTER TABLE `itinerary_items` ADD COLUMN `actualCurrencyCode` TEXT")
            db.execSQL("ALTER TABLE `itinerary_items` ADD COLUMN `actualKrwPerUnit` TEXT")
            db.execSQL("ALTER TABLE `itinerary_items` ADD COLUMN `actualRateDate` TEXT")
            db.execSQL("ALTER TABLE `itinerary_items` ADD COLUMN `actualConvertedKrwAmount` TEXT")
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `trip_budgets` (
                    `tripPlanId` INTEGER NOT NULL,
                    `currencyCode` TEXT NOT NULL,
                    `createdAt` INTEGER NOT NULL,
                    `updatedAt` INTEGER NOT NULL,
                    PRIMARY KEY(`tripPlanId`),
                    FOREIGN KEY(`tripPlanId`) REFERENCES `trip_plans`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `budget_entries` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `tripPlanId` INTEGER NOT NULL,
                    `type` TEXT NOT NULL,
                    `amount` TEXT NOT NULL,
                    `krwPerUnit` TEXT NOT NULL,
                    `rateDate` TEXT NOT NULL,
                    `convertedKrwAmount` TEXT NOT NULL,
                    `memo` TEXT NOT NULL,
                    `sortOrder` INTEGER NOT NULL,
                    `createdAt` INTEGER NOT NULL,
                    `updatedAt` INTEGER NOT NULL,
                    FOREIGN KEY(`tripPlanId`) REFERENCES `trip_budgets`(`tripPlanId`) ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent()
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_budget_entries_tripPlanId` " +
                    "ON `budget_entries` (`tripPlanId`)"
            )
            db.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS `index_budget_entries_tripPlanId_sortOrder` " +
                    "ON `budget_entries` (`tripPlanId`, `sortOrder`)"
            )
        }
    }

    val Migration7To8 = object : Migration(7, 8) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Early development builds briefly used v7 before country fields were added.
            // Check the actual table so both variants can migrate safely.
            if (!db.hasColumn("trip_plans", "countryCode")) {
                db.execSQL(
                    "ALTER TABLE `trip_plans` ADD COLUMN `countryCode` TEXT NOT NULL DEFAULT 'KR'"
                )
            }
            if (!db.hasColumn("trip_plans", "currencyCode")) {
                db.execSQL(
                    "ALTER TABLE `trip_plans` ADD COLUMN `currencyCode` TEXT NOT NULL DEFAULT 'KRW'"
                )
            }
        }
    }

    val Migration8To9 = object : Migration(8, 9) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `expenses` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `tripPlanId` INTEGER NOT NULL,
                    `itineraryItemId` INTEGER,
                    `occurredDate` TEXT NOT NULL,
                    `occurredTimeMinutes` INTEGER NOT NULL,
                    `amount` TEXT NOT NULL,
                    `currencyCode` TEXT NOT NULL,
                    `krwPerUnit` TEXT NOT NULL,
                    `rateDate` TEXT NOT NULL,
                    `convertedKrwAmount` TEXT NOT NULL,
                    `category` TEXT NOT NULL,
                    `createdAt` INTEGER NOT NULL,
                    FOREIGN KEY(`tripPlanId`) REFERENCES `trip_plans`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                    FOREIGN KEY(`itineraryItemId`) REFERENCES `itinerary_items`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL
                )
                """.trimIndent()
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_expenses_tripPlanId` ON `expenses` (`tripPlanId`)"
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_expenses_itineraryItemId` " +
                    "ON `expenses` (`itineraryItemId`)"
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_expenses_tripPlanId_occurredDate_occurredTimeMinutes` " +
                    "ON `expenses` (`tripPlanId`, `occurredDate`, `occurredTimeMinutes`)"
            )
        }
    }

    val Migration9To10 = object : Migration(9, 10) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                INSERT INTO expenses (
                    tripPlanId, itineraryItemId, occurredDate, occurredTimeMinutes,
                    amount, currencyCode, krwPerUnit, rateDate,
                    convertedKrwAmount, category, createdAt
                )
                SELECT
                    item.tripPlanId,
                    item.id,
                    item.date,
                    CASE
                        WHEN item.startTime IS NULL THEN 0
                        ELSE CAST(substr(item.startTime, 1, 2) AS INTEGER) * 60 +
                            CAST(substr(item.startTime, 4, 2) AS INTEGER)
                    END,
                    item.actualAmount,
                    item.actualCurrencyCode,
                    CASE
                        WHEN item.actualCurrencyCode = 'KRW' AND trip.currencyCode != 'KRW'
                        THEN COALESCE((
                            SELECT rate.krwPerUnit FROM exchange_rates AS rate
                            WHERE rate.currencyCode = trip.currencyCode
                            LIMIT 1
                        ), item.actualKrwPerUnit)
                        ELSE item.actualKrwPerUnit
                    END,
                    item.actualRateDate,
                    item.actualConvertedKrwAmount,
                    'Other',
                    trip.updatedAt
                FROM itinerary_items AS item
                INNER JOIN trip_plans AS trip ON trip.id = item.tripPlanId
                WHERE item.actualAmount IS NOT NULL
                    AND item.actualCurrencyCode IS NOT NULL
                    AND item.actualKrwPerUnit IS NOT NULL
                    AND item.actualRateDate IS NOT NULL
                    AND item.actualConvertedKrwAmount IS NOT NULL
                """.trimIndent()
            )
            db.execSQL(
                """
                UPDATE itinerary_items SET
                    actualAmount = NULL,
                    actualCurrencyCode = NULL,
                    actualKrwPerUnit = NULL,
                    actualRateDate = NULL,
                    actualConvertedKrwAmount = NULL
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
        Migration6To7,
        Migration7To8,
        Migration8To9,
        Migration9To10,
    )

    private fun SupportSQLiteDatabase.hasColumn(tableName: String, columnName: String): Boolean =
        query("PRAGMA table_info(`$tableName`)").use { cursor ->
            val nameIndex = cursor.getColumnIndexOrThrow("name")
            while (cursor.moveToNext()) {
                if (cursor.getString(nameIndex) == columnName) return@use true
            }
            false
        }
}
