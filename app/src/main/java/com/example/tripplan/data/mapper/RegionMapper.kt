package com.example.tripplan.data.mapper

import com.example.tripplan.data.local.RegionEntity
import com.example.tripplan.domain.model.Region

fun RegionEntity.toDomain(): Region = Region(
    id = id,
    placeName = placeName,
    provinceName = provinceName,
    cityName = cityName,
    districtName = districtName,
    latitude = latitude,
    longitude = longitude,
)
