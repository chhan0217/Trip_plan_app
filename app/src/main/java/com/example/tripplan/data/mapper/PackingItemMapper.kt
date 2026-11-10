package com.example.tripplan.data.mapper

import com.example.tripplan.data.local.PackingItemEntity
import com.example.tripplan.domain.model.PackingItem
import com.example.tripplan.domain.model.PackingItemType
import java.time.Instant

fun PackingItemEntity.toDomain(): PackingItem = PackingItem(
    id = id,
    tripPlanId = tripPlanId,
    name = name,
    category = category,
    quantity = quantity,
    memo = memo,
    type = PackingItemType.valueOf(type),
    checkedAt = checkedAt?.let(Instant::ofEpochMilli),
    order = sortOrder,
)

fun PackingItem.toEntity(): PackingItemEntity = PackingItemEntity(
    id = id,
    tripPlanId = tripPlanId,
    name = name,
    category = category,
    quantity = quantity,
    memo = memo,
    type = type.name,
    checkedAt = checkedAt?.toEpochMilli(),
    sortOrder = order,
)
