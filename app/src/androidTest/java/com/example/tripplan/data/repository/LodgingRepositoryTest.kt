package com.example.tripplan.data.repository

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.example.tripplan.data.local.TripPlanDatabase
import com.example.tripplan.data.local.TripPlanEntity
import com.example.tripplan.domain.model.Lodging
import com.example.tripplan.domain.repository.LodgingSaveResult
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class LodgingRepositoryTest {
    private lateinit var database: TripPlanDatabase
    private lateinit var repository: DefaultLodgingRepository
    private val start = LocalDate.of(2026, 11, 26)

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            TripPlanDatabase::class.java,
        ).build()
        repository = DefaultLodgingRepository(
            database,
            Clock.fixed(Instant.parse("2026-11-25T00:00:00Z"), ZoneOffset.UTC),
        )
    }

    @After
    fun close() = database.close()

    @Test
    fun sameDayTransfer_isAllowedWhileOverlappingNightsAreRejected() = runBlocking {
        val tripId = createTrip()
        val first = Lodging(
            name = "마이애미 호텔",
            checkIn = start,
            checkOut = start.plusDays(2),
            address = "방콕",
            checkInTime = LocalTime.of(15, 30),
            note = "예약 확인",
            checkOutTime = LocalTime.of(11, 0),
            latitude = 13.7,
            longitude = 100.5,
            googlePlaceId = "test-place-id",
        )
        val second = Lodging(name = "다른 호텔", checkIn = first.checkOut, checkOut = start.plusDays(4))

        assertEquals(LodgingSaveResult.Saved, repository.save(tripId, second))
        assertEquals(LodgingSaveResult.Saved, repository.save(tripId, first))
        assertEquals(LodgingSaveResult.Overlap, repository.save(tripId, second.copy(checkIn = start.plusDays(1))))
        assertEquals(LodgingSaveResult.InvalidDates, repository.save(tripId, first.copy(checkIn = start.minusDays(1))))

        val saved = repository.observe(tripId).first()
        assertEquals(listOf(first.name, second.name), saved.map { it.name })
        assertEquals(listOf(2L, 2L), saved.map { it.nights })
        assertEquals(first, saved.first().copy(id = 0))
        val edited = saved.first().copy(note = "수정된 메모")
        assertEquals(LodgingSaveResult.Saved, repository.save(tripId, edited))
        assertEquals(edited, repository.observe(tripId).first().first())
        assertTrue(repository.remove(tripId, edited.id))
        assertEquals(second.name, repository.observe(tripId).first().single().name)
    }

    private suspend fun createTrip() = database.tripPlanDao().insertTripPlan(
        TripPlanEntity(
            title = "태국 여행",
            destination = "방콕",
            startDate = start.toString(),
            endDate = start.plusDays(4).toString(),
            description = "",
            createdAt = 0,
            updatedAt = 0,
        )
    )
}
