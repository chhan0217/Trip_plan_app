package com.example.tripplan.domain.model

import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.ChronoUnit

data class Lodging(
    val id: Long = 0,
    val name: String,
    val checkIn: LocalDate,
    val checkOut: LocalDate,
    val address: String = "",
    val checkInTime: LocalTime? = null,
    val note: String = "",
    val checkOutTime: LocalTime? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val googlePlaceId: String? = null,
) {
    val nights: Long get() = ChronoUnit.DAYS.between(checkIn, checkOut)

    fun overlaps(other: Lodging): Boolean = checkIn < other.checkOut && other.checkIn < checkOut
}
