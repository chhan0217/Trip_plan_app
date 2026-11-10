package com.example.tripplan.domain.usecase

import com.example.tripplan.domain.model.ItineraryItem
import com.example.tripplan.domain.model.PackingItem
import com.example.tripplan.domain.model.PackingItemInput
import com.example.tripplan.domain.model.PackingItemType
import com.example.tripplan.domain.model.Place
import com.example.tripplan.domain.model.PlaceInput
import com.example.tripplan.domain.model.PlaceSource
import com.example.tripplan.domain.model.TripPlan
import com.example.tripplan.domain.model.TripPlanInput
import com.example.tripplan.domain.repository.PackingRepository
import com.example.tripplan.domain.repository.PlaceRepository
import com.example.tripplan.domain.repository.TripCopyRepository
import com.example.tripplan.testutil.FakeItineraryRepository
import com.example.tripplan.testutil.FakeTripPlanRepository
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class Stage2FeatureUseCasesTest {
    @Test fun packing_createToggleAndTypeLock() = runTest {
        val repo = FakePackingRepository()
        val created = CreatePackingItemUseCase(repo)(1, PackingItemInput("여권", type=PackingItemType.Shared))
        val id = (created as PackingMutationResult.Success).itemId
        TogglePackingItemUseCase(repo, clock)(id)
        assertTrue(repo.getItem(id)?.checkedAt != null)
        val changed = UpdatePackingItemUseCase(repo)(id, PackingItemInput("여권", type=PackingItemType.Personal))
        assertEquals(PackingMutationResult.TypeCannotChange, changed)
    }

    @Test fun copy_shorterTrip_mapsRetainedDaysAndCreatesIncompleteTrip() = runTest {
        val trips = FakeTripPlanRepository().apply { setTrips(listOf(sourceTrip)) }
        val itineraries = FakeItineraryRepository().apply { setItems(listOf(item(1,0), item(2,1), item(3,2))) }
        val copyRepo = FakeCopyRepository()
        val result = CopyTripPlanUseCase(trips, itineraries, copyRepo)(
            1, TripPlanInput("복사", "제주", LocalDate.of(2027,1,1), LocalDate.of(2027,1,2)), true, setOf(1,3)
        )
        assertTrue(result is CopyTripResult.Success)
        assertEquals(listOf(LocalDate.of(2027,1,1), LocalDate.of(2027,1,2)), copyRepo.items.map { it.date })
    }

    @Test fun place_linkCopiesNameAndDeleteKeepsItinerarySnapshot() = runTest {
        val places = FakePlaceRepository()
        val itineraries = FakeItineraryRepository().apply { setItems(listOf(item(1,0))) }
        val id = (CreatePlaceUseCase(places, clock)(PlaceInput("성산일출봉", "제주")) as PlaceMutationResult.Success).id
        LinkItineraryPlaceUseCase(places, itineraries)(1, id)
        assertEquals("성산일출봉", itineraries.getItem(1)?.place)
        DeletePlaceUseCase(places)(id)
        assertEquals("성산일출봉", itineraries.getItem(1)?.place)
    }

    private fun item(id:Long, plus:Long)=ItineraryItem(id,1,sourceTrip.startDate.plusDays(plus),"일정$id","기존",order=plus.toInt())
    private val sourceTrip=TripPlan(1,"원본","제주",LocalDate.of(2026,11,9),LocalDate.of(2026,11,11),"",Instant.EPOCH,Instant.EPOCH)
    private val clock=Clock.fixed(Instant.parse("2026-11-09T00:00:00Z"),ZoneOffset.UTC)
}

private class FakePackingRepository: PackingRepository {
    private val flow=MutableStateFlow<List<PackingItem>>(emptyList()); private var next=1L
    override fun observeItems(tripPlanId:Long):Flow<List<PackingItem>> = flow
    override suspend fun getItem(itemId:Long)=flow.value.find{it.id==itemId}
    override suspend fun createItem(item:PackingItem):Long { val id=next++; flow.value+=item.copy(id=id); return id }
    override suspend fun updateItem(item:PackingItem){flow.value=flow.value.map{if(it.id==item.id)item else it}}
    override suspend fun updateItems(items:List<PackingItem>){items.forEach{updateItem(it)}}
    override suspend fun deleteItem(itemId:Long){flow.value=flow.value.filterNot{it.id==itemId}}
}
private class FakeCopyRepository: TripCopyRepository { lateinit var input:TripPlanInput; var items=listOf<ItineraryItem>()
    override suspend fun copyTrip(input:TripPlanInput,itineraryItems:List<ItineraryItem>):Long{this.input=input;items=itineraryItems;return 10} }
private class FakePlaceRepository: PlaceRepository {
    private val flow=MutableStateFlow<List<Place>>(emptyList()); private var next=1L
    override fun observePlaces():Flow<List<Place>> = flow
    override suspend fun getPlace(placeId:Long)=flow.value.find{it.id==placeId}
    override suspend fun createPlace(place:Place):Long{val id=next++;flow.value+=place.copy(id=id);return id}
    override suspend fun updatePlace(place:Place){flow.value=flow.value.map{if(it.id==place.id)place else it}}
    override suspend fun deletePlace(placeId:Long){flow.value=flow.value.filterNot{it.id==placeId}}
}
