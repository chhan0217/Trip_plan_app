package com.example.tripplan.data.file

import android.content.Context
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.example.tripplan.data.local.ItineraryItemEntity
import com.example.tripplan.data.local.TripPlanDatabase
import com.example.tripplan.data.local.TripPlanEntity
import com.example.tripplan.data.local.PackingItemEntity
import com.example.tripplan.data.local.PlaceEntity
import com.example.tripplan.domain.model.TripPlanFileFailure
import com.example.tripplan.domain.model.TripPlanFileResult
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TripPlanFileRepositoryTest {
    private lateinit var context: Context
    private lateinit var database: TripPlanDatabase
    private lateinit var repository: DefaultTripPlanFileRepository

    @Before
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, TripPlanDatabase::class.java).build()
        repository = DefaultTripPlanFileRepository(
            database = database,
            tripPlanDao = database.tripPlanDao(),
            itineraryItemDao = database.itineraryItemDao(),
            packingItemDao = database.packingItemDao(),
            placeDao = database.placeDao(),
            clock = Clock.fixed(Instant.parse("2026-11-09T01:00:00Z"), ZoneOffset.UTC),
            context = context,
        )
    }

    @After
    fun close() = database.close()

    @Test
    fun export_containsVersionAndPlanWithoutLocalIds() = runTest {
        val tripId = insertSampleTrip()

        val result = repository.exportTrip(tripId) as TripPlanFileResult.Success

        assertTrue(result.value.fileName.endsWith(".tripplan.json"))
        assertTrue(result.value.content.contains("\"formatVersion\": 2"))
        assertTrue(result.value.content.contains("\"title\": \"제주 여행\""))
        assertFalse(result.value.content.contains("\"tripPlanId\""))
        assertFalse(result.value.content.contains("\"id\""))
    }

    @Test
    fun version2_roundTripIncludesSharedPackingAndPlaceButExcludesPersonalState() = runTest {
        val tripId = insertSampleTrip()
        val placeId = database.placeDao().insertPlace(
            PlaceEntity(name="성산일출봉",address="제주",latitude=33.4,longitude=126.9,source="Manual",
                providerName=null,externalId=null,createdAt=1,updatedAt=1)
        )
        database.itineraryItemDao().insertItem(
            ItineraryItemEntity(tripPlanId=tripId,date="2026-11-09",title="관광",place="성산일출봉",
                startTime=null,endTime=null,note="",sortOrder=1,placeId=placeId)
        )
        database.packingItemDao().insertItem(PackingItemEntity(tripPlanId=tripId,name="여권",category="문서",
            quantity=1,memo="",type="Shared",checkedAt=123,sortOrder=0))
        database.packingItemDao().insertItem(PackingItemEntity(tripPlanId=tripId,name="개인 약",category="",
            quantity=1,memo="",type="Personal",checkedAt=null,sortOrder=0))

        val exported = repository.exportTrip(tripId) as TripPlanFileResult.Success
        assertTrue(exported.value.content.contains("\"formatVersion\": 2"))
        assertTrue(exported.value.content.contains("여권"))
        assertTrue(exported.value.content.contains("성산일출봉"))
        assertFalse(exported.value.content.contains("개인 약"))
        assertFalse(exported.value.content.contains("checkedAt"))

        val imported = repository.importTrip(exported.value.content) as TripPlanFileResult.Success
        val packing = database.packingItemDao().observeItems(imported.value).first()
        assertEquals(listOf("여권"), packing.map { it.name })
        assertEquals(null, packing.single().checkedAt)
        val importedItems = database.itineraryItemDao().observeItems(imported.value).first()
        assertTrue(importedItems.any { it.place == "성산일출봉" && it.placeId != null })
    }

    @Test
    fun preview_validFile_returnsSummary() {
        val result = repository.previewImport(validFile) as TripPlanFileResult.Success

        assertEquals("제주 여행", result.value.title)
        assertEquals(3, result.value.dayCount)
        assertEquals(1, result.value.itineraryItemCount)
    }

    @Test
    fun preview_unsupportedVersion_returnsExplicitFailure() {
        val result = repository.previewImport(validFile.replace("\"formatVersion\": 1", "\"formatVersion\": 9"))

        assertEquals(
            TripPlanFileResult.Failure(TripPlanFileFailure.UnsupportedVersion),
            result,
        )
    }

    @Test
    fun import_validFile_createsNewTripAndItems() = runTest {
        val result = repository.importTrip(validFile) as TripPlanFileResult.Success

        val trip = database.tripPlanDao().getTripPlan(result.value)
        val items = database.itineraryItemDao().observeItems(result.value).first()
        assertEquals("제주 여행", trip?.title)
        assertEquals(1, items.size)
        assertEquals("성산일출봉", items.single().title)
    }

    @Test
    fun import_invalidFile_doesNotChangeDatabase() = runTest {
        val result = repository.importTrip(validFile.replace("\"title\": \"제주 여행\"", "\"title\": \"\""))

        assertEquals(TripPlanFileResult.Failure(TripPlanFileFailure.InvalidData), result)
        assertTrue(database.tripPlanDao().observeTripPlans().first().isEmpty())
    }

    @Test
    fun import_sameFileTwice_createsTwoIndependentTrips() = runTest {
        val first = repository.importTrip(validFile) as TripPlanFileResult.Success
        val second = repository.importTrip(validFile) as TripPlanFileResult.Success

        assertTrue(first.value != second.value)
        assertEquals(2, database.tripPlanDao().observeTripPlans().first().size)
        assertEquals(1, database.itineraryItemDao().observeItems(first.value).first().size)
        assertEquals(1, database.itineraryItemDao().observeItems(second.value).first().size)
    }

    @Test
    fun createShareDocument_returnsReadableContentUri() = runTest {
        val shared = repository.createShareDocument("sample.tripplan.json", validFile)
            as TripPlanFileResult.Success

        assertTrue(shared.value.startsWith("content://${context.packageName}.fileprovider/"))
        assertEquals(
            TripPlanFileResult.Success(validFile),
            repository.readDocument(shared.value),
        )
    }

    @Test
    fun exportShareReadAndImport_roundTripPreservesPlanStructure() = runTest {
        val sourceId = insertSampleTrip()
        val exported = (repository.exportTrip(sourceId) as TripPlanFileResult.Success).value
        val sharedUri = (
            repository.createShareDocument(exported.fileName, exported.content)
                as TripPlanFileResult.Success
            ).value
        val sharedContent = (repository.readDocument(sharedUri) as TripPlanFileResult.Success).value

        val importedId = (repository.importTrip(sharedContent) as TripPlanFileResult.Success).value
        val sourceTrip = database.tripPlanDao().getTripPlan(sourceId)
        val importedTrip = database.tripPlanDao().getTripPlan(importedId)
        val sourceItems = database.itineraryItemDao().observeItems(sourceId).first()
        val importedItems = database.itineraryItemDao().observeItems(importedId).first()

        assertTrue(sourceId != importedId)
        assertEquals(sourceTrip?.title, importedTrip?.title)
        assertEquals(sourceTrip?.destination, importedTrip?.destination)
        assertEquals(sourceTrip?.startDate, importedTrip?.startDate)
        assertEquals(sourceTrip?.endDate, importedTrip?.endDate)
        assertEquals(sourceItems.map { it.copy(id = 0, tripPlanId = 0) }, importedItems.map {
            it.copy(id = 0, tripPlanId = 0)
        })
    }

    @Test
    fun import_failureDuringItemInsert_rollsBackTripAndEveryItem() = runTest {
        database.openHelper.writableDatabase.execSQL(
            """
            CREATE TRIGGER fail_selected_item
            BEFORE INSERT ON itinerary_items
            WHEN NEW.title = '강제 실패'
            BEGIN
                SELECT RAISE(ABORT, 'forced test failure');
            END
            """.trimIndent()
        )
        val content = fileWithItems(
            listOf(
                itemJson(title = "정상 일정", order = 0),
                itemJson(title = "강제 실패", order = 1),
            )
        )

        val result = repository.importTrip(content)

        assertEquals(TripPlanFileResult.Failure(TripPlanFileFailure.InvalidData), result)
        assertTrue(database.tripPlanDao().observeTripPlans().first().isEmpty())
        database.openHelper.writableDatabase.query("SELECT COUNT(*) FROM itinerary_items").use {
            it.moveToFirst()
            assertEquals(0, it.getInt(0))
        }
    }

    @Test
    fun import_largePlan_preservesEveryItemAndOrder() = runTest {
        val content = fileWithItems(
            (0 until 500).map { order -> itemJson(title = "일정 $order", order = order) }
        )

        val result = repository.importTrip(content) as TripPlanFileResult.Success
        val items = database.itineraryItemDao().observeItems(result.value).first()

        assertEquals(500, items.size)
        assertEquals((0 until 500).toList(), items.map { it.sortOrder })
        assertEquals("일정 499", items.last().title)
    }

    @Test
    fun preview_leapDayBoundary_acceptsDatesAndCountsDays() {
        val content = fileWithItems(
            items = listOf(itemJson(title = "윤일 일정", date = "2028-02-29", order = 0)),
            startDate = "2028-02-28",
            endDate = "2028-03-01",
        )

        val result = repository.previewImport(content) as TripPlanFileResult.Success

        assertEquals(3, result.value.dayCount)
        assertEquals(LocalDate.parse("2028-02-28"), result.value.startDate)
        assertEquals(LocalDate.parse("2028-03-01"), result.value.endDate)
    }

    @Test
    fun preview_itemOutsideTripBoundary_rejectsFile() {
        val content = fileWithItems(
            items = listOf(itemJson(title = "범위 밖", date = "2028-03-02", order = 0)),
            startDate = "2028-02-28",
            endDate = "2028-03-01",
        )

        assertEquals(
            TripPlanFileResult.Failure(TripPlanFileFailure.InvalidData),
            repository.previewImport(content),
        )
    }

    private suspend fun insertSampleTrip(): Long {
        val id = database.tripPlanDao().insertTripPlan(
            TripPlanEntity(
                title = "제주 여행",
                destination = "제주",
                startDate = "2026-11-09",
                endDate = "2026-11-11",
                description = "친구와 여행",
                createdAt = 1L,
                updatedAt = 1L,
            )
        )
        database.itineraryItemDao().insertItem(
            ItineraryItemEntity(
                tripPlanId = id,
                date = "2026-11-09",
                title = "성산일출봉",
                place = "제주",
                startTime = "09:00",
                endTime = "11:00",
                note = "입장권",
                sortOrder = 0,
            )
        )
        return id
    }

    private val validFile = """
        {
          "formatVersion": 1,
          "trip": {
            "title": "제주 여행",
            "destination": "제주",
            "startDate": "2026-11-09",
            "endDate": "2026-11-11",
            "description": "친구와 여행"
          },
          "itineraryItems": [
            {
              "date": "2026-11-09",
              "title": "성산일출봉",
              "place": "제주",
              "startTime": "09:00",
              "endTime": "11:00",
              "note": "입장권",
              "order": 0
            }
          ]
        }
    """.trimIndent()

    private fun fileWithItems(
        items: List<String>,
        startDate: String = "2026-11-09",
        endDate: String = "2026-11-11",
    ) = """
        {
          "formatVersion": 1,
          "trip": {
            "title": "안정성 테스트 여행",
            "destination": "제주",
            "startDate": "$startDate",
            "endDate": "$endDate",
            "description": ""
          },
          "itineraryItems": [${items.joinToString(",")}]
        }
    """.trimIndent()

    private fun itemJson(
        title: String,
        order: Int,
        date: String = "2026-11-09",
    ) = """
        {
          "date": "$date",
          "title": "$title",
          "place": "장소",
          "startTime": "09:00",
          "endTime": "10:00",
          "note": "",
          "order": $order
        }
    """.trimIndent()
}
