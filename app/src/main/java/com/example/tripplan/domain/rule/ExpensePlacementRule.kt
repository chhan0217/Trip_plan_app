package com.example.tripplan.domain.rule

import com.example.tripplan.domain.model.ItineraryItem
import java.time.LocalDate
import java.time.LocalTime

object ExpensePlacementRule {
    fun findItineraryItemId(
        items: List<ItineraryItem>,
        date: LocalDate,
        time: LocalTime,
    ): Long? = items
        .asSequence()
        .filter { item ->
            item.date == date &&
                item.startTime != null &&
                item.endTime != null &&
                !time.isBefore(item.startTime) &&
                time.isBefore(item.endTime)
        }
        // If schedules overlap, prefer the one that started most recently.
        .maxWithOrNull(compareBy<ItineraryItem> { it.startTime }.thenBy { it.order })
        ?.id
}
