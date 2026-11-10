package com.example.tripplan.data.repository

import com.example.tripplan.data.local.PackingItemDao
import com.example.tripplan.data.mapper.toDomain
import com.example.tripplan.data.mapper.toEntity
import com.example.tripplan.domain.model.PackingItem
import com.example.tripplan.domain.repository.PackingRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class DefaultPackingRepository @Inject constructor(
    private val dao: PackingItemDao,
) : PackingRepository {
    override fun observeItems(tripPlanId: Long): Flow<List<PackingItem>> =
        dao.observeItems(tripPlanId).map { items -> items.map { it.toDomain() } }

    override suspend fun getItem(itemId: Long): PackingItem? = dao.getItem(itemId)?.toDomain()
    override suspend fun createItem(item: PackingItem): Long = dao.insertItem(item.toEntity())
    override suspend fun updateItem(item: PackingItem) = dao.updateItem(item.toEntity())
    override suspend fun updateItems(items: List<PackingItem>) = dao.updateItems(items.map { it.toEntity() })
    override suspend fun deleteItem(itemId: Long) = dao.deleteItem(itemId)
}
