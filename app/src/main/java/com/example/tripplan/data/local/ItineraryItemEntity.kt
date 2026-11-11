package com.example.tripplan.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "itinerary_items",
    foreignKeys = [
        ForeignKey(
            entity = TripPlanEntity::class,
            parentColumns = ["id"],
            childColumns = ["tripPlanId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = PlaceEntity::class,
            parentColumns = ["id"],
            childColumns = ["placeId"],
            onDelete = ForeignKey.SET_NULL,
        )
    ],
    indices = [
        Index(value = ["tripPlanId"]),
        Index(value = ["tripPlanId", "date", "sortOrder"]),
        Index(value = ["placeId"]),
    ],
)
data class ItineraryItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val tripPlanId: Long,
    val date: String,
    val title: String,
    val place: String,
    val startTime: String?,
    val endTime: String?,
    val note: String,
    val sortOrder: Int,
    val placeId: Long? = null,
    val estimatedAmount: String? = null,
    val estimatedCurrencyCode: String? = null,
    val estimatedKrwPerUnit: String? = null,
    val estimatedRateDate: String? = null,
    val estimatedConvertedKrwAmount: String? = null,
    val actualAmount: String? = null,
    val actualCurrencyCode: String? = null,
    val actualKrwPerUnit: String? = null,
    val actualRateDate: String? = null,
    val actualConvertedKrwAmount: String? = null,
)
