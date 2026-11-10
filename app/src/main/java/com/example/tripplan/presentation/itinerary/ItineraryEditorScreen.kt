package com.example.tripplan.presentation.itinerary

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.tripplan.R
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun ItineraryEditorScreen(
    tripPlanId: Long,
    itemId: Long?,
    initialDate: LocalDate?,
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {},
    onCompleted: () -> Unit = {},
    viewModel: ItineraryEditorViewModel = hiltViewModel(
        key = "itinerary-editor-$tripPlanId-${itemId ?: "new"}",
    ),
) {
    val uiState by viewModel.uiState.collectAsState()
    LaunchedEffect(viewModel, tripPlanId, itemId, initialDate) {
        viewModel.load(tripPlanId, itemId, initialDate)
    }
    LaunchedEffect(viewModel, onCompleted) {
        viewModel.events.collect { onCompleted() }
    }

    ItineraryEditorScreenContent(
        uiState = uiState,
        modifier = modifier,
        onBackClick = onBackClick,
        onTitleChange = viewModel::onTitleChange,
        onPlaceChange = viewModel::onPlaceChange,
        onSavedPlaceSelected = viewModel::onSavedPlaceSelected,
        onDateSelected = viewModel::onDateSelected,
        onStartTimeChange = viewModel::onStartTimeChange,
        onEndTimeChange = viewModel::onEndTimeChange,
        onNoteChange = viewModel::onNoteChange,
        onSaveClick = viewModel::onSaveClick,
        onDeleteRequest = viewModel::onDeleteRequest,
        onDeleteDismiss = viewModel::onDeleteDismiss,
        onDeleteConfirm = viewModel::onDeleteConfirm,
    )
}

@Composable
internal fun ItineraryEditorScreenContent(
    uiState: ItineraryEditorUiState,
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {},
    onTitleChange: (String) -> Unit = {},
    onPlaceChange: (String) -> Unit = {},
    onSavedPlaceSelected: (Long) -> Unit = {},
    onDateSelected: (LocalDate) -> Unit = {},
    onStartTimeChange: (String) -> Unit = {},
    onEndTimeChange: (String) -> Unit = {},
    onNoteChange: (String) -> Unit = {},
    onSaveClick: () -> Unit = {},
    onDeleteRequest: () -> Unit = {},
    onDeleteDismiss: () -> Unit = {},
    onDeleteConfirm: () -> Unit = {},
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing,
    ) { innerPadding ->
        Column(Modifier.fillMaxSize().padding(innerPadding)) {
            EditorHeader(isEdit = uiState.isEdit, onBackClick = onBackClick)
            when {
                uiState.isLoading -> Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) { CircularProgressIndicator() }
                uiState.isNotFound -> EditorMessage("일정 또는 여행을 찾을 수 없습니다.")
                uiState.isLoadFailed -> EditorMessage("일정을 불러오지 못했습니다.", isError = true)
                else -> EditorForm(
                    uiState = uiState,
                    onTitleChange = onTitleChange,
                    onPlaceChange = onPlaceChange,
                    onSavedPlaceSelected = onSavedPlaceSelected,
                    onDateSelected = onDateSelected,
                    onStartTimeChange = onStartTimeChange,
                    onEndTimeChange = onEndTimeChange,
                    onNoteChange = onNoteChange,
                    onSaveClick = onSaveClick,
                    onDeleteRequest = onDeleteRequest,
                )
            }
        }
    }

    if (uiState.isDeleteConfirmationVisible) {
        AlertDialog(
            onDismissRequest = onDeleteDismiss,
            title = { Text("일정 삭제") },
            text = { Text("이 일정을 삭제할까요? 삭제한 일정은 복구할 수 없습니다.") },
            confirmButton = { TextButton(onClick = onDeleteConfirm) { Text("삭제") } },
            dismissButton = { TextButton(onClick = onDeleteDismiss) { Text("취소") } },
        )
    }
}

@Composable
private fun EditorHeader(isEdit: Boolean, onBackClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBackClick) {
            Icon(
                imageVector = ImageVector.vectorResource(R.drawable.ic_arrow_back_24),
                contentDescription = "뒤로가기",
            )
        }
        Text(if (isEdit) "일정 수정" else "일정 추가", style = MaterialTheme.typography.titleLarge)
    }
}

