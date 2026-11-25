package com.example.tripplan.data.repository

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.example.tripplan.data.local.TripPlanDatabase
import com.example.tripplan.data.local.TripPlanEntity
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PlaceCandidateRepositoryTest {
    private lateinit var database: TripPlanDatabase
    private lateinit var repository: DefaultPlaceCandidateRepository
    private val day = LocalDate.parse("2026-11-27")

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            TripPlanDatabase::class.java,
        ).build()
        repository = DefaultPlaceCandidateRepository(
            database,
            Clock.fixed(Instant.parse("2026-11-25T00:00:00Z"), ZoneOffset.UTC),
        )
    }

    @After
    fun close() = database.close()

    @Test
    fun schedule_linksOnceAndKeepsItineraryWhenCandidateIsRemoved() = runBlocking {
        val tripId = createTrip()
        val otherTripId = createTrip()
        assertTrue(repository.add(tripId, "동문시장", "제주시", "저녁", null))
        val candidate = repository.observe(tripId).first().single()
        assertNull(candidate.scheduledDate)
        assertTrue(repository.observe(otherTripId).first().isEmpty())
        assertFalse(repository.schedule(otherTripId, candidate.id, day))
        assertFalse(repository.schedule(tripId, candidate.id, day.minusDays(1)))

        assertTrue(repository.schedule(tripId, candidate.id, day))
        assertFalse(repository.schedule(tripId, candidate.id, day))
        assertEquals(day, repository.observe(tripId).first().single().scheduledDate)
        val schedule = database.itineraryItemDao().observeItems(tripId).first().single()
        assertEquals("동문시장", schedule.title)
        assertEquals("저녁", schedule.note)
        assertNull(schedule.startTime)
        assertEquals("제주시", database.placeDao().getPlace(checkNotNull(schedule.placeId))?.address)

        assertTrue(repository.remove(tripId, candidate.id))
        assertTrue(repository.observe(tripId).first().isEmpty())
        assertEquals(schedule, database.itineraryItemDao().getItem(schedule.id))
    }

    @Test
    fun deletingLinkedSchedule_makesCandidateAvailableAgain() = runBlocking {
        val tripId = createTrip()
        assertTrue(repository.add(tripId, "동문시장", "", "", null))
        val candidate = repository.observe(tripId).first().single()
        assertTrue(repository.schedule(tripId, candidate.id, day))
        val schedule = database.itineraryItemDao().observeItems(tripId).first().single()
        database.itineraryItemDao().deleteItem(schedule.id)

        assertNull(repository.observe(tripId).first().single().scheduledDate)
        assertTrue(repository.schedule(tripId, candidate.id, day))
    }

    private suspend fun createTrip(): Long = database.tripPlanDao().insertTripPlan(
        TripPlanEntity(
            title = "제주",
            destination = "",
            startDate = day.toString(),
            endDate = day.plusDays(1).toString(),
            description = "",
            createdAt = 0,
            updatedAt = 0,
        )
    )
}
