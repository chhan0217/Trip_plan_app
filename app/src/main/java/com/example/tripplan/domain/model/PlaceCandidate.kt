package com.example.tripplan.domain.model

import java.time.LocalDate

data class PlaceCandidate(
    val id: Long,
    val name: String,
    val address: String,
    val note: String,
    val scheduledDate: LocalDate?,
)
