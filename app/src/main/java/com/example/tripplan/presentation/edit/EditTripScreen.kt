package com.example.tripplan.presentation.edit

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
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
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.tripplan.R
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@Composable
fun EditTripScreen(
    tripPlanId: Long,
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {},
    onSaved: () -> Unit = {},
    onDeleted: () -> Unit = {},
    viewModel: EditTripViewModel = hiltViewModel(key = "edit-trip-$tripPlanId"),
) {
    val uiState by viewModel.uiState.collectAsState()
    LaunchedEffect(viewModel, tripPlanId) { viewModel.load(tripPlanId) }
    LaunchedEffect(viewModel, onSaved, onDeleted) {
        viewModel.events.collect { event ->
            when (event) {
                EditTripEvent.Saved -> onSaved()
                EditTripEvent.Deleted -> onDeleted()
            }
        }
    }
    EditTripScreenContent(
        uiState = uiState,
        modifier = modifier,
        onBackClick = onBackClick,
        onTitleChange = viewModel::onTitleChange,
        onDestinationChange = viewModel::onDestinationChange,
        onDateRangeChange = viewModel::onDateRangeChange,
        onDescriptionChange = viewModel::onDescriptionChange,
        onSaveClick = viewModel::onSaveClick,
        onCompletionResetConfirm = viewModel::onCompletionResetConfirm,
        onCompletionResetDismiss = viewModel::onCompletionResetDismiss,
        onOutOfRangeConfirm = viewModel::onOutOfRangeConfirm,
        onOutOfRangeDismiss = viewModel::onOutOfRangeDismiss,
        onDeleteRequest = viewModel::onDeleteRequest,
        onDeleteDismiss = viewModel::onDeleteDismiss,
        onDeleteConfirm = viewModel::onDeleteConfirm,
    )
}

@Composable
internal fun EditTripScreenContent(
    uiState: EditTripUiState,
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {},
    onTitleChange: (String) -> Unit = {},
    onDestinationChange: (String) -> Unit = {},
    onDateRangeChange: (LocalDate?, LocalDate?) -> Unit = { _, _ -> },
    onDescriptionChange: (String) -> Unit = {},
    onSaveClick: () -> Unit = {},
    onCompletionResetConfirm: () -> Unit = {},
    onCompletionResetDismiss: () -> Unit = {},
    onOutOfRangeConfirm: () -> Unit = {},
    onOutOfRangeDismiss: () -> Unit = {},
    onDeleteRequest: () -> Unit = {},
    onDeleteDismiss: () -> Unit = {},
    onDeleteConfirm: () -> Unit = {},
) {
    var isDatePickerVisible by rememberSaveable { mutableStateOf(false) }
    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing,
    ) { innerPadding ->
        Column(Modifier.fillMaxSize().padding(innerPadding)) {
            EditTripHeader(onBackClick)
            when {
                uiState.isLoading -> CenterContent { CircularProgressIndicator() }
                uiState.isNotFound -> CenterContent { Text("여행을 찾을 수 없습니다.") }
                uiState.isLoadFailed -> CenterContent {
                    Text("여행을 불러오지 못했습니다.", color = MaterialTheme.colorScheme.error)
                }
                else -> Column(
                    modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    OutlinedTextField(
                        value = uiState.title,
                        onValueChange = onTitleChange,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("여행 이름") },
                        supportingText = if (uiState.isTitleError) {
                            { Text("여행 이름을 입력해 주세요.") }
                        } else null,
                        isError = uiState.isTitleError,
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = uiState.destination,
                        onValueChange = onDestinationChange,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("여행 지역") },
                        singleLine = true,
                    )
                    Box(Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = formatDateRange(uiState.startDate, uiState.endDate),
                            onValueChange = {},
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("여행 날짜") },
                            supportingText = uiState.dateError?.let { error ->
                                {
                                    Text(
                                        if (error == EditTripDateError.MissingRange) {
                                            "여행 날짜를 선택해 주세요."
                                        } else {
                                            "종료일은 시작일보다 빠를 수 없습니다."
                                        }
                                    )
                                }
                            },
                            isError = uiState.dateError != null,
                            readOnly = true,
                            singleLine = true,
                        )
                        Box(Modifier.matchParentSize().clickable { isDatePickerVisible = true })
                    }
                    OutlinedTextField(
                        value = uiState.description,
                        onValueChange = onDescriptionChange,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("여행 메모") },
                        minLines = 3,
                        maxLines = 5,
                    )
                    if (uiState.isSaveFailed) {
                        Text("여행을 저장하지 못했습니다.", color = MaterialTheme.colorScheme.error)
                    }
                    Button(
                        onClick = onSaveClick,
                        enabled = !uiState.isSaving,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                    ) {
                        if (uiState.isSaving) CircularProgressIndicator() else Text("변경사항 저장")
                    }
                    OutlinedButton(
                        onClick = onDeleteRequest,
                        enabled = !uiState.isSaving,
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("여행 삭제") }
                }
            }
        }
    }

    if (isDatePickerVisible) {
        EditTripDateRangeDialog(
            initialStartDate = uiState.startDate,
            initialEndDate = uiState.endDate,
            onDismiss = { isDatePickerVisible = false },
            onConfirm = { start, end ->
                onDateRangeChange(start, end)
                isDatePickerVisible = false
            },
        )
    }
    uiState.outOfRangeItemCount?.let { count ->
        AlertDialog(
            onDismissRequest = onOutOfRangeDismiss,
            title = { Text("여행 기간 변경") },
            text = { Text("새 여행 기간을 벗어나는 일정 ${count}개가 삭제됩니다. 계속할까요?") },
            confirmButton = { TextButton(onClick = onOutOfRangeConfirm) { Text("삭제 후 변경") } },
            dismissButton = { TextButton(onClick = onOutOfRangeDismiss) { Text("취소") } },
        )
    }
    if (uiState.isCompletionResetConfirmationVisible) {
        AlertDialog(
            onDismissRequest = onCompletionResetDismiss,
            title = { Text("완료 상태 해제") },
            text = { Text("완료한 여행의 날짜를 현재 또는 미래로 변경하면 완료 상태가 해제됩니다. 계속할까요?") },
            confirmButton = { TextButton(onClick = onCompletionResetConfirm) { Text("해제 후 변경") } },
            dismissButton = { TextButton(onClick = onCompletionResetDismiss) { Text("취소") } },
        )
    }
    if (uiState.isDeleteConfirmationVisible) {
        AlertDialog(
            onDismissRequest = onDeleteDismiss,
            title = { Text("여행 삭제") },
            text = { Text("여행과 모든 일정을 삭제할까요? 삭제한 데이터는 복구할 수 없습니다.") },
            confirmButton = { TextButton(onClick = onDeleteConfirm) { Text("삭제") } },
            dismissButton = { TextButton(onClick = onDeleteDismiss) { Text("취소") } },
        )
    }
}

