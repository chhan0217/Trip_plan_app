package com.example.tripplan.presentation.file

import com.example.tripplan.domain.model.ExportedTripPlanFile
import com.example.tripplan.domain.model.TripPlanFileFailure
import com.example.tripplan.domain.model.TripPlanFileResult
import com.example.tripplan.domain.model.TripPlanImportPreview
import com.example.tripplan.domain.repository.TripPlanFileRepository
import com.example.tripplan.domain.usecase.CreateTripPlanShareDocumentUseCase
import com.example.tripplan.domain.usecase.ExportTripPlanUseCase
import com.example.tripplan.domain.usecase.ImportTripPlanUseCase
import com.example.tripplan.domain.usecase.PreviewTripPlanImportUseCase
import com.example.tripplan.domain.usecase.SaveTripPlanDocumentUseCase
import com.example.tripplan.testutil.MainDispatcherRule
import java.time.LocalDate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TripPlanFileViewModelsTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun exportLoad_success_exposesFileName() = runTest {
        val repository = FakeFileRepository()
        val viewModel = exportViewModel(repository)

        viewModel.load(7L)
        advanceUntilIdle()

        assertEquals(7L, repository.exportedTripId)
        assertEquals("제주.tripplan.json", viewModel.uiState.value.fileName)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun exportSave_failure_exposesReason() = runTest {
        val repository = FakeFileRepository().apply {
            writeResult = TripPlanFileResult.Failure(TripPlanFileFailure.WriteFailed)
        }
        val viewModel = exportViewModel(repository)
        viewModel.load(1L)
        advanceUntilIdle()

        viewModel.onSaveDestinationSelected("content://saved")
        advanceUntilIdle()

        assertEquals(TripPlanFileFailure.WriteFailed, viewModel.uiState.value.failure)
        assertEquals("content://saved", repository.writtenUri)
    }

    @Test
    fun exportShare_success_emitsShareUri() = runTest {
        val repository = FakeFileRepository()
        val viewModel = exportViewModel(repository)
        viewModel.load(1L)
        advanceUntilIdle()
        val event = async(UnconfinedTestDispatcher(testScheduler)) { viewModel.events.first() }

        viewModel.onShareClick()
        advanceUntilIdle()

        assertEquals(TripPlanExportEvent.Share("content://shared"), event.await())
    }

    @Test
    fun importLoad_success_exposesPreviewWithoutSaving() = runTest {
        val repository = FakeFileRepository()
        val viewModel = importViewModel(repository)

        viewModel.load("content://source")
        advanceUntilIdle()

        assertEquals("제주 여행", viewModel.uiState.value.preview?.title)
        assertEquals(0, repository.importCount)
    }

    @Test
    fun importClick_success_savesAndEmitsTripId() = runTest {
        val repository = FakeFileRepository()
        val viewModel = importViewModel(repository)
        viewModel.load("content://source")
        advanceUntilIdle()
        val event = async(UnconfinedTestDispatcher(testScheduler)) { viewModel.events.first() }

        viewModel.onImportClick()
        advanceUntilIdle()

        assertEquals(1, repository.importCount)
        assertEquals(TripPlanImportEvent.Imported(42L), event.await())
        assertFalse(viewModel.uiState.value.isImporting)
    }

    private fun exportViewModel(repository: TripPlanFileRepository) = TripPlanExportViewModel(
        ExportTripPlanUseCase(repository),
        SaveTripPlanDocumentUseCase(repository),
        CreateTripPlanShareDocumentUseCase(repository),
    )

    private fun importViewModel(repository: TripPlanFileRepository) = TripPlanImportViewModel(
        PreviewTripPlanImportUseCase(repository),
        ImportTripPlanUseCase(repository),
    )
}

private class FakeFileRepository : TripPlanFileRepository {
    var exportedTripId: Long? = null
    var writtenUri: String? = null
    var importCount = 0
    var writeResult: TripPlanFileResult<Unit> = TripPlanFileResult.Success(Unit)
    private val file = ExportedTripPlanFile("제주.tripplan.json", "valid-content")
    private val preview = TripPlanImportPreview(
        title = "제주 여행",
        destination = "제주",
        startDate = LocalDate.of(2026, 11, 9),
        endDate = LocalDate.of(2026, 11, 11),
        dayCount = 3,
        itineraryItemCount = 2,
    )

    override suspend fun exportTrip(tripPlanId: Long): TripPlanFileResult<ExportedTripPlanFile> {
        exportedTripId = tripPlanId
        return TripPlanFileResult.Success(file)
    }

    override fun previewImport(content: String) = TripPlanFileResult.Success(preview)

    override suspend fun importTrip(content: String): TripPlanFileResult<Long> {
        importCount++
        return TripPlanFileResult.Success(42L)
    }

    override suspend fun readDocument(uri: String) = TripPlanFileResult.Success(file.content)

    override suspend fun writeDocument(uri: String, content: String): TripPlanFileResult<Unit> {
        writtenUri = uri
        return writeResult
    }

    override suspend fun createShareDocument(fileName: String, content: String) =
        TripPlanFileResult.Success("content://shared")
}
