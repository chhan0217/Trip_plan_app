package com.example.tripplan.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "packing_items",
    foreignKeys = [
        ForeignKey(
            entity = TripPlanEntity::class,
            parentColumns = ["id"],
            childColumns = ["tripPlanId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [
        Index(value = ["tripPlanId"]),
        Index(value = ["tripPlanId", "type", "sortOrder"]),
    ],
)
data class PackingItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val tripPlanId: Long,
    val name: String,
    val category: String,
    val quantity: Int,
    val memo: String,
    val type: String,
    val checkedAt: Long?,
    val sortOrder: Int,
)
