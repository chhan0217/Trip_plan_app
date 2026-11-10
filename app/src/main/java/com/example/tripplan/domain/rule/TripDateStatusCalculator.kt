package com.example.tripplan.domain.rule

import com.example.tripplan.domain.model.TripDatePhase
import com.example.tripplan.domain.model.TripDateStatus
import java.time.LocalDate
import java.time.temporal.ChronoUnit

object TripDateStatusCalculator {
    fun calculate(
        startDate: LocalDate,
        endDate: LocalDate,
        today: LocalDate,
    ): TripDateStatus {
        require(!endDate.isBefore(startDate)) { "Trip end date must not be before start date" }
        return when {
            today.isBefore(startDate) -> TripDateStatus(
                phase = TripDatePhase.Upcoming,
                daysUntilStart = ChronoUnit.DAYS.between(today, startDate),
            )
            today.isAfter(endDate) -> TripDateStatus(phase = TripDatePhase.Ended)
            else -> TripDateStatus(
                phase = TripDatePhase.Ongoing,
                currentDayNumber = ChronoUnit.DAYS.between(startDate, today).toInt() + 1,
                isLastDay = today == endDate,
            )
        }
    }
}
