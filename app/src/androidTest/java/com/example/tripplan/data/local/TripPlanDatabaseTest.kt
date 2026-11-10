package com.example.tripplan.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.example.tripplan.data.repository.DefaultTripPlanRepository
import com.example.tripplan.domain.model.TripPlanInput
import com.example.tripplan.domain.usecase.CreateTripPlanResult
import com.example.tripplan.domain.usecase.CreateTripPlanUseCase
import java.io.IOException
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TripPlanDatabaseTest {
    private lateinit var database: TripPlanDatabase
    private lateinit var context: Context

    @Before
    fun createDatabase() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(
            context,
            TripPlanDatabase::class.java,
        ).build()
    }

    @After
    @Throws(IOException::class)
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun insertAndObserveTripPlanAndItems() = runTest {
        val tripPlanId = database.tripPlanDao().insertTripPlan(sampleTripPlan())
        database.itineraryItemDao().insertItem(sampleItem(tripPlanId))

        val tripPlans = database.tripPlanDao().observeTripPlans().first()
        val items = database.itineraryItemDao().observeItems(tripPlanId).first()

        assertEquals(1, tripPlans.size)
        assertEquals("제주 여행", tripPlans.single().title)
        assertEquals(1, items.size)
        assertEquals("성산일출봉", items.single().title)
    }

    @Test
    fun deleteTripPlan_cascadesToItems() = runTest {
        val tripPlanId = database.tripPlanDao().insertTripPlan(sampleTripPlan())
        database.itineraryItemDao().insertItem(sampleItem(tripPlanId))

        database.tripPlanDao().deleteTripPlan(tripPlanId)

        assertTrue(database.itineraryItemDao().observeItems(tripPlanId).first().isEmpty())
    }

    @Test
    fun stage2Tables_storePackingAndPlaceAndPreservePlaceSnapshot() = runTest {
        val tripPlanId = database.tripPlanDao().insertTripPlan(sampleTripPlan())
        val placeId = database.placeDao().insertPlace(
            PlaceEntity(
                name = "제주국제공항",
                address = "제주시 공항로 2",
                latitude = 33.5104,
                longitude = 126.4914,
                source = "Manual",
                providerName = null,
                externalId = null,
                createdAt = 1L,
                updatedAt = 1L,
            )
        )
        database.itineraryItemDao().insertItem(
            sampleItem(tripPlanId).copy(place = "제주국제공항", placeId = placeId)
        )
        database.packingItemDao().insertItem(
            PackingItemEntity(
                tripPlanId = tripPlanId,
                name = "여권",
                category = "서류",
                quantity = 1,
                memo = "",
                type = "Shared",
                checkedAt = null,
                sortOrder = 0,
            )
        )

        assertEquals("여권", database.packingItemDao().observeItems(tripPlanId).first().single().name)
        database.placeDao().deletePlace(placeId)
        val itinerary = database.itineraryItemDao().observeItems(tripPlanId).first().single()
        assertEquals(null, itinerary.placeId)
        assertEquals("제주국제공항", itinerary.place)

        database.tripPlanDao().deleteTripPlan(tripPlanId)
        assertTrue(database.packingItemDao().observeItems(tripPlanId).first().isEmpty())
    }

    @Test
    fun createTripPlanUseCase_savesAndReadsDomainModel() = runTest {
        val clock = Clock.fixed(Instant.parse("2026-11-09T01:00:00Z"), ZoneOffset.UTC)
        val repository = DefaultTripPlanRepository(database.tripPlanDao(), clock)
        val useCase = CreateTripPlanUseCase(repository)

        val result = useCase(
            TripPlanInput(
                title = "제주 여행",
                destination = "제주",
                startDate = LocalDate.of(2026, 11, 9),
                endDate = LocalDate.of(2026, 11, 11),
                description = "친구와 여행",
            )
        )
        val id = (result as CreateTripPlanResult.Success).tripPlanId
        val saved = repository.getTripPlan(id)

        assertEquals("제주 여행", saved?.title)
        assertEquals("제주", saved?.destination)
        assertEquals(LocalDate.of(2026, 11, 9), saved?.startDate)
        assertEquals(Instant.parse("2026-11-09T01:00:00Z"), saved?.createdAt)
    }

    @Test
    fun deleteItemAndUpdateItems_removesAndReordersInTransaction() = runTest {
        val tripPlanId = database.tripPlanDao().insertTripPlan(sampleTripPlan())
        val firstId = database.itineraryItemDao().insertItem(sampleItem(tripPlanId))
        val secondId = database.itineraryItemDao().insertItem(
            sampleItem(tripPlanId).copy(title = "두 번째", sortOrder = 4)
        )

        database.itineraryItemDao().deleteItemAndUpdateItems(
            itemId = firstId,
            items = listOf(sampleItem(tripPlanId).copy(id = secondId, title = "두 번째", sortOrder = 0)),
        )

        val remaining = database.itineraryItemDao().observeItems(tripPlanId).first().single()
        assertEquals(secondId, remaining.id)
        assertEquals(0, remaining.sortOrder)
    }

    @Test
    fun updateTripAndDeleteOutOfRangeItems_runsInSingleTransaction() = runTest {
        val tripPlanId = database.tripPlanDao().insertTripPlan(sampleTripPlan())
        val keptId = database.itineraryItemDao().insertItem(sampleItem(tripPlanId))
        val deletedId = database.itineraryItemDao().insertItem(
            sampleItem(tripPlanId).copy(date = "2026-11-11", title = "삭제 일정")
        )

        database.tripPlanDao().updateTripPlanAndDeleteItineraryItems(
            tripPlan = sampleTripPlan().copy(
                id = tripPlanId,
                title = "기간 변경 여행",
                endDate = "2026-11-10",
            ),
            itemIds = listOf(deletedId),
        )

        val updatedTrip = database.tripPlanDao().getTripPlan(tripPlanId)
        val remainingItems = database.itineraryItemDao().observeItems(tripPlanId).first()
        assertEquals("기간 변경 여행", updatedTrip?.title)
        assertEquals("2026-11-10", updatedTrip?.endDate)
        assertEquals(listOf(keptId), remainingItems.map { it.id })
    }

    @Test
    fun closeAndReopenDatabase_preservesTripItemsAndOrder() = runTest {
        database.close()
        val databaseName = "restart-persistence-test"
        context.deleteDatabase(databaseName)
        database = Room.databaseBuilder(context, TripPlanDatabase::class.java, databaseName).build()
        val tripPlanId = database.tripPlanDao().insertTripPlan(sampleTripPlan())
        database.tripPlanDao().updateTripCompletion(
            tripPlanId = tripPlanId,
            completedAt = 500L,
            updatedAt = 600L,
        )
        database.itineraryItemDao().insertItem(sampleItem(tripPlanId).copy(title = "두 번째", sortOrder = 1))
        database.itineraryItemDao().insertItem(sampleItem(tripPlanId).copy(title = "첫 번째", sortOrder = 0))

        database.close()
        database = Room.databaseBuilder(context, TripPlanDatabase::class.java, databaseName).build()

        assertEquals("제주 여행", database.tripPlanDao().getTripPlan(tripPlanId)?.title)
        assertEquals(500L, database.tripPlanDao().getTripPlan(tripPlanId)?.completedAt)
        assertEquals(
            listOf("첫 번째", "두 번째"),
            database.itineraryItemDao().observeItems(tripPlanId).first().map { it.title },
        )
        database.close()
        context.deleteDatabase(databaseName)
        database = Room.inMemoryDatabaseBuilder(context, TripPlanDatabase::class.java).build()
    }

    private fun sampleTripPlan() = TripPlanEntity(
        title = "제주 여행",
        destination = "제주",
        startDate = "2026-11-09",
        endDate = "2026-11-11",
        description = "",
        createdAt = 1L,
        updatedAt = 1L,
    )

    private fun sampleItem(tripPlanId: Long) = ItineraryItemEntity(
        tripPlanId = tripPlanId,
        date = "2026-11-09",
        title = "성산일출봉",
        place = "제주",
        startTime = null,
        endTime = null,
        note = "",
        sortOrder = 0,
    )
}
