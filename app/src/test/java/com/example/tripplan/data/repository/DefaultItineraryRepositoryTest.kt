package com.example.tripplan.data.repository

import com.example.tripplan.data.local.ItineraryItemDao
import com.example.tripplan.data.local.ItineraryItemEntity
import com.example.tripplan.domain.model.ItineraryItem
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class DefaultItineraryRepositoryTest {
    @Test
    fun createAndObserveItem_mapsBetweenDomainAndEntity() = runTest {
        val dao = FakeItineraryItemDao()
        val repository = DefaultItineraryRepository(dao)
        val item = sampleItem()

        val id = repository.createItem(item)
        val observed = repository.observeItems(item.tripPlanId).first().single()

        assertEquals(17L, id)
        assertEquals(item.copy(id = 17L), observed)
        assertEquals("09:30", dao.insertedItem?.startTime)
        assertEquals(2, dao.insertedItem?.sortOrder)
    }

    private fun sampleItem() = ItineraryItem(
        id = 0L,
        tripPlanId = 4L,
        date = LocalDate.of(2026, 11, 10),
        title = "성산일출봉",
        place = "제주",
        startTime = LocalTime.of(9, 30),
        endTime = LocalTime.of(11, 0),
        note = "입장권 확인",
        order = 2,
    )

    private class FakeItineraryItemDao : ItineraryItemDao {
        private val items = MutableStateFlow<List<ItineraryItemEntity>>(emptyList())
        var insertedItem: ItineraryItemEntity? = null

        override fun observeItems(tripPlanId: Long): Flow<List<ItineraryItemEntity>> = items

        override fun observeItemsByDate(
            tripPlanId: Long,
            date: String,
        ): Flow<List<ItineraryItemEntity>> = items

        override suspend fun getItem(itemId: Long): ItineraryItemEntity? {
            return items.value.firstOrNull { it.id == itemId }
        }

        override suspend fun insertItem(item: ItineraryItemEntity): Long {
            val saved = item.copy(id = 17L)
            insertedItem = item
            items.value = listOf(saved)
            return saved.id
        }

        override suspend fun updateItem(item: ItineraryItemEntity) {
            items.value = items.value.map { current ->
                if (current.id == item.id) item else current
            }
        }

        override suspend fun updateItems(values: List<ItineraryItemEntity>) {
            val updates = values.associateBy { it.id }
            items.value = items.value.map { current -> updates[current.id] ?: current }
        }

        override suspend fun deleteItem(itemId: Long) {
            items.value = items.value.filterNot { it.id == itemId }
        }
    }
}