@Composable
private fun EditorMessage(message: String, isError: Boolean = false) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            message,
            color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun EditorForm(
    uiState: ItineraryEditorUiState,
    onTitleChange: (String) -> Unit,
    onPlaceChange: (String) -> Unit,
    onSavedPlaceSelected: (Long) -> Unit,
    onDateSelected: (LocalDate) -> Unit,
    onStartTimeChange: (String) -> Unit,
    onEndTimeChange: (String) -> Unit,
    onNoteChange: (String) -> Unit,
    onSaveClick: () -> Unit,
    onDeleteRequest: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp),
            ) {
                items(uiState.days, key = { it.date.toString() }) { day ->
                    FilterChip(
                        selected = day.date == uiState.selectedDate,
                        onClick = { onDateSelected(day.date) },
                        label = {
                            Text("${day.dayNumber}일차 ${day.date.format(dayFormatter)}")
                        },
                    )
                }
            }
        }
        item {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                OutlinedTextField(
                    value = uiState.title,
                    onValueChange = onTitleChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("일정 제목") },
                    supportingText = if (uiState.isTitleError) {
                        { Text("일정 제목을 입력해 주세요.") }
                    } else null,
                    isError = uiState.isTitleError,
                    singleLine = true,
                )
                if (uiState.savedPlaces.isNotEmpty()) {
                    Text("저장된 장소 선택", style = MaterialTheme.typography.labelLarge)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(uiState.savedPlaces, key = { it.id }) { place ->
                            FilterChip(
                                selected = uiState.selectedPlaceId == place.id,
                                onClick = { onSavedPlaceSelected(place.id) },
                                label = { Text(place.name) },
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = uiState.place,
                    onValueChange = onPlaceChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("장소") },
                    singleLine = true,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    OutlinedTextField(
                        value = uiState.startTime,
                        onValueChange = onStartTimeChange,
                        modifier = Modifier.weight(1f),
                        label = { Text("시작 시간") },
                        placeholder = { Text("09:00") },
                        isError = uiState.isTimeError,
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = uiState.endTime,
                        onValueChange = onEndTimeChange,
                        modifier = Modifier.weight(1f),
                        label = { Text("종료 시간") },
                        placeholder = { Text("10:30") },
                        isError = uiState.isTimeError,
                        singleLine = true,
                    )
                }
                if (uiState.isTimeError) {
                    Text(
                        "시간은 HH:mm 형식이며 종료 시간은 시작 시간보다 빠를 수 없습니다.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                OutlinedTextField(
                    value = uiState.note,
                    onValueChange = onNoteChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("메모") },
                    minLines = 3,
                    maxLines = 5,
                )
                if (uiState.isSaveFailed) {
                    Text(
                        "일정을 저장하지 못했습니다. 다시 시도해 주세요.",
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                Button(
                    onClick = onSaveClick,
                    enabled = !uiState.isSaving,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                ) {
                    if (uiState.isSaving) CircularProgressIndicator()
                    else Text(if (uiState.isEdit) "일정 저장" else "일정 추가")
                }
                if (uiState.isEdit) {
                    OutlinedButton(
                        onClick = onDeleteRequest,
                        enabled = !uiState.isSaving,
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("일정 삭제") }
                }
            }
        }
        item { Text("", modifier = Modifier.padding(bottom = 8.dp)) }
    }
}

private val previewDays = listOf(
    EditorDayUiModel(1, LocalDate.of(2026, 11, 9)),
    EditorDayUiModel(2, LocalDate.of(2026, 11, 10)),
)

@Preview(showBackground = true)
@Composable
private fun ItineraryCreatePreview() {
    MaterialTheme {
        ItineraryEditorScreenContent(
            uiState = ItineraryEditorUiState(
                isLoading = false,
                days = previewDays,
                selectedDate = previewDays.first().date,
            )
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ItineraryEditPreview() {
    MaterialTheme {
        ItineraryEditorScreenContent(
            uiState = ItineraryEditorUiState(
                isLoading = false,
                isEdit = true,
                itemId = 1L,
                days = previewDays,
                selectedDate = previewDays.first().date,
                title = "성산일출봉",
                place = "서귀포시",
                startTime = "09:00",
                endTime = "11:00",
                note = "입장권 확인",
            )
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ItineraryErrorPreview() {
    MaterialTheme {
        ItineraryEditorScreenContent(
            uiState = ItineraryEditorUiState(
                isLoading = false,
                days = previewDays,
                selectedDate = previewDays.first().date,
                isTitleError = true,
                isTimeError = true,
                isSaveFailed = true,
            )
        )
    }
}

private val dayFormatter = DateTimeFormatter.ofPattern("MM.dd")
