package com.example.tripplan.domain.usecase

import com.example.tripplan.domain.model.Lodging
import com.example.tripplan.domain.repository.LodgingRepository
import com.example.tripplan.domain.repository.LodgingSaveResult
import javax.inject.Inject

class ManageLodgingsUseCase @Inject constructor(private val repository: LodgingRepository) {
    fun observe(tripId: Long) = repository.observe(tripId)

    suspend fun save(tripId: Long, lodging: Lodging): LodgingSaveResult {
        if (lodging.name.isBlank()) return LodgingSaveResult.Unavailable
        if (lodging.checkIn >= lodging.checkOut) return LodgingSaveResult.InvalidDates
        return repository.save(
            tripId,
            lodging.copy(name = lodging.name.trim(), address = lodging.address.trim(), note = lodging.note.trim())
        )
    }

    suspend fun remove(tripId: Long, id: Long) = repository.remove(tripId, id)
}
