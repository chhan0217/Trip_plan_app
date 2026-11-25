package com.example.tripplan.data.repository

import androidx.room.withTransaction
import com.example.tripplan.data.local.PlaceCandidateEntity
import com.example.tripplan.data.local.TripPlanDatabase
import com.example.tripplan.data.local.TripPlanEntity
import com.example.tripplan.data.mapper.toDomain
import com.example.tripplan.data.mapper.toEntity
import com.example.tripplan.domain.model.ItineraryItem
import com.example.tripplan.domain.model.Place
import com.example.tripplan.domain.model.PlaceSource
import com.example.tripplan.domain.repository.PlaceCandidateRepository
import com.example.tripplan.domain.rule.ItineraryTimeOrderNormalizer
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class DefaultPlaceCandidateRepository @Inject constructor(
    private val database: TripPlanDatabase,
    private val clock: Clock,
) : PlaceCandidateRepository {
    private val candidates = database.placeCandidateDao()
    private val trips = database.tripPlanDao()
    private val itineraries = database.itineraryItemDao()

    override fun observe(tripId: Long) =
        candidates.observe(tripId).map { rows -> rows.map { it.toDomain() } }

    override suspend fun add(tripId: Long, name: String, address: String, note: String, place: Place?) =
        database.withTransaction {
            if (trips.getTripPlan(tripId)?.isEditable() != true) return@withTransaction false
            val placeDao = database.placeDao()
            val placeId = place?.let {
                if (it.id == 0L) placeDao.insertPlace(it.toEntity())
                else placeDao.getPlace(it.id)?.id ?: return@withTransaction false
            }
            candidates.insert(PlaceCandidateEntity(tripPlanId = tripId, name = name, address = address, note = note, placeId = placeId))
            true
        }

    override suspend fun schedule(tripId: Long, candidateId: Long, date: LocalDate) =
        database.withTransaction {
            val trip = trips.getTripPlan(tripId) ?: return@withTransaction false
            if (!trip.isEditable() || date !in LocalDate.parse(trip.startDate)..LocalDate.parse(trip.endDate)) {
                return@withTransaction false
            }
            val candidate = candidates.get(tripId, candidateId) ?: return@withTransaction false
            if (candidate.itineraryId != null) return@withTransaction false
            val items = itineraries.observeItemsByDate(tripId, date.toString()).first().map { it.toDomain() }
            val placeId = candidate.placeId ?: candidate.address.takeIf(String::isNotBlank)?.let { address ->
                val now = clock.instant()
                database.placeDao().insertPlace(
                    Place(0, candidate.name, address, null, null, PlaceSource.Manual, null, null, now, now)
                        .toEntity()
                )
            }
            val item = ItineraryItem(
                id = 0,
                tripPlanId = tripId,
                date = date,
                title = candidate.name,
                place = candidate.name,
                note = candidate.note,
                order = items.maxOfOrNull { it.order }?.plus(1) ?: 0,
                placeId = placeId,
            )
            val id = itineraries.insertItem(item.toEntity())
            val normalized = ItineraryTimeOrderNormalizer.normalize(items + item.copy(id = id))
            itineraries.updateItems(normalized.map { it.toEntity() })
            candidates.link(candidateId, id)
            true
        }

    override suspend fun remove(tripId: Long, candidateId: Long) = database.withTransaction {
        trips.getTripPlan(tripId)?.isEditable() == true && candidates.delete(tripId, candidateId) == 1
    }

    private fun TripPlanEntity.isEditable() =
        isDraft || (completedAt == null && LocalDate.parse(endDate) >= LocalDate.now(clock))
}
