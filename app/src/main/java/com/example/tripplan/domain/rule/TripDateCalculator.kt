package com.example.tripplan.domain.rule

import com.example.tripplan.domain.model.TripDay
import java.time.LocalDate
import java.time.temporal.ChronoUnit

object TripDateCalculator {
    fun createDays(
        startDate: LocalDate,
        endDate: LocalDate,
    ): List<TripDay> {
        if (endDate.isBefore(startDate)) return emptyList()

        val dayCount = ChronoUnit.DAYS.between(startDate, endDate).toInt() + 1
        return List(dayCount) { index ->
            TripDay(
                dayNumber = index + 1,
                date = startDate.plusDays(index.toLong()),
            )
        }
    }
}
