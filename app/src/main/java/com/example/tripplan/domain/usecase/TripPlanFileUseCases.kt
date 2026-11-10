package com.example.tripplan.domain.usecase

import com.example.tripplan.domain.model.ExportedTripPlanFile
import com.example.tripplan.domain.model.TripPlanFileResult
import com.example.tripplan.domain.model.TripPlanImportPreview
import com.example.tripplan.domain.repository.TripPlanFileRepository
import javax.inject.Inject

class ExportTripPlanUseCase @Inject constructor(
    private val repository: TripPlanFileRepository,
) {
    suspend operator fun invoke(tripPlanId: Long): TripPlanFileResult<ExportedTripPlanFile> =
        repository.exportTrip(tripPlanId)
}

class PreviewTripPlanImportUseCase @Inject constructor(
    private val repository: TripPlanFileRepository,
) {
    suspend operator fun invoke(uri: String): TripPlanFileResult<Pair<String, TripPlanImportPreview>> {
        return when (val read = repository.readDocument(uri)) {
            is TripPlanFileResult.Failure -> read
            is TripPlanFileResult.Success -> when (val preview = repository.previewImport(read.value)) {
                is TripPlanFileResult.Failure -> preview
                is TripPlanFileResult.Success -> TripPlanFileResult.Success(read.value to preview.value)
            }
        }
    }
}

class ImportTripPlanUseCase @Inject constructor(
    private val repository: TripPlanFileRepository,
) {
    suspend operator fun invoke(content: String): TripPlanFileResult<Long> =
        repository.importTrip(content)
}

class SaveTripPlanDocumentUseCase @Inject constructor(
    private val repository: TripPlanFileRepository,
) {
    suspend operator fun invoke(uri: String, content: String): TripPlanFileResult<Unit> =
        repository.writeDocument(uri, content)
}

class CreateTripPlanShareDocumentUseCase @Inject constructor(
    private val repository: TripPlanFileRepository,
) {
    suspend operator fun invoke(file: ExportedTripPlanFile): TripPlanFileResult<String> =
        repository.createShareDocument(file.fileName, file.content)
}
