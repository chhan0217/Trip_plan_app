package com.example.tripplan.presentation.itineraryphoto

import com.example.tripplan.domain.model.RuntimePermission
import com.example.tripplan.domain.repository.DeviceCapabilityRepository
import com.example.tripplan.domain.repository.ItineraryPhotoRef
import com.example.tripplan.domain.repository.ItineraryPhotoRepository
import com.example.tripplan.domain.usecase.GetRuntimePermissionStatusUseCase
import com.example.tripplan.domain.usecase.ManageItineraryPhotosUseCase
import com.example.tripplan.testutil.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ItineraryPhotoViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun showsSavedPhotosEvenWhenAnotherPickedPhotoCannotBeRead() = runTest {
        val repository = FakePhotos()
        val viewModel = createViewModel(repository)
        viewModel.load(7)
        viewModel.addPickedPhotos(3, listOf("good", "invalid"))
        advanceUntilIdle()

        assertEquals(mapOf(3L to listOf(1L)), viewModel.uiState.value.photoIdsByItinerary)
        assertEquals("일부 사진을 저장하지 못했습니다.", viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.isSaving)

        assertArrayEquals(byteArrayOf(1), viewModel.readPhoto(1))
    }

    @Test
    fun canceledCaptureDeletesTemporaryTargetWithoutSaving() = runTest {
        val repository = FakePhotos()
        val viewModel = createViewModel(repository)
        viewModel.load(7)
        viewModel.handleCapture(3, "temporary", captured = false)
        advanceUntilIdle()

        assertEquals(listOf("temporary"), repository.discarded)
        assertEquals(emptyMap<Long, List<Long>>(), viewModel.uiState.value.photoIdsByItinerary)
        assertNull(viewModel.uiState.value.error)
    }

    private fun createViewModel(repository: FakePhotos) = ItineraryPhotoViewModel(
        ManageItineraryPhotosUseCase(repository),
        GetRuntimePermissionStatusUseCase(object : DeviceCapabilityRepository {
            override fun isNetworkAvailable() = false
            override fun isPermissionSupported(permission: RuntimePermission) = true
            override fun isPermissionGranted(permission: RuntimePermission) = false
        }),
    )

    private class FakePhotos : ItineraryPhotoRepository {
        private val refs = MutableStateFlow<List<ItineraryPhotoRef>>(emptyList())
        val discarded = mutableListOf<String>()

        override fun observe(tripId: Long) = refs

        override suspend fun getJpeg(tripId: Long, photoId: Long) = byteArrayOf(1)

        override suspend fun add(tripId: Long, itineraryId: Long, source: String): Boolean {
            if (source == "invalid") return false
            refs.value += ItineraryPhotoRef(refs.value.size + 1L, itineraryId)
            return true
        }

        override suspend fun createCaptureTarget() = "temporary"

        override suspend fun discardCaptureTarget(target: String) {
            discarded += target
        }
    }
}
