package com.example.tripplan.presentation.file

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tripplan.domain.model.ExportedTripPlanFile
import com.example.tripplan.domain.model.TripPlanFileFailure
import com.example.tripplan.domain.model.TripPlanFileResult
import com.example.tripplan.domain.model.TripPlanImportPreview
import com.example.tripplan.domain.usecase.CreateTripPlanShareDocumentUseCase
import com.example.tripplan.domain.usecase.ExportTripPlanUseCase
import com.example.tripplan.domain.usecase.ImportTripPlanUseCase
import com.example.tripplan.domain.usecase.PreviewTripPlanImportUseCase
import com.example.tripplan.domain.usecase.SaveTripPlanDocumentUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class TripPlanExportViewModel @Inject constructor(
    private val exportTripPlan: ExportTripPlanUseCase,
    private val saveDocument: SaveTripPlanDocumentUseCase,
    private val createShareDocument: CreateTripPlanShareDocumentUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(TripPlanExportUiState())
    val uiState: StateFlow<TripPlanExportUiState> = _uiState.asStateFlow()
    private val eventChannel = Channel<TripPlanExportEvent>(Channel.BUFFERED)
    val events = eventChannel.receiveAsFlow()
    private var exportedFile: ExportedTripPlanFile? = null

    fun load(tripPlanId: Long) {
        if (_uiState.value.tripPlanId == tripPlanId) return
        _uiState.value = TripPlanExportUiState(tripPlanId = tripPlanId, isLoading = true)
        viewModelScope.launch {
            when (val result = exportTripPlan(tripPlanId)) {
                is TripPlanFileResult.Success -> {
                    exportedFile = result.value
                    _uiState.update {
                        it.copy(isLoading = false, fileName = result.value.fileName)
                    }
                }
                is TripPlanFileResult.Failure -> _uiState.update {
                    it.copy(isLoading = false, failure = result.reason)
                }
            }
        }
    }

    fun onSaveDestinationSelected(uri: String?) {
        val file = exportedFile ?: return
        if (uri == null) return
        viewModelScope.launch {
            _uiState.update { it.copy(isWorking = true, failure = null, isSaved = false) }
            when (val result = saveDocument(uri, file.content)) {
                is TripPlanFileResult.Success -> _uiState.update {
                    it.copy(isWorking = false, isSaved = true)
                }
                is TripPlanFileResult.Failure -> _uiState.update {
                    it.copy(isWorking = false, failure = result.reason)
                }
            }
        }
    }

    fun onShareClick() {
        val file = exportedFile ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isWorking = true, failure = null) }
            when (val result = createShareDocument(file)) {
                is TripPlanFileResult.Success -> {
                    _uiState.update { it.copy(isWorking = false) }
                    eventChannel.send(TripPlanExportEvent.Share(result.value))
                }
                is TripPlanFileResult.Failure -> _uiState.update {
                    it.copy(isWorking = false, failure = result.reason)
                }
            }
        }
    }
}

data class TripPlanExportUiState(
    val tripPlanId: Long? = null,
    val fileName: String = "",
    val isLoading: Boolean = false,
    val isWorking: Boolean = false,
    val isSaved: Boolean = false,
    val failure: TripPlanFileFailure? = null,
)

sealed interface TripPlanExportEvent {
    data class Share(val uri: String) : TripPlanExportEvent
}

@HiltViewModel
class TripPlanImportViewModel @Inject constructor(
    private val previewImport: PreviewTripPlanImportUseCase,
    private val importTripPlan: ImportTripPlanUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(TripPlanImportUiState())
    val uiState: StateFlow<TripPlanImportUiState> = _uiState.asStateFlow()
    private val eventChannel = Channel<TripPlanImportEvent>(Channel.BUFFERED)
    val events = eventChannel.receiveAsFlow()
    private var content: String? = null

    fun load(uri: String) {
        if (_uiState.value.uri == uri) return
        _uiState.value = TripPlanImportUiState(uri = uri, isLoading = true)
        viewModelScope.launch {
            when (val result = previewImport(uri)) {
                is TripPlanFileResult.Success -> {
                    content = result.value.first
                    _uiState.update {
                        it.copy(isLoading = false, preview = result.value.second)
                    }
                }
                is TripPlanFileResult.Failure -> _uiState.update {
                    it.copy(isLoading = false, failure = result.reason)
                }
            }
        }
    }

    fun onImportClick() {
        val source = content ?: return
        if (_uiState.value.isImporting) return
        viewModelScope.launch {
            _uiState.update { it.copy(isImporting = true, failure = null) }
            when (val result = importTripPlan(source)) {
                is TripPlanFileResult.Success -> {
                    _uiState.update { it.copy(isImporting = false) }
                    eventChannel.send(TripPlanImportEvent.Imported(result.value))
                }
                is TripPlanFileResult.Failure -> _uiState.update {
                    it.copy(isImporting = false, failure = result.reason)
                }
            }
        }
    }
}

data class TripPlanImportUiState(
    val uri: String? = null,
    val preview: TripPlanImportPreview? = null,
    val isLoading: Boolean = false,
    val isImporting: Boolean = false,
    val failure: TripPlanFileFailure? = null,
)

sealed interface TripPlanImportEvent {
    data class Imported(val tripPlanId: Long) : TripPlanImportEvent
}
