package com.example.tripplan.presentation.create

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.tripplan.R
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@Composable
fun CreateTripScreen(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {},
    onTripCreated: (Long) -> Unit = {},
    viewModel: CreateTripViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(viewModel, onTripCreated) {
        viewModel.events.collect { event ->
            when (event) {
                is CreateTripEvent.TripCreated -> onTripCreated(event.tripPlanId)
            }
        }
    }

    CreateTripScreenContent(
        uiState = uiState,
        modifier = modifier,
        onBackClick = onBackClick,
        onTripTitleChange = viewModel::onTripTitleChange,
        onTripPlaceChange = viewModel::onTripPlaceChange,
        onRegionClick = viewModel::onRegionClick,
        onRegionSearchDismiss = viewModel::onRegionSearchDismiss,
        onTripDateRangeChange = viewModel::onTripDateRangeChange,
        onDescriptionChange = viewModel::onDescriptionChange,
        onSaveClick = viewModel::onSaveClick,
    )
}

@Composable
internal fun CreateTripScreenContent(
    uiState: CreateTripUiState,
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {},
    onTripTitleChange: (String) -> Unit = {},
    onTripPlaceChange: (String) -> Unit = {},
    onRegionClick: (RegionSearchResultUiModel) -> Unit = {},
    onRegionSearchDismiss: () -> Unit = {},
    onTripDateRangeChange: (LocalDate?, LocalDate?) -> Unit = { _, _ -> },
    onDescriptionChange: (String) -> Unit = {},
    onSaveClick: () -> Unit = {},
) {
    var isDatePickerVisible by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            CreateTripHeader(onBackClick = onBackClick)
            TripInfoFields(
                uiState = uiState,
                onTripTitleChange = onTripTitleChange,
                onTripPlaceChange = onTripPlaceChange,
                onRegionClick = onRegionClick,
                onRegionSearchDismiss = onRegionSearchDismiss,
                onTripDateClick = { isDatePickerVisible = true },
                onDescriptionChange = onDescriptionChange,
            )
            if (uiState.isSaveFailed) {
                Text(
                    text = "여행을 저장하지 못했습니다. 다시 시도해 주세요.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Button(
                onClick = onSaveClick,
                enabled = !uiState.isSaving,
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
            ) {
                if (uiState.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    Text("여행 만들기")
                }
            }
        }
    }

    if (isDatePickerVisible) {
        TripDateRangePickerDialog(
            initialStartDate = uiState.startDate,
            initialEndDate = uiState.endDate,
            onDismissRequest = { isDatePickerVisible = false },
            onDateRangeSelected = { startDate, endDate ->
                onTripDateRangeChange(startDate, endDate)
                isDatePickerVisible = false
            },
        )
    }
}

@Composable
private fun CreateTripHeader(onBackClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        IconButton(onClick = onBackClick) {
            Icon(
                imageVector = ImageVector.vectorResource(R.drawable.ic_arrow_back_24),
                contentDescription = "뒤로가기",
            )
        }
        Text("새 여행 만들기", style = MaterialTheme.typography.titleLarge)
    }
}

@Composable
private fun TripInfoFields(
    uiState: CreateTripUiState,
    onTripTitleChange: (String) -> Unit,
    onTripPlaceChange: (String) -> Unit,
    onRegionClick: (RegionSearchResultUiModel) -> Unit,
    onRegionSearchDismiss: () -> Unit,
    onTripDateClick: () -> Unit,
    onDescriptionChange: (String) -> Unit,
) {
    var regionFieldSize by remember { mutableStateOf(IntSize.Zero) }
    val density = LocalDensity.current
    val regionPopupWidth = with(density) { regionFieldSize.width.toDp() }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        OutlinedTextField(
            value = uiState.tripTitle,
            onValueChange = onTripTitleChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("여행 이름") },
            supportingText = if (uiState.isTitleError) {
                { Text("여행 이름을 입력해 주세요.") }
            } else null,
            isError = uiState.isTitleError,
            singleLine = true,
        )
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = uiState.tripPlace,
                onValueChange = onTripPlaceChange,
                modifier = Modifier.fillMaxWidth().onSizeChanged { regionFieldSize = it },
                label = { Text("여행 지역") },
                singleLine = true,
            )
            if (uiState.isRegionSearchExpanded) {
                Popup(
                    alignment = Alignment.TopStart,
                    offset = IntOffset(0, regionFieldSize.height),
                    onDismissRequest = onRegionSearchDismiss,
                    properties = PopupProperties(focusable = false),
                ) {
                    RegionSearchResultsPopup(
                        modifier = Modifier.width(regionPopupWidth),
                        regions = uiState.regionSearchResults,
                        onRegionClick = onRegionClick,
                    )
                }
            }
        }
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = formatTripDateRange(uiState.startDate, uiState.endDate),
                onValueChange = {},
                modifier = Modifier.fillMaxWidth(),
                label = { Text("여행 날짜") },
                placeholder = { Text("시작일과 종료일 선택") },
                supportingText = uiState.dateError?.let { error ->
                    {
                        Text(
                            when (error) {
                                CreateTripDateError.MissingRange -> "여행 날짜를 선택해 주세요."
                                CreateTripDateError.InvalidRange -> "종료일은 시작일보다 빠를 수 없습니다."
                            }
                        )
                    }
                },
                isError = uiState.dateError != null,
                readOnly = true,
                singleLine = true,
            )
            Box(Modifier.matchParentSize().clickable(onClick = onTripDateClick))
        }
        OutlinedTextField(
            value = uiState.description,
            onValueChange = onDescriptionChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("여행 메모") },
            placeholder = { Text("여행에 대한 메모를 입력하세요.") },
            minLines = 3,
            maxLines = 5,
        )
    }
}