@Composable
private fun EditTripHeader(onBackClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBackClick) {
            Icon(
                ImageVector.vectorResource(R.drawable.ic_arrow_back_24),
                contentDescription = "뒤로가기",
            )
        }
        Text("여행 수정", style = MaterialTheme.typography.titleLarge)
    }
}

@Composable
private fun CenterContent(content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { content() }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditTripDateRangeDialog(
    initialStartDate: LocalDate?,
    initialEndDate: LocalDate?,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate?, LocalDate?) -> Unit,
) {
    val state = rememberDateRangePickerState(
        initialSelectedStartDateMillis = initialStartDate?.toUtcMillis(),
        initialSelectedEndDateMillis = initialEndDate?.toUtcMillis(),
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                onConfirm(
                    state.selectedStartDateMillis?.toUtcDate(),
                    state.selectedEndDateMillis?.toUtcDate(),
                )
            }) { Text("확인") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("취소") } },
    ) {
        DateRangePicker(state = state, title = { Text("여행 기간 선택") })
    }
}

private fun formatDateRange(start: LocalDate?, end: LocalDate?): String = when {
    start != null && end != null -> "${start.format(dateFormatter)} - ${end.format(dateFormatter)}"
    start != null -> "${start.format(dateFormatter)} -"
    else -> ""
}

private fun LocalDate.toUtcMillis(): Long = atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
private fun Long.toUtcDate(): LocalDate = Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()
private val dateFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd")

private val previewState = EditTripUiState(
    isLoading = false,
    tripPlanId = 7L,
    title = "제주 여행",
    destination = "제주",
    startDate = LocalDate.of(2026, 11, 9),
    endDate = LocalDate.of(2026, 11, 11),
    description = "친구와 여행",
)

@Preview(showBackground = true)
@Composable
private fun EditTripPreview() {
    MaterialTheme { EditTripScreenContent(uiState = previewState) }
}

@Preview(showBackground = true)
@Composable
private fun EditTripOutOfRangePreview() {
    MaterialTheme { EditTripScreenContent(uiState = previewState.copy(outOfRangeItemCount = 2)) }
}

@Preview(showBackground = true)
@Composable
private fun EditTripErrorPreview() {
    MaterialTheme {
        EditTripScreenContent(
            uiState = previewState.copy(isTitleError = true, isSaveFailed = true)
        )
    }
}
