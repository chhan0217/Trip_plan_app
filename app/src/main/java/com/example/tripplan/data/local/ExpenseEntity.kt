package com.example.tripplan.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "expenses",
    foreignKeys = [
        ForeignKey(
            entity = TripPlanEntity::class,
            parentColumns = ["id"],
            childColumns = ["tripPlanId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = ItineraryItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["itineraryItemId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [
        Index(value = ["tripPlanId"]),
        Index(value = ["itineraryItemId"]),
        Index(value = ["tripPlanId", "occurredDate", "occurredTimeMinutes"]),
    ],
)
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val tripPlanId: Long,
    val itineraryItemId: Long?,
    val occurredDate: String,
    val occurredTimeMinutes: Int,
    val amount: String,
    val currencyCode: String,
    val krwPerUnit: String,
    val rateDate: String,
    val convertedKrwAmount: String,
    val category: String,
    val createdAt: Long,
)
