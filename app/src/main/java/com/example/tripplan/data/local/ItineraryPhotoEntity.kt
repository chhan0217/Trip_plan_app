package com.example.tripplan.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "itinerary_photos",
    foreignKeys = [
        ForeignKey(ItineraryItemEntity::class, ["id"], ["itineraryId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("itineraryId")],
)
data class ItineraryPhotoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val itineraryId: Long,
    val jpeg: ByteArray,
)
