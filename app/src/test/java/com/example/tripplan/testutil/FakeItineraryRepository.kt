package com.example.tripplan.testutil

import com.example.tripplan.domain.model.ItineraryItem
import com.example.tripplan.domain.repository.ItineraryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeItineraryRepository : ItineraryRepository {
    private val items = MutableStateFlow<List<ItineraryItem>>(emptyList())
    var nextCreatedId: Long = 100L

    fun setItems(values: List<ItineraryItem>) {
        items.value = values
    }

    override fun observeItems(tripPlanId: Long): Flow<List<ItineraryItem>> = items

    override suspend fun getItem(itemId: Long): ItineraryItem? {
        return items.value.firstOrNull { it.id == itemId }
    }

    override suspend fun createItem(item: ItineraryItem): Long {
        val id = if (item.id == 0L) nextCreatedId else item.id
        items.value = items.value + item.copy(id = id)
        return id
    }

    override suspend fun updateItem(item: ItineraryItem) {
        items.value = items.value.map { current -> if (current.id == item.id) item else current }
    }

    override suspend fun updateItems(values: List<ItineraryItem>) {
        val updates = values.associateBy { it.id }
        items.value = items.value.map { current -> updates[current.id] ?: current }
    }

    override suspend fun deleteItem(itemId: Long) {
        items.value = items.value.filterNot { it.id == itemId }
    }

    override suspend fun deleteItemAndUpdateItems(
        itemId: Long,
        values: List<ItineraryItem>,
    ) {
        deleteItem(itemId)
        updateItems(values)
    }
}
