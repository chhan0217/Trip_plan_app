package com.example.tripplan.presentation.create

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.tripplan.R
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateTripScreen(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {},
    viewModel: CreateTripViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    CreateTripScreenContent(
        uiState = uiState,
        modifier = modifier,
        onBackClick = onBackClick,
        onTripTitleChange = viewModel::onTripTitleChange,
        onTripPlaceChange = viewModel::onTripPlaceChange,
        onRegionClick = viewModel::onRegionClick,
        onRegionSearchDismiss = viewModel::onRegionSearchDismiss,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateTripScreenContent(
    uiState: CreateTripUiState,
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {},
    onTripTitleChange: (String) -> Unit = {},
    onTripPlaceChange: (String) -> Unit = {},
    onRegionClick: (RegionSearchResultUiModel) -> Unit = {},
    onRegionSearchDismiss: () -> Unit = {},
) {
    var selectedStartDateMillis by rememberSaveable { mutableStateOf<Long?>(null) }
    var selectedEndDateMillis by rememberSaveable { mutableStateOf<Long?>(null) }
    var isDatePickerVisible by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        CreateTripHeader(onBackClick = onBackClick)
        TripInfoFields(
            tripTitle = uiState.tripTitle,
            onTripTitleChange = onTripTitleChange,
            tripPlace = uiState.tripPlace,
            onTripPlaceChange = onTripPlaceChange,
            regionSearchResults = uiState.regionSearchResults,
            isRegionSearchExpanded = uiState.isRegionSearchExpanded,
            onRegionClick = onRegionClick,
            onRegionSearchDismiss = onRegionSearchDismiss,
            tripDateText = formatTripDateRange(
                startDateMillis = selectedStartDateMillis,
                endDateMillis = selectedEndDateMillis
            ),
            onTripDateClick = { isDatePickerVisible = true }
        )
        AddScheduleCard()
    }

    if (isDatePickerVisible) {
        TripDateRangePickerDialog(
            initialStartDateMillis = selectedStartDateMillis,
            initialEndDateMillis = selectedEndDateMillis,
            onDismissRequest = { isDatePickerVisible = false },
            onDateRangeSelected = { startDateMillis, endDateMillis ->
                selectedStartDateMillis = startDateMillis
                selectedEndDateMillis = endDateMillis
                isDatePickerVisible = false
            }
        )
    }
}

@Composable
private fun CreateTripHeader(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        IconButton(onClick = onBackClick) {
            Icon(
                imageVector = ImageVector.vectorResource(R.drawable.ic_arrow_back_24),
                contentDescription = "뒤로가기"
            )
        }
        Text(
            text = "여행 플랜 작성",
            style = MaterialTheme.typography.titleLarge
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TripInfoFields(
    tripTitle: String,
    onTripTitleChange: (String) -> Unit,
    tripPlace: String,
    onTripPlaceChange: (String) -> Unit,
    regionSearchResults: List<RegionSearchResultUiModel>,
    isRegionSearchExpanded: Boolean,
    onRegionClick: (RegionSearchResultUiModel) -> Unit,
    onRegionSearchDismiss: () -> Unit,
    tripDateText: String,
    onTripDateClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var regionFieldSize by remember { mutableStateOf(IntSize.Zero) }
    val density = LocalDensity.current
    val regionPopupWidth = with(density) { regionFieldSize.width.toDp() }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedTextField(
            value = tripTitle,
            onValueChange = onTripTitleChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("여행 이름") },
            singleLine = true
        )
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = tripPlace,
                onValueChange = onTripPlaceChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .onSizeChanged { regionFieldSize = it },
                label = { Text("여행 지역") },
                singleLine = true
            )
            if (isRegionSearchExpanded) {
                Popup(
                    alignment = Alignment.TopStart,
                    offset = IntOffset(0, regionFieldSize.height),
                    onDismissRequest = onRegionSearchDismiss,
                    properties = PopupProperties(focusable = false)
                ) {
                    RegionSearchResultsPopup(
                        modifier = if (regionFieldSize.width > 0) {
                            Modifier.width(regionPopupWidth)
                        } else {
                            Modifier.fillMaxWidth()
                        },
                        regions = regionSearchResults,
                        onRegionClick = onRegionClick
                    )
                }
            }
        }
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = tripDateText,
                onValueChange = {},
                modifier = Modifier.fillMaxWidth(),
                label = { Text("여행 날짜") },
                placeholder = { Text("시작일과 종료일 선택") },
                readOnly = true,
                singleLine = true
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clickable(onClick = onTripDateClick)
            )
        }
    }
}

