package com.example.tripplan.domain.repository

import com.example.tripplan.domain.model.ExportedTripPlanFile
import com.example.tripplan.domain.model.TripPlanFileResult
import com.example.tripplan.domain.model.TripPlanImportPreview

interface TripPlanFileRepository {
    suspend fun exportTrip(tripPlanId: Long): TripPlanFileResult<ExportedTripPlanFile>
    fun previewImport(content: String): TripPlanFileResult<TripPlanImportPreview>
    suspend fun importTrip(content: String): TripPlanFileResult<Long>
    suspend fun readDocument(uri: String): TripPlanFileResult<String>
    suspend fun writeDocument(uri: String, content: String): TripPlanFileResult<Unit>
    suspend fun createShareDocument(fileName: String, content: String): TripPlanFileResult<String>
}
