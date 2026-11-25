package com.example.tripplan.data.repository

import androidx.room.withTransaction
import com.example.tripplan.data.local.TripPlanDatabase
import com.example.tripplan.data.local.TripPlanEntity
import com.example.tripplan.data.mapper.toDomain
import com.example.tripplan.data.mapper.toEntity
import com.example.tripplan.domain.model.Lodging
import com.example.tripplan.domain.repository.LodgingRepository
import com.example.tripplan.domain.repository.LodgingSaveResult
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.map

class DefaultLodgingRepository @Inject constructor(
    private val database: TripPlanDatabase,
    private val clock: Clock,
) : LodgingRepository {
    private val lodgings = database.lodgingDao()

    override fun observe(tripId: Long) = lodgings.observe(tripId).map { rows -> rows.map { it.toDomain() } }

    override suspend fun save(tripId: Long, lodging: Lodging): LodgingSaveResult = database.withTransaction {
        val trip = database.tripPlanDao().getTripPlan(tripId)
            ?: return@withTransaction LodgingSaveResult.Unavailable
        if (!trip.isEditable()) return@withTransaction LodgingSaveResult.Unavailable
        if (lodging.checkIn < LocalDate.parse(trip.startDate) ||
            lodging.checkOut > LocalDate.parse(trip.endDate) || lodging.checkIn >= lodging.checkOut
        ) return@withTransaction LodgingSaveResult.InvalidDates

        val existing = lodgings.getAll(tripId)
        if (lodging.id != 0L && existing.none { it.id == lodging.id }) {
            return@withTransaction LodgingSaveResult.Unavailable
        }
        if (existing.any { it.id != lodging.id && lodging.overlaps(it.toDomain()) }) {
            return@withTransaction LodgingSaveResult.Overlap
        }

        if (lodging.id == 0L) lodgings.insert(lodging.toEntity(tripId))
        else lodgings.update(lodging.toEntity(tripId))
        LodgingSaveResult.Saved
    }

    override suspend fun remove(tripId: Long, id: Long) = database.withTransaction {
        database.tripPlanDao().getTripPlan(tripId)?.isEditable() == true && lodgings.delete(tripId, id) == 1
    }

    private fun TripPlanEntity.isEditable() =
        isDraft || (completedAt == null && LocalDate.parse(endDate) >= LocalDate.now(clock))
}
