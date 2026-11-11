package com.example.tripplan.data.repository

import com.example.tripplan.data.local.TripBudgetDao
import com.example.tripplan.data.local.TripBudgetEntity
import com.example.tripplan.data.mapper.toDomain
import com.example.tripplan.data.mapper.toEntity
import com.example.tripplan.domain.model.BudgetEntry
import com.example.tripplan.domain.model.TripBudget
import com.example.tripplan.domain.repository.TripBudgetRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class DefaultTripBudgetRepository @Inject constructor(
    private val dao: TripBudgetDao,
) : TripBudgetRepository {
    override fun observeBudget(tripPlanId: Long): Flow<TripBudget?> =
        dao.observeBudget(tripPlanId).map { it?.toDomain() }

    override suspend fun getBudget(tripPlanId: Long): TripBudget? =
        dao.getBudget(tripPlanId)?.toDomain()

    override suspend fun createBudget(
        tripPlanId: Long,
        currencyCode: String,
        initialEntry: BudgetEntry,
    ): Long = dao.insertBudgetWithInitial(
        budget = TripBudgetEntity(
            tripPlanId = tripPlanId,
            currencyCode = currencyCode,
            createdAt = initialEntry.createdAt.toEpochMilli(),
            updatedAt = initialEntry.updatedAt.toEpochMilli(),
        ),
        initialEntry = initialEntry.toEntity(),
    )

    override suspend fun addEntry(entry: BudgetEntry): Long =
        dao.insertAdditionalEntry(entry.toEntity())
}
