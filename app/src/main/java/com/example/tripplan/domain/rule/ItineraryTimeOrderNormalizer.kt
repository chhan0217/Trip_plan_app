package com.example.tripplan.domain.rule

import com.example.tripplan.domain.model.ItineraryItem
import java.time.LocalTime

object ItineraryTimeOrderNormalizer {
    fun normalize(items: List<ItineraryItem>): List<ItineraryItem> {
        return items.sortedWith(
            compareBy<ItineraryItem>(
                { it.startTime == null },
                { it.startTime ?: LocalTime.MAX },
                { it.endTime == null },
                { it.endTime ?: LocalTime.MAX },
                { it.order },
                { it.id },
            )
        ).mapIndexed { index, item -> item.copy(order = index) }
    }
}
