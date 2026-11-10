package com.example.tripplan.domain.repository

import com.example.tripplan.domain.model.PackingItem
import kotlinx.coroutines.flow.Flow

interface PackingRepository {
    fun observeItems(tripPlanId: Long): Flow<List<PackingItem>>
    suspend fun getItem(itemId: Long): PackingItem?
    suspend fun createItem(item: PackingItem): Long
    suspend fun updateItem(item: PackingItem)
    suspend fun updateItems(items: List<PackingItem>)
    suspend fun deleteItem(itemId: Long)
}
