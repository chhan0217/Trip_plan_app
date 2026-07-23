package com.example.tripplan.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "trip_plans")
data class TripPlanEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val startDate: String,
    val endDate: String,
)
