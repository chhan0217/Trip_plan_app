package com.example.tripplan.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "regions")
data class RegionEntity(
    @PrimaryKey
    val id: String,
    val placeName: String,
    val provinceName: String,
    val provinceCode: String,
    val cityName: String,
    val cityCode: String,
    val districtName: String,
    val districtCode: String,
    val latitude: Double,
    val longitude: Double,
)