@Composable
private fun RegionSearchResultsPopup(
    regions: List<RegionSearchResultUiModel>,
    onRegionClick: (RegionSearchResultUiModel) -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedCard(
        modifier = modifier.padding(horizontal = 2.dp),
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            regions.forEach { region ->
                Surface(
                    modifier = Modifier.fillMaxWidth().clickable { onRegionClick(region) },
                    color = MaterialTheme.colorScheme.surface,
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        Text(region.displayName, style = MaterialTheme.typography.bodyLarge)
                        if (region.detailText.isNotBlank()) {
                            Text(
                                region.detailText,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TripDateRangePickerDialog(
    initialStartDate: LocalDate?,
    initialEndDate: LocalDate?,
    onDismissRequest: () -> Unit,
    onDateRangeSelected: (LocalDate?, LocalDate?) -> Unit,
) {
    val state = rememberDateRangePickerState(
        initialSelectedStartDateMillis = initialStartDate?.toPickerMillis(),
        initialSelectedEndDateMillis = initialEndDate?.toPickerMillis(),
    )
    DatePickerDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(
                onClick = {
                    onDateRangeSelected(
                        state.selectedStartDateMillis?.toUtcLocalDate(),
                        state.selectedEndDateMillis?.toUtcLocalDate(),
                    )
                }
            ) { Text("확인") }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismissRequest) { Text("취소") }
        },
    ) {
        DateRangePicker(
            state = state,
            title = { Text("여행 기간 선택", Modifier.padding(start = 24.dp, top = 16.dp)) },
            headline = {
                Text("시작일과 종료일을 선택하세요", Modifier.padding(start = 24.dp, bottom = 12.dp))
            },
        )
    }
}

private fun formatTripDateRange(startDate: LocalDate?, endDate: LocalDate?): String {
    if (startDate == null && endDate == null) return ""
    val start = startDate?.format(tripDateFormatter).orEmpty()
    val end = endDate?.format(tripDateFormatter).orEmpty()
    return when {
        start.isNotEmpty() && end.isNotEmpty() -> "$start - $end"
        start.isNotEmpty() -> "$start -"
        else -> "- $end"
    }
}

private fun LocalDate.toPickerMillis(): Long =
    atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

private fun Long.toUtcLocalDate(): LocalDate =
    Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()

private val tripDateFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd")

@Preview(showBackground = true)
@Composable
private fun CreateTripScreenEmptyPreview() {
    MaterialTheme { CreateTripScreenContent(uiState = CreateTripUiState()) }
}

@Preview(showBackground = true)
@Composable
private fun CreateTripScreenFilledPreview() {
    MaterialTheme {
        CreateTripScreenContent(
            uiState = CreateTripUiState(
                tripTitle = "제주 여름 여행",
                tripPlace = "제주특별자치도 제주시",
                startDate = LocalDate.of(2026, 7, 21),
                endDate = LocalDate.of(2026, 7, 25),
                description = "친구들과 함께하는 여행",
            )
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun CreateTripScreenErrorPreview() {
    MaterialTheme {
        CreateTripScreenContent(
            uiState = CreateTripUiState(
                isTitleError = true,
                dateError = CreateTripDateError.MissingRange,
                isSaveFailed = true,
            )
        )
    }
}
