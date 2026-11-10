package com.example.tripplan.domain.model

import java.time.Instant

enum class PackingItemType {
    Shared,
    Personal,
}

data class PackingItem(
    val id: Long,
    val tripPlanId: Long,
    val name: String,
    val category: String,
    val quantity: Int,
    val memo: String,
    val type: PackingItemType,
    val checkedAt: Instant?,
    val order: Int,
)

data class PackingItemInput(
    val name: String,
    val category: String = "",
    val quantity: Int = 1,
    val memo: String = "",
    val type: PackingItemType,
)
