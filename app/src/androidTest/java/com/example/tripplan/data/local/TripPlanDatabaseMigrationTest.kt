package com.example.tripplan.data.local

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.platform.app.InstrumentationRegistry
import java.io.IOException
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class TripPlanDatabaseMigrationTest {
    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        TripPlanDatabase::class.java,
    )

    @Test
    @Throws(IOException::class)
    fun migrate2To3_preservesTripsAndCreatesItineraryTable() {
        helper.createDatabase(TEST_DATABASE, 2).apply {
            execSQL(
                """
                INSERT INTO trip_plans (id, title, startDate, endDate)
                VALUES (1, '제주 여행', '2026-11-09', '2026-11-11')
                """.trimIndent()
            )
            close()
        }

        val database = helper.runMigrationsAndValidate(
            TEST_DATABASE,
            3,
            true,
            DatabaseMigrations.Migration2To3,
        )

        database.query("SELECT * FROM trip_plans WHERE id = 1").use { cursor ->
            cursor.moveToFirst()
            assertEquals("제주 여행", cursor.getString(cursor.getColumnIndexOrThrow("title")))
            assertEquals("", cursor.getString(cursor.getColumnIndexOrThrow("destination")))
            assertEquals(0L, cursor.getLong(cursor.getColumnIndexOrThrow("createdAt")))
        }

        insertItineraryItem(database)
        database.query("SELECT COUNT(*) FROM itinerary_items").use { cursor ->
            cursor.moveToFirst()
            assertEquals(1, cursor.getInt(0))
        }
        database.close()
    }

    @Test
    @Throws(IOException::class)
    fun migrate1To3_preservesTripsAndAppliesEveryMigration() {
        helper.createDatabase(TEST_DATABASE_FROM_V1, 1).apply {
            execSQL(
                """
                INSERT INTO trip_plans (id, title, startDate, endDate)
                VALUES (7, '이전 여행', '2025-12-31', '2026-01-01')
                """.trimIndent()
            )
            close()
        }

        val database = helper.runMigrationsAndValidate(
            TEST_DATABASE_FROM_V1,
            3,
            true,
            DatabaseMigrations.Migration1To2,
            DatabaseMigrations.Migration2To3,
        )

        database.query("SELECT * FROM trip_plans WHERE id = 7").use { cursor ->
            cursor.moveToFirst()
            assertEquals("이전 여행", cursor.getString(cursor.getColumnIndexOrThrow("title")))
            assertEquals("", cursor.getString(cursor.getColumnIndexOrThrow("destination")))
        }
        database.query("SELECT COUNT(*) FROM regions").use { cursor ->
            cursor.moveToFirst()
            assertEquals(0, cursor.getInt(0))
        }
        insertItineraryItem(database, tripPlanId = 7)
        database.close()
    }

    @Test
    @Throws(IOException::class)
    fun migrate3To4_preservesDataAndCreatesStage2Storage() {
        helper.createDatabase(TEST_DATABASE_FROM_V3, 3).apply {
            execSQL(
                """
                INSERT INTO trip_plans (
                    id, title, destination, startDate, endDate, description, createdAt, updatedAt
                ) VALUES (
                    9, '기존 여행', '제주', '2026-11-09', '2026-11-11', '', 1, 2
                )
                """.trimIndent()
            )
            insertItineraryItem(this, tripPlanId = 9)
            close()
        }

        val database = helper.runMigrationsAndValidate(
            TEST_DATABASE_FROM_V3,
            4,
            true,
            DatabaseMigrations.Migration3To4,
        )
        database.setForeignKeyConstraintsEnabled(true)

        database.query("SELECT completedAt FROM trip_plans WHERE id = 9").use { cursor ->
            cursor.moveToFirst()
            assertEquals(true, cursor.isNull(0))
        }
        database.query("SELECT place, placeId FROM itinerary_items WHERE tripPlanId = 9").use { cursor ->
            cursor.moveToFirst()
            assertEquals("제주", cursor.getString(0))
            assertEquals(true, cursor.isNull(1))
        }
        insertPlaceAndLinkItinerary(database, tripPlanId = 9)
        insertPackingItem(database, tripPlanId = 9)
        database.execSQL("DELETE FROM places WHERE id = 20")
        database.query("SELECT place, placeId FROM itinerary_items WHERE tripPlanId = 9").use { cursor ->
            cursor.moveToFirst()
            assertEquals("제주", cursor.getString(0))
            assertEquals(true, cursor.isNull(1))
        }
        database.execSQL("DELETE FROM trip_plans WHERE id = 9")
        database.query("SELECT COUNT(*) FROM packing_items").use { cursor ->
            cursor.moveToFirst()
            assertEquals(0, cursor.getInt(0))
        }
        database.close()
    }

    @Test
    @Throws(IOException::class)
    fun migrate1To4_preservesOriginalTripAcrossEveryMigration() {
        helper.createDatabase(TEST_DATABASE_FROM_V1_TO_V4, 1).apply {
            execSQL(
                """
                INSERT INTO trip_plans (id, title, startDate, endDate)
                VALUES (11, '최초 버전 여행', '2026-01-01', '2026-01-02')
                """.trimIndent()
            )
            close()
        }

        val database = helper.runMigrationsAndValidate(
            TEST_DATABASE_FROM_V1_TO_V4,
            4,
            true,
            DatabaseMigrations.Migration1To2,
            DatabaseMigrations.Migration2To3,
            DatabaseMigrations.Migration3To4,
        )

        database.query("SELECT title, completedAt FROM trip_plans WHERE id = 11").use { cursor ->
            cursor.moveToFirst()
            assertEquals("최초 버전 여행", cursor.getString(0))
            assertEquals(true, cursor.isNull(1))
        }
        insertPackingItem(database, tripPlanId = 11)
        database.close()
    }

    @Test
    @Throws(IOException::class)
    fun migrate4To5_preservesTripsAndCreatesExchangeRateCache() {
        helper.createDatabase(TEST_DATABASE_FROM_V4, 4).apply {
            execSQL(
                """
                INSERT INTO trip_plans (
                    id, title, destination, startDate, endDate, description,
                    createdAt, updatedAt, completedAt
                ) VALUES (
                    12, '환율 여행', '도쿄', '2026-11-20', '2026-11-23', '', 1, 2, NULL
                )
                """.trimIndent()
            )
            close()
        }

        val database = helper.runMigrationsAndValidate(
            TEST_DATABASE_FROM_V4,
            5,
            true,
            DatabaseMigrations.Migration4To5,
        )

        database.query("SELECT title FROM trip_plans WHERE id = 12").use { cursor ->
            cursor.moveToFirst()
            assertEquals("환율 여행", cursor.getString(0))
        }
        database.execSQL(
            """
            INSERT INTO exchange_rates (
                currencyUnit, currencyName, baseRate,
                telegraphicTransferBuyingRate, telegraphicTransferSellingRate,
                rateDate, fetchedAt
            ) VALUES (
                'JPY(100)', '일본 옌', '951.05', '941.53', '960.56',
                '2026-11-10', 100
            )
            """.trimIndent()
        )
        database.query("SELECT baseRate FROM exchange_rates WHERE currencyUnit = 'JPY(100)'").use {
            cursor ->
            cursor.moveToFirst()
            assertEquals("951.05", cursor.getString(0))
        }
        database.close()
    }

    @Test
    @Throws(IOException::class)
    fun migrate5To6_preservesTripsAndRecreatesProviderIndependentExchangeRateCache() {
        helper.createDatabase(TEST_DATABASE_FROM_V5, 5).apply {
            execSQL(
                """
                INSERT INTO trip_plans (
                    id, title, destination, startDate, endDate, description,
                    createdAt, updatedAt, completedAt
                ) VALUES (
                    13, '베트남 여행', '다낭', '2026-12-01', '2026-12-05', '', 1, 2, NULL
                )
                """.trimIndent()
            )
            execSQL(
                """
                INSERT INTO exchange_rates (
                    currencyUnit, currencyName, baseRate,
                    telegraphicTransferBuyingRate, telegraphicTransferSellingRate,
                    rateDate, fetchedAt
                ) VALUES (
                    'JPY(100)', '일본 옌', '951.05', '941.53', '960.56',
                    '2026-11-10', 100
                )
                """.trimIndent()
            )
            close()
        }

        val database = helper.runMigrationsAndValidate(
            TEST_DATABASE_FROM_V5,
            6,
            true,
            DatabaseMigrations.Migration5To6,
        )

        database.query("SELECT title FROM trip_plans WHERE id = 13").use { cursor ->
            cursor.moveToFirst()
            assertEquals("베트남 여행", cursor.getString(0))
        }
        database.query("SELECT COUNT(*) FROM exchange_rates").use { cursor ->
            cursor.moveToFirst()
            assertEquals(0, cursor.getInt(0))
        }
        database.execSQL(
            """
            INSERT INTO exchange_rates (
                currencyCode, currencyName, krwPerUnit, rateDate, fetchedAt
            ) VALUES (
                'VND', '베트남 동', '0.05272', '2026-11-10', 200
            )
            """.trimIndent()
        )
        database.query("SELECT krwPerUnit FROM exchange_rates WHERE currencyCode = 'VND'").use {
            cursor ->
            cursor.moveToFirst()
            assertEquals("0.05272", cursor.getString(0))
        }
        database.close()
    }

    private fun insertPlaceAndLinkItinerary(
        database: SupportSQLiteDatabase,
        tripPlanId: Long,
    ) {
        database.execSQL(
            """
            INSERT INTO places (
                id, name, address, latitude, longitude, source,
                providerName, externalId, createdAt, updatedAt
            ) VALUES (
                20, '제주국제공항', '제주시 공항로 2', 33.5104, 126.4914,
                'Manual', NULL, NULL, 1, 1
            )
            """.trimIndent()
        )
        database.execSQL(
            "UPDATE itinerary_items SET placeId = 20 WHERE tripPlanId = $tripPlanId"
        )
    }

    private fun insertPackingItem(database: SupportSQLiteDatabase, tripPlanId: Long) {
        database.execSQL(
            """
            INSERT INTO packing_items (
                tripPlanId, name, category, quantity, memo, type, checkedAt, sortOrder
            ) VALUES (
                $tripPlanId, '여권', '서류', 1, '', 'Shared', NULL, 0
            )
            """.trimIndent()
        )
    }

    private fun insertItineraryItem(database: SupportSQLiteDatabase, tripPlanId: Long = 1) {
        database.execSQL(
            """
            INSERT INTO itinerary_items (
                tripPlanId, date, title, place, startTime, endTime, note, sortOrder
            ) VALUES (
                $tripPlanId, '2026-11-09', '성산일출봉', '제주', NULL, NULL, '', 0
            )
            """.trimIndent()
        )
    }

    private companion object {
        const val TEST_DATABASE = "migration-test"
        const val TEST_DATABASE_FROM_V1 = "migration-test-from-v1"
        const val TEST_DATABASE_FROM_V3 = "migration-test-from-v3"
        const val TEST_DATABASE_FROM_V1_TO_V4 = "migration-test-from-v1-to-v4"
        const val TEST_DATABASE_FROM_V4 = "migration-test-from-v4"
        const val TEST_DATABASE_FROM_V5 = "migration-test-from-v5"
    }
}
