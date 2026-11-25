package com.example.tripplan.presentation.itinerary

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectableGroup
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.tripplan.R
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Composable
fun ItineraryEditorScreen(
    tripPlanId: Long,
    itemId: Long?,
    initialDate: LocalDate?,
    sessionId: String,
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {},
    onCompleted: () -> Unit = {},
    viewModel: ItineraryEditorViewModel = hiltViewModel(
        key = if (itemId == null) "itinerary-editor-$tripPlanId-new-$sessionId"
            else "itinerary-editor-$tripPlanId-item-$itemId",
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
        onPlaceSuggestionSelected = viewModel::onPlaceSuggestionSelected,
        onDateSelected = viewModel::onDateSelected,
        onStartTimeChange = viewModel::onStartTimeChange,
        onEndTimeChange = viewModel::onEndTimeChange,
        onNoteChange = viewModel::onNoteChange,
        onSaveClick = viewModel::onSaveClick,
        onSaveAndContinueClick = viewModel::onSaveAndContinueClick,
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
    onPlaceSuggestionSelected: (PlaceSuggestionUiModel) -> Unit = {},
    onDateSelected: (LocalDate) -> Unit = {},
    onStartTimeChange: (String) -> Unit = {},
    onEndTimeChange: (String) -> Unit = {},
    onNoteChange: (String) -> Unit = {},
    onSaveClick: () -> Unit = {},
    onSaveAndContinueClick: () -> Unit = {},
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
                    onPlaceSuggestionSelected = onPlaceSuggestionSelected,
                    onDateSelected = onDateSelected,
                    onStartTimeChange = onStartTimeChange,
                    onEndTimeChange = onEndTimeChange,
                    onNoteChange = onNoteChange,
                    onSaveClick = onSaveClick,
                    onSaveAndContinueClick = onSaveAndContinueClick,
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
    onPlaceSuggestionSelected: (PlaceSuggestionUiModel) -> Unit,
    onDateSelected: (LocalDate) -> Unit,
    onStartTimeChange: (String) -> Unit,
    onEndTimeChange: (String) -> Unit,
    onNoteChange: (String) -> Unit,
    onSaveClick: () -> Unit,
    onSaveAndContinueClick: () -> Unit,
    onDeleteRequest: () -> Unit,
) {
    var timePickerTarget by rememberSaveable { mutableStateOf<TimePickerTarget?>(null) }

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
                    placeholder = { Text("관광지, 식당, 숙소 등을 검색하세요.") },
                    singleLine = true,
                )
                if (uiState.isPlaceSearchLoading) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }
                if (uiState.placeSuggestions.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            "Google Places 검색 결과",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        uiState.placeSuggestions.forEach { suggestion ->
                            TextButton(
                                onClick = { onPlaceSuggestionSelected(suggestion) },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Text(suggestion.name, style = MaterialTheme.typography.bodyLarge)
                                    if (suggestion.addressHint.isNotBlank()) {
                                        Text(
                                            suggestion.addressHint,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                if (uiState.selectedPlaceAddress.isNotBlank()) {
                    Text(
                        uiState.selectedPlaceAddress,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                GoogleMapsButton(uiState)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    TimeSelectionField(
                        label = "시작 시간",
                        value = uiState.startTime,
                        modifier = Modifier.weight(1f),
                        isError = uiState.isTimeError,
                        testTag = "start_time_picker",
                        onSelectClick = { timePickerTarget = TimePickerTarget.Start },
                        onClearClick = { onStartTimeChange("") },
                    )
                    TimeSelectionField(
                        label = "종료 시간",
                        value = uiState.endTime,
                        modifier = Modifier.weight(1f),
                        isError = uiState.isTimeError,
                        testTag = "end_time_picker",
                        onSelectClick = { timePickerTarget = TimePickerTarget.End },
                        onClearClick = { onEndTimeChange("") },
                    )
                }
                if (uiState.isTimeError) {
                    Text(
                        "종료 시간은 시작 시간보다 빠를 수 없습니다.",
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
                if (uiState.isEdit) {
                    Button(
                        onClick = onSaveClick,
                        enabled = !uiState.isSaving,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                    ) {
                        if (uiState.isSaving) CircularProgressIndicator() else Text("일정 저장")
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        OutlinedButton(
                            onClick = onSaveClick,
                            enabled = !uiState.isSaving,
                            modifier = Modifier.weight(1f).heightIn(min = 52.dp),
                        ) { Text("일정 추가") }
                        Button(
                            onClick = onSaveAndContinueClick,
                            enabled = !uiState.isSaving,
                            modifier = Modifier.weight(1f).heightIn(min = 52.dp),
                        ) {
                            if (uiState.isSaving) CircularProgressIndicator() else Text("다음 일정")
                        }
                    }
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

    timePickerTarget?.let { target ->
        ScheduleTimePickerDialog(
            title = if (target == TimePickerTarget.Start) "시작 시간 선택" else "종료 시간 선택",
            initialTime = initialPickerTime(
                target = target,
                startTime = uiState.startTime,
                endTime = uiState.endTime,
            ),
            onDismiss = { timePickerTarget = null },
            onConfirm = { selectedTime ->
                val formattedTime = selectedTime.format(timeFormatter)
                if (target == TimePickerTarget.Start) {
                    onStartTimeChange(formattedTime)
                } else {
                    onEndTimeChange(formattedTime)
                }
                timePickerTarget = null
            },
        )
    }
}

@Composable
private fun TimeSelectionField(
    label: String,
    value: String,
    isError: Boolean,
    testTag: String,
    onSelectClick: () -> Unit,
    onClearClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
        )
        OutlinedButton(
            onClick = onSelectClick,
            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).testTag(testTag),
        ) {
            Text(value.ifBlank { "시간 선택" })
        }
        if (value.isNotBlank()) {
            TextButton(
                onClick = onClearClick,
                modifier = Modifier.align(Alignment.End).testTag("${testTag}_clear"),
            ) {
                Text("지우기")
            }
        }
    }
}

@Composable
internal fun ScheduleTimePickerDialog(
    title: String,
    initialTime: LocalTime,
    onDismiss: () -> Unit,
    onConfirm: (LocalTime) -> Unit,
) {
    var hour by rememberSaveable { mutableStateOf(initialTime.hour) }
    var minute by rememberSaveable { mutableStateOf(initialTime.minute) }
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("schedule_time_picker_dialog"),
        title = { Text(title) },
        text = {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TimeValueList(
                    label = "시",
                    optionCount = 24,
                    selectedValue = hour,
                    onValueSelected = { hour = it },
                    modifier = Modifier.weight(1f),
                )
                TimeValueList(
                    label = "분",
                    optionCount = 60,
                    selectedValue = minute,
                    onValueSelected = { minute = it },
                    modifier = Modifier.weight(1f),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(LocalTime.of(hour, minute)) }) {
                Text("확인")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("취소") } },
    )
}

@Composable
private fun TimeValueList(
    label: String,
    optionCount: Int,
    selectedValue: Int,
    onValueSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = (selectedValue - 1).coerceAtLeast(0))
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelLarge)
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxWidth().height(192.dp).selectableGroup().testTag("time_values_$label"),
        ) {
            items(optionCount) { value ->
                FilterChip(
                    selected = value == selectedValue,
                    onClick = { onValueSelected(value) },
                    label = { Text("${value.toString().padStart(2, '0')}$label") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ScheduleTimePickerPreview() {
    MaterialTheme {
        ScheduleTimePickerDialog(
            title = "시작 시간 선택",
            initialTime = LocalTime.of(9, 30),
            onDismiss = {},
            onConfirm = {},
        )
    }
}

private fun initialPickerTime(
    target: TimePickerTarget,
    startTime: String,
    endTime: String,
): LocalTime {
    val start = startTime.toLocalTimeOrNull()
    val end = endTime.toLocalTimeOrNull()
    return when (target) {
        TimePickerTarget.Start -> start ?: LocalTime.of(9, 0)
        TimePickerTarget.End -> end ?: start?.plusHours(1) ?: LocalTime.of(10, 0)
    }
}

private fun String.toLocalTimeOrNull(): LocalTime? = runCatching {
    LocalTime.parse(this, timeFormatter)
}.getOrNull()

private enum class TimePickerTarget { Start, End }

@Composable
private fun GoogleMapsButton(uiState: ItineraryEditorUiState) {
    val context = LocalContext.current
    val mapUri = uiState.googleMapsUri() ?: return
    OutlinedButton(
        onClick = {
            context.startActivity(Intent(Intent.ACTION_VIEW, mapUri))
        },
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text("Google 지도에서 보기")
    }
}

private fun ItineraryEditorUiState.googleMapsUri(): Uri? {
    val query = selectedPlaceAddress.ifBlank { place }
    return when {
        selectedExternalPlaceId != null -> Uri.parse(
            "https://www.google.com/maps/search/?api=1&query=" +
                Uri.encode(query) + "&query_place_id=" + Uri.encode(selectedExternalPlaceId)
        )
        selectedPlaceLatitude != null && selectedPlaceLongitude != null -> Uri.parse(
            "https://www.google.com/maps/search/?api=1&query=" +
                Uri.encode("$selectedPlaceLatitude,$selectedPlaceLongitude")
        )
        else -> null
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
private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
