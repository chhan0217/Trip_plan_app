package com.example.tripplan.data.repository

import android.graphics.Bitmap
import android.net.Uri
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.example.tripplan.data.local.ItineraryItemEntity
import com.example.tripplan.data.local.TripPlanDatabase
import com.example.tripplan.data.local.TripPlanEntity
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ItineraryPhotoRepositoryTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val clock = Clock.fixed(Instant.parse("2026-11-25T10:00:00Z"), ZoneOffset.UTC)
    private lateinit var database: TripPlanDatabase
    private lateinit var repository: DefaultItineraryPhotoRepository

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(context, TripPlanDatabase::class.java).build()
        repository = DefaultItineraryPhotoRepository(context, database, clock)
    }

    @After
    fun close() = database.close()

    @Test
    fun storesIndependentCopyAndDeletesItWithSchedule() = runBlocking {
        val trip = createTrip()
        val item = createItem(trip)
        val target = repository.createCaptureTarget()
        try {
            val bitmap = Bitmap.createBitmap(32, 32, Bitmap.Config.ARGB_8888)
            try {
                checkNotNull(context.contentResolver.openOutputStream(Uri.parse(target))).use {
                    assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it))
                }
            } finally {
                bitmap.recycle()
            }
            assertTrue(repository.add(trip, item, target))
        } finally {
            repository.discardCaptureTarget(target)
        }

        val saved = repository.observe(trip).first().single()
        assertEquals(item, saved.itineraryId)
        assertNotNull(repository.getJpeg(trip, saved.id))
        database.itineraryItemDao().deleteItem(item)
        assertTrue(repository.observe(trip).first().isEmpty())
    }

    @Test
    fun deniesWritesBeforeTravelAndAfterCompletion() = runBlocking {
        val future = createTrip(start = "2026-11-26")
        assertFalse(repository.add(future, createItem(future), "unused"))
        val completed = createTrip(completedAt = 1)
        assertFalse(repository.add(completed, createItem(completed), "unused"))
    }

    private suspend fun createTrip(start: String = "2026-11-25", completedAt: Long? = null): Long =
        database.tripPlanDao().insertTripPlan(
            TripPlanEntity(
                title = "제주 여행",
                destination = "",
                startDate = start,
                endDate = "2026-11-27",
                description = "",
                createdAt = 0,
                updatedAt = 0,
                completedAt = completedAt,
            )
        )

    private suspend fun createItem(tripId: Long): Long = database.itineraryItemDao().insertItem(
        ItineraryItemEntity(
            tripPlanId = tripId,
            date = "2026-11-26",
            title = "동문시장",
            place = "",
            startTime = null,
            endTime = null,
            note = "",
            sortOrder = 0,
        )
    )
}
