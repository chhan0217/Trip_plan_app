package com.example.tripplan.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "lodgings",
    foreignKeys = [ForeignKey(TripPlanEntity::class, ["id"], ["tripPlanId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("tripPlanId")],
)
data class LodgingEntity(
    @PrimaryKey(autoGenerate = true) val id: Long,
    val tripPlanId: Long,
    val name: String,
    val checkIn: String,
    val checkOut: String,
    val address: String,
    val checkInTime: String?,
    val note: String,
    val checkOutTime: String?,
    val latitude: Double?,
    val longitude: Double?,
    val googlePlaceId: String?,
)
