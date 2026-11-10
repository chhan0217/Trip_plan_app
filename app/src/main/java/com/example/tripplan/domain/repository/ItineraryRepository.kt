package com.example.tripplan.domain.repository

import com.example.tripplan.domain.model.ItineraryItem
import kotlinx.coroutines.flow.Flow

interface ItineraryRepository {
    fun observeItems(tripPlanId: Long): Flow<List<ItineraryItem>>
    suspend fun getItem(itemId: Long): ItineraryItem?
    suspend fun createItem(item: ItineraryItem): Long
    suspend fun updateItem(item: ItineraryItem)
    suspend fun updateItems(items: List<ItineraryItem>)
    suspend fun deleteItem(itemId: Long)
    suspend fun deleteItemAndUpdateItems(itemId: Long, items: List<ItineraryItem>)
}
