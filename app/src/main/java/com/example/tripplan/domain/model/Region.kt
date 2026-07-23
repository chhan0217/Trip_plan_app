package com.example.tripplan.domain.model

data class Region(
    val id: String,
    val placeName: String,
    val provinceName: String,
    val cityName: String,
    val districtName: String,
    val latitude: Double,
    val longitude: Double,
)
