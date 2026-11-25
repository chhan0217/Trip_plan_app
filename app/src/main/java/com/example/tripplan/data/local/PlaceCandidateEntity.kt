package com.example.tripplan.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "place_candidates",
    foreignKeys = [
        ForeignKey(TripPlanEntity::class, ["id"], ["tripPlanId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(PlaceEntity::class, ["id"], ["placeId"], onDelete = ForeignKey.SET_NULL),
        ForeignKey(ItineraryItemEntity::class, ["id"], ["itineraryId"], onDelete = ForeignKey.SET_NULL),
    ],
    indices = [Index("tripPlanId"), Index("placeId"), Index("itineraryId")],
)
data class PlaceCandidateEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tripPlanId: Long,
    val name: String,
    val address: String,
    val note: String,
    val placeId: Long?,
    val itineraryId: Long? = null,
)
