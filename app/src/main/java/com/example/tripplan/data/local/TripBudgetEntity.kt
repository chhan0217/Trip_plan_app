package com.example.tripplan.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "trip_budgets",
    foreignKeys = [
        ForeignKey(
            entity = TripPlanEntity::class,
            parentColumns = ["id"],
            childColumns = ["tripPlanId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
)
data class TripBudgetEntity(
    @PrimaryKey
    val tripPlanId: Long,
    val currencyCode: String,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(
    tableName = "budget_entries",
    foreignKeys = [
        ForeignKey(
            entity = TripBudgetEntity::class,
            parentColumns = ["tripPlanId"],
            childColumns = ["tripPlanId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [
        Index(value = ["tripPlanId"]),
        Index(value = ["tripPlanId", "sortOrder"], unique = true),
    ],
)
data class BudgetEntryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val tripPlanId: Long,
    val type: String,
    val amount: String,
    val krwPerUnit: String,
    val rateDate: String,
    val convertedKrwAmount: String,
    val memo: String,
    val sortOrder: Int,
    val createdAt: Long,
    val updatedAt: Long,
)
