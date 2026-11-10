package com.example.tripplan.data.repository

import com.example.tripplan.data.local.ItineraryItemDao
import com.example.tripplan.data.mapper.toDomain
import com.example.tripplan.data.mapper.toEntity
import com.example.tripplan.domain.model.ItineraryItem
import com.example.tripplan.domain.repository.ItineraryRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class DefaultItineraryRepository @Inject constructor(
    private val itineraryItemDao: ItineraryItemDao,
) : ItineraryRepository {
    override fun observeItems(tripPlanId: Long): Flow<List<ItineraryItem>> {
        return itineraryItemDao.observeItems(tripPlanId)
            .map { items -> items.map { it.toDomain() } }
    }

    override suspend fun getItem(itemId: Long): ItineraryItem? {
        return itineraryItemDao.getItem(itemId)?.toDomain()
    }

    override suspend fun createItem(item: ItineraryItem): Long {
        return itineraryItemDao.insertItem(item.toEntity())
    }

    override suspend fun updateItem(item: ItineraryItem) {
        itineraryItemDao.updateItem(item.toEntity())
    }

    override suspend fun updateItems(items: List<ItineraryItem>) {
        itineraryItemDao.updateItems(items.map { it.toEntity() })
    }

    override suspend fun deleteItem(itemId: Long) {
        itineraryItemDao.deleteItem(itemId)
    }

    override suspend fun deleteItemAndUpdateItems(
        itemId: Long,
        items: List<ItineraryItem>,
    ) {
        itineraryItemDao.deleteItemAndUpdateItems(itemId, items.map { it.toEntity() })
    }
}
