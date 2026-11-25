package com.example.tripplan.presentation.itineraryphoto

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tripplan.domain.model.RuntimePermission
import com.example.tripplan.domain.model.RuntimePermissionStatus
import com.example.tripplan.domain.usecase.GetRuntimePermissionStatusUseCase
import com.example.tripplan.domain.usecase.ManageItineraryPhotosUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@HiltViewModel
class ItineraryPhotoViewModel @Inject constructor(
    private val photos: ManageItineraryPhotosUseCase,
    private val permissionStatus: GetRuntimePermissionStatusUseCase,
) : ViewModel() {
    private val state = MutableStateFlow(ItineraryPhotoUiState())
    val uiState = state.asStateFlow()
    private var tripId: Long? = null
    private var observationJob: Job? = null

    fun load(id: Long) {
        if (tripId == id) return
        tripId = id
        observationJob?.cancel()
        state.value = ItineraryPhotoUiState()
        observationJob = viewModelScope.launch {
            try {
                photos.observe(id).collect { refs ->
                    state.update { current ->
                        current.copy(photoIdsByItinerary = refs.groupBy({ it.itineraryId }, { it.id }))
                    }
                }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                showError("저장된 사진 목록을 불러오지 못했습니다.")
            }
        }
    }

    suspend fun readPhoto(photoId: Long): ByteArray? {
        val id = tripId ?: return null
        return photos.read(id, photoId)
    }

    fun cameraStatus(wasRequested: Boolean, shouldShowRationale: Boolean): RuntimePermissionStatus =
        permissionStatus(RuntimePermission.Camera, wasRequested, shouldShowRationale)

    fun prepareCapture(onReady: (String) -> Unit) {
        if (state.value.isSaving) return
        state.update { it.copy(isSaving = true, error = null) }
        viewModelScope.launch {
            try {
                onReady(photos.prepareCapture())
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                showError("카메라 촬영을 시작하지 못했습니다.")
            } finally {
                state.update { it.copy(isSaving = false) }
            }
        }
    }

    fun handleCapture(itineraryId: Long?, target: String, captured: Boolean) {
        if (captured && itineraryId != null) {
            save(itineraryId, listOf(target), captureTarget = target)
        } else {
            viewModelScope.launch { discardCapture(target) }
        }
    }

    fun addPickedPhotos(itineraryId: Long, sources: List<String>) = save(itineraryId, sources)

    fun showError(message: String) = state.update { it.copy(error = message) }

    fun clearError() = state.update { it.copy(error = null) }

    private fun save(itineraryId: Long, sources: List<String>, captureTarget: String? = null) {
        val id = tripId
        if (id == null || sources.isEmpty() || state.value.isSaving) {
            if (captureTarget != null) viewModelScope.launch { discardCapture(captureTarget) }
            return
        }
        state.update { it.copy(isSaving = true, error = null) }
        viewModelScope.launch {
            var saved = 0
            try {
                for (source in sources) {
                    try {
                        if (photos.add(id, itineraryId, source)) saved++
                    } catch (error: CancellationException) {
                        throw error
                    } catch (_: Exception) {
                        // A bad image does not prevent saving the rest of the selection.
                    }
                }
                if (saved < sources.size) {
                    showError(if (saved == 0) "사진을 저장하지 못했습니다." else "일부 사진을 저장하지 못했습니다.")
                }
            } finally {
                if (captureTarget != null) withContext(NonCancellable) { discardCapture(captureTarget) }
                state.update { it.copy(isSaving = false) }
            }
        }
    }

    private suspend fun discardCapture(target: String) {
        try {
            photos.discardCapture(target)
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            // The capture is in cache and is also pruned before future captures.
        }
    }
}

data class ItineraryPhotoUiState(
    val photoIdsByItinerary: Map<Long, List<Long>> = emptyMap(),
    val isSaving: Boolean = false,
    val error: String? = null,
)

