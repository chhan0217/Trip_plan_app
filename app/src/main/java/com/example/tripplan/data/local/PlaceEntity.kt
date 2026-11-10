package com.example.tripplan.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "places",
    indices = [
        Index(value = ["name"]),
        Index(value = ["providerName", "externalId"]),
    ],
)
data class PlaceEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val address: String,
    val latitude: Double?,
    val longitude: Double?,
    val source: String,
    val providerName: String?,
    val externalId: String?,
    val createdAt: Long,
    val updatedAt: Long,
)