@Composable
private fun RegionSearchResultsPopup(
    regions: List<RegionSearchResultUiModel>,
    onRegionClick: (RegionSearchResultUiModel) -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedCard(
        modifier = modifier
            .padding(horizontal = 2.dp),
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant
        ),
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            regions.forEach { region ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onRegionClick(region) },
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(
                            text = region.displayName,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        if (region.detailText.isNotBlank()) {
                            Text(
                                text = region.detailText,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
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
    initialStartDateMillis: Long?,
    initialEndDateMillis: Long?,
    onDismissRequest: () -> Unit,
    onDateRangeSelected: (Long?, Long?) -> Unit,
) {
    val dateRangePickerState = rememberDateRangePickerState(
        initialSelectedStartDateMillis = initialStartDateMillis,
        initialSelectedEndDateMillis = initialEndDateMillis
    )

    DatePickerDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(
                onClick = {
                    onDateRangeSelected(
                        dateRangePickerState.selectedStartDateMillis,
                        dateRangePickerState.selectedEndDateMillis
                    )
                }
            ) {
                Text("확인")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismissRequest) {
                Text("취소")
            }
        }
    ) {
        DateRangePicker(
            state = dateRangePickerState,
            title = {
                Text(
                    modifier = Modifier.padding(start = 24.dp, end = 12.dp, top = 16.dp),
                    text = "여행 기간 선택"
                )
            },
            headline = {
                Text(
                    modifier = Modifier.padding(start = 24.dp, end = 12.dp, bottom = 12.dp),
                    text = "시작일과 종료일을 선택하세요"
                )
            }
        )
    }
}

@Composable
private fun AddScheduleCard(modifier: Modifier = Modifier) {
    OutlinedCard(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant
        ),
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(MaterialTheme.shapes.small)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.ic_add_24),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "일정 추가",
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = "날짜별 장소와 메모를 추가합니다.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CreateTripScreenPreview() {
    MaterialTheme {
        CreateTripScreenContent(
            uiState = CreateTripUiState(
                tripTitle = "여름 휴가",
                tripPlace = "제주",
                regionSearchResults = listOf(
                    RegionSearchResultUiModel(
                        id = "jeju",
                        displayName = "제주특별자치도 제주시 애월읍",
                        detailText = ""
                    ),
                    RegionSearchResultUiModel(
                        id = "seogwipo",
                        displayName = "제주특별자치도 서귀포시",
                        detailText = ""
                    )
                ),
                isRegionSearchExpanded = true
            )
        )
    }
}

private fun formatTripDateRange(
    startDateMillis: Long?,
    endDateMillis: Long?,
): String {
    if (startDateMillis == null && endDateMillis == null) return ""

    val startDate = startDateMillis?.toLocalDateText().orEmpty()
    val endDate = endDateMillis?.toLocalDateText().orEmpty()

    return when {
        startDate.isNotEmpty() && endDate.isNotEmpty() -> "$startDate - $endDate"
        startDate.isNotEmpty() -> "$startDate -"
        else -> "- $endDate"
    }
}

private fun Long.toLocalDateText(): String {
    val date = Instant.ofEpochMilli(this)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
    return date.format(tripDateFormatter)
}

private val tripDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd")
