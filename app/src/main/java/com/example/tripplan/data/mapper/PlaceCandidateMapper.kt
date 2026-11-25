package com.example.tripplan.data.mapper

import com.example.tripplan.data.local.PlaceCandidateWithDate
import com.example.tripplan.domain.model.PlaceCandidate
import java.time.LocalDate

fun PlaceCandidateWithDate.toDomain() = PlaceCandidate(
    id = candidate.id,
    name = candidate.name,
    address = candidate.address,
    note = candidate.note,
    scheduledDate = scheduledDate?.let(LocalDate::parse),
)
