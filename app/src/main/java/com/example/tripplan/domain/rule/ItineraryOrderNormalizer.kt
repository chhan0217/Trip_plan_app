package com.example.tripplan.domain.rule

import com.example.tripplan.domain.model.ItineraryItem

object ItineraryOrderNormalizer {
    fun normalize(items: List<ItineraryItem>): List<ItineraryItem> {
        return items
            .sortedWith(compareBy(ItineraryItem::order, ItineraryItem::id))
            .mapIndexed { index, item -> item.copy(order = index) }
    }
}
