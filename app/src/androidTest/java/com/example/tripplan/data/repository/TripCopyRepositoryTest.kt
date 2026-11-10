package com.example.tripplan.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.example.tripplan.data.local.TripPlanDatabase
import com.example.tripplan.domain.model.ItineraryItem
import com.example.tripplan.domain.model.TripPlanInput
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TripCopyRepositoryTest {
    private lateinit var database: TripPlanDatabase
    private lateinit var repository: DefaultTripCopyRepository
    @Before fun setup(){val context:Context=InstrumentationRegistry.getInstrumentation().targetContext;database=Room.inMemoryDatabaseBuilder(context,TripPlanDatabase::class.java).build();repository=DefaultTripCopyRepository(database,database.tripPlanDao(),database.itineraryItemDao(),Clock.fixed(Instant.EPOCH,ZoneOffset.UTC))}
    @After fun close(){database.close()}
    @Test fun invalidPlaceReference_rollsBackWholeCopy()=runTest{
        val input=TripPlanInput("복사","제주",LocalDate.of(2027,1,1),LocalDate.of(2027,1,1))
        val item=ItineraryItem(0,0,input.startDate,"일정","없는 장소",order=0,placeId=999)
        runCatching{repository.copyTrip(input,listOf(item))}
        assertTrue(database.tripPlanDao().observeTripPlans().first().isEmpty())
    }
}
