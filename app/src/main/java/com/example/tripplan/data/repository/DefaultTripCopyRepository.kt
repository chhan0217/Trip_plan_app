package com.example.tripplan.data.repository

import androidx.room.withTransaction
import com.example.tripplan.data.local.ItineraryItemDao
import com.example.tripplan.data.local.TripPlanDao
import com.example.tripplan.data.local.TripPlanDatabase
import com.example.tripplan.data.local.TripPlanEntity
import com.example.tripplan.data.mapper.toEntity
import com.example.tripplan.domain.model.ItineraryItem
import com.example.tripplan.domain.model.TripPlanInput
import com.example.tripplan.domain.repository.TripCopyRepository
import java.time.Clock
import javax.inject.Inject

class DefaultTripCopyRepository @Inject constructor(
    private val database: TripPlanDatabase,
    private val tripDao: TripPlanDao,
    private val itineraryDao: ItineraryItemDao,
    private val clock: Clock,
) : TripCopyRepository {
    override suspend fun copyTrip(input: TripPlanInput, itineraryItems: List<ItineraryItem>): Long =
        database.withTransaction {
            val now = clock.instant().toEpochMilli()
            val tripId = tripDao.insertTripPlan(
                TripPlanEntity(
                    title = input.title.trim(), destination = input.destination.trim(),
                    startDate = input.startDate.toString(), endDate = input.endDate.toString(),
                    description = input.description.trim(), createdAt = now, updatedAt = now,
                    completedAt = null,
                    countryCode = input.countryCode,
                    currencyCode = input.currencyCode,
                )
            )
            itineraryItems.forEach { item ->
                itineraryDao.insertItem(
                    item.copy(
                        id = 0,
                        tripPlanId = tripId,
                        estimatedCost = null,
                        actualCost = null,
                    ).toEntity()
                )
            }
            tripId
        }
}
