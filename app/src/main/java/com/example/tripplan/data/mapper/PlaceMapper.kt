package com.example.tripplan.data.mapper

import com.example.tripplan.data.local.PlaceEntity
import com.example.tripplan.domain.model.Place
import com.example.tripplan.domain.model.PlaceSource
import java.time.Instant

fun PlaceEntity.toDomain(): Place = Place(
    id = id,
    name = name,
    address = address,
    latitude = latitude,
    longitude = longitude,
    source = PlaceSource.valueOf(source),
    providerName = providerName,
    externalId = externalId,
    createdAt = Instant.ofEpochMilli(createdAt),
    updatedAt = Instant.ofEpochMilli(updatedAt),
)

fun Place.toEntity(): PlaceEntity = PlaceEntity(
    id = id,
    name = name,
    address = address,
    latitude = latitude,
    longitude = longitude,
    source = source.name,
    providerName = providerName,
    externalId = externalId,
    createdAt = createdAt.toEpochMilli(),
    updatedAt = updatedAt.toEpochMilli(),
)
