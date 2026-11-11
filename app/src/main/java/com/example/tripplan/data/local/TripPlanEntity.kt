package com.example.tripplan.data.local

import androidx.room.Entity
import androidx.room.ColumnInfo
import androidx.room.PrimaryKey

@Entity(tableName = "trip_plans")
data class TripPlanEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    @ColumnInfo(defaultValue = "")
    val destination: String,
    val startDate: String,
    val endDate: String,
    @ColumnInfo(defaultValue = "")
    val description: String,
    @ColumnInfo(defaultValue = "0")
    val createdAt: Long,
    @ColumnInfo(defaultValue = "0")
    val updatedAt: Long,
    val completedAt: Long? = null,
    @ColumnInfo(defaultValue = "'KR'")
    val countryCode: String = "KR",
    @ColumnInfo(defaultValue = "'KRW'")
    val currencyCode: String = "KRW",
)
