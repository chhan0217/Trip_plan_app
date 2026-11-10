package com.example.tripplan.domain.usecase

import com.example.tripplan.domain.model.TripCopyPlanError
import com.example.tripplan.domain.model.TripCopyPlanResult
import com.example.tripplan.domain.model.TripPlanInput
import com.example.tripplan.domain.repository.ItineraryRepository
import com.example.tripplan.domain.repository.TripCopyRepository
import com.example.tripplan.domain.repository.TripPlanRepository
import com.example.tripplan.domain.rule.TripCopyPlanner
import com.example.tripplan.domain.rule.TripPlanValidator
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import kotlinx.coroutines.flow.first

class CopyTripPlanUseCase @Inject constructor(
    private val tripRepository: TripPlanRepository,
    private val itineraryRepository: ItineraryRepository,
    private val copyRepository: TripCopyRepository,
) {
    suspend operator fun invoke(
        sourceTripId: Long,
        input: TripPlanInput,
        includeItinerary: Boolean,
        retainedOriginalDayNumbers: Set<Int> = emptySet(),
    ): CopyTripResult {
        if (TripPlanValidator.validate(input).isNotEmpty()) return CopyTripResult.Invalid
        val source = tripRepository.getTripPlan(sourceTripId) ?: return CopyTripResult.NotFound
        val planResult = TripCopyPlanner.plan(
            source.startDate, source.endDate, input.startDate, input.endDate,
            includeItinerary, retainedOriginalDayNumbers,
        )
        val plan = when (planResult) {
            is TripCopyPlanResult.Invalid -> return CopyTripResult.InvalidDays(planResult.errors)
            is TripCopyPlanResult.Success -> planResult.plan
        }
        val mapping = plan.dayMappings.associateBy { it.originalDayNumber }
        val copiedItems = if (!includeItinerary) emptyList() else {
            itineraryRepository.observeItems(sourceTripId).first().mapNotNull { item ->
                val day = ChronoUnit.DAYS.between(source.startDate, item.date).toInt() + 1
                mapping[day]?.let { item.copy(id = 0, tripPlanId = 0, date = it.newDate) }
            }
        }
        return CopyTripResult.Success(copyRepository.copyTrip(input, copiedItems))
    }
}

sealed interface CopyTripResult {
    data class Success(val tripPlanId: Long) : CopyTripResult
    data class InvalidDays(val errors: Set<TripCopyPlanError>) : CopyTripResult
    data object Invalid : CopyTripResult
    data object NotFound : CopyTripResult
}
