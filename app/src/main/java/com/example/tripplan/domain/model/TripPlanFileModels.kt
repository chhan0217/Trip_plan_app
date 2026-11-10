package com.example.tripplan.domain.model

import java.time.LocalDate

data class ExportedTripPlanFile(
    val fileName: String,
    val content: String,
)

data class TripPlanImportPreview(
    val title: String,
    val destination: String,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val dayCount: Int,
    val itineraryItemCount: Int,
)

sealed interface TripPlanFileResult<out T> {
    data class Success<T>(val value: T) : TripPlanFileResult<T>
    data class Failure(val reason: TripPlanFileFailure) : TripPlanFileResult<Nothing>
}

enum class TripPlanFileFailure {
    TripNotFound,
    InvalidFormat,
    UnsupportedVersion,
    InvalidData,
    ReadFailed,
    WriteFailed,
}
