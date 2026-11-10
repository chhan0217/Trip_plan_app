package com.example.tripplan.presentation.detail

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
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

@Composable
fun TripDetailScreen(
    tripPlanId: Long,
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {},
    onEditTrip: () -> Unit = {},
    onAddItinerary: (LocalDate) -> Unit = {},
    onEditItinerary: (Long) -> Unit = {},
    onExportTrip: () -> Unit = {},
    onPackingClick: () -> Unit = {},
    onCopyTrip: () -> Unit = {},
    onPlacesClick: () -> Unit = {},
    viewModel: TripDetailViewModel = hiltViewModel(key = "trip-detail-$tripPlanId"),
) {
    val uiState by viewModel.uiState.collectAsState()
    LaunchedEffect(viewModel, tripPlanId) { viewModel.load(tripPlanId) }

    TripDetailScreenContent(
        uiState = uiState,
        modifier = modifier,
        onBackClick = onBackClick,
        onEditTrip = onEditTrip,
        onDaySelected = viewModel::onDaySelected,
        onAddItinerary = onAddItinerary,
        onEditItinerary = onEditItinerary,
        onExportTrip = onExportTrip,
        onPackingClick = onPackingClick,
        onCopyTrip = onCopyTrip,
        onPlacesClick = onPlacesClick,
        onMoveUp = viewModel::onMoveUp,
        onMoveDown = viewModel::onMoveDown,
        onCompletionRequest = viewModel::onCompletionRequest,
        onCompletionConfirm = viewModel::onCompletionConfirm,
        onCompletionDismiss = viewModel::onCompletionDismiss,
    )
}

@Composable
internal fun TripDetailScreenContent(
    uiState: TripDetailUiState,
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {},
    onEditTrip: () -> Unit = {},
    onDaySelected: (LocalDate) -> Unit = {},
    onAddItinerary: (LocalDate) -> Unit = {},
    onEditItinerary: (Long) -> Unit = {},
    onExportTrip: () -> Unit = {},
    onPackingClick: () -> Unit = {},
    onCopyTrip: () -> Unit = {},
    onPlacesClick: () -> Unit = {},
    onMoveUp: (Long) -> Unit = {},
    onMoveDown: (Long) -> Unit = {},
    onCompletionRequest: () -> Unit = {},
    onCompletionConfirm: () -> Unit = {},
    onCompletionDismiss: () -> Unit = {},
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing,
    ) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
        ) {
            DetailHeader(
                onBackClick = onBackClick,
                onEditTrip = onEditTrip,
                onExportTrip = onExportTrip,
            )
            when {
                uiState.isLoading -> CenterMessage { CircularProgressIndicator() }
                uiState.isNotFound -> CenterMessage { Text("여행을 찾을 수 없습니다.") }
                uiState.isLoadFailed -> CenterMessage {
                    Text(
                        "여행을 불러오지 못했습니다. 다시 시도해 주세요.",
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                else -> DetailContent(
                    uiState = uiState,
                    onDaySelected = onDaySelected,
                    onAddItinerary = onAddItinerary,
                    onEditItinerary = onEditItinerary,
                    onMoveUp = onMoveUp,
                    onMoveDown = onMoveDown,
                    onCompletionRequest = onCompletionRequest,
                    onPackingClick = onPackingClick,
                    onCopyTrip = onCopyTrip,
                    onPlacesClick = onPlacesClick,
                )
            }
        }
    }
    if (uiState.isCompletionConfirmationVisible) {
        AlertDialog(
            onDismissRequest = onCompletionDismiss,
            title = { Text(if (uiState.isCompleted) "완료 취소" else "여행 완료") },
            text = {
                Text(if (uiState.isCompleted) "이 여행의 완료 상태를 취소할까요?" else "이 여행을 완료 처리할까요?")
            },
            confirmButton = { TextButton(onClick = onCompletionConfirm) { Text("확인") } },
            dismissButton = { TextButton(onClick = onCompletionDismiss) { Text("취소") } },
        )
    }
}

@Composable
private fun DetailHeader(
    onBackClick: () -> Unit,
    onEditTrip: () -> Unit,
    onExportTrip: () -> Unit,
) {
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
        Text("여행 상세", style = MaterialTheme.typography.titleLarge)
        Box(Modifier.weight(1f))
        TextButton(onClick = onExportTrip) { Text("파일 공유") }
        TextButton(onClick = onEditTrip) { Text("여행 수정") }
    }
}

@Composable
private fun CenterMessage(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        contentAlignment = Alignment.Center,
    ) { content() }
}

@Composable
private fun DetailContent(
    uiState: TripDetailUiState,
    onDaySelected: (LocalDate) -> Unit,
    onAddItinerary: (LocalDate) -> Unit,
    onEditItinerary: (Long) -> Unit,
    onMoveUp: (Long) -> Unit,
    onMoveDown: (Long) -> Unit,
    onCompletionRequest: () -> Unit,
    onPackingClick: () -> Unit,
    onCopyTrip: () -> Unit,
    onPlacesClick: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(uiState.title, style = MaterialTheme.typography.headlineSmall)
                if (uiState.statusText.isNotBlank()) {
                    Text(
                        uiState.statusText,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Text(
                    uiState.dateText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (uiState.destination.isNotBlank()) {
                    Text(uiState.destination, style = MaterialTheme.typography.bodyLarge)
                }
                if (uiState.description.isNotBlank()) {
                    Text(
                        uiState.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                TextButton(
                    onClick = onCompletionRequest,
                    enabled = !uiState.isCompletionSaving,
                ) { Text(if (uiState.isCompleted) "완료 취소" else "여행 완료") }
                Row {
                    TextButton(onClick = onPackingClick) { Text("준비물") }
                    TextButton(onClick = onCopyTrip) { Text("여행 복사") }
                    TextButton(onClick = onPlacesClick) { Text("장소") }
                }
            }
        }
        item {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp),
            ) {
                items(uiState.days, key = { it.date.toString() }) { day ->
                    FilterChip(
                        selected = day.date == uiState.selectedDate,
                        onClick = { onDaySelected(day.date) },
                        label = {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("${day.dayNumber}일차")
                                Text(day.dateText, style = MaterialTheme.typography.labelSmall)
                            }
                        },
                    )
                }
            }
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(
                    onClick = { uiState.selectedDate?.let(onAddItinerary) },
                    enabled = uiState.selectedDate != null,
                ) { Text("일정 추가") }
            }
        }
        if (uiState.isMutationFailed) {
            item {
                Text(
                    "일정 순서를 변경하지 못했습니다.",
                    modifier = Modifier.padding(horizontal = 20.dp),
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
        if (uiState.itineraryItems.isEmpty()) {
            item {
                Text(
                    text = "이 날짜에는 등록된 일정이 없습니다.",
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            items(uiState.itineraryItems, key = { it.id }) { item ->
                ItineraryCard(
                    item = item,
                    modifier = Modifier.padding(horizontal = 20.dp),
                    onClick = { onEditItinerary(item.id) },
                    onMoveUp = { onMoveUp(item.id) },
                    onMoveDown = { onMoveDown(item.id) },
                )
            }
        }
        item { Box(Modifier.padding(bottom = 12.dp)) }
    }
}

@Composable
private fun ItineraryCard(
    item: ItineraryItemUiModel,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
) {
    OutlinedCard(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            if (item.timeText.isNotBlank()) {
                Text(
                    item.timeText,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Text(item.title, style = MaterialTheme.typography.titleMedium)
            if (item.place.isNotBlank()) {
                Text(item.place, style = MaterialTheme.typography.bodyMedium)
            }
            if (item.note.isNotBlank()) {
                Text(
                    item.note,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = onMoveUp, enabled = item.canMoveUp) { Text("위로") }
                TextButton(onClick = onMoveDown, enabled = item.canMoveDown) { Text("아래로") }
            }
        }
    }
}

private val previewDays = listOf(
    TripDayUiModel(1, LocalDate.of(2026, 11, 9), "11.09"),
    TripDayUiModel(2, LocalDate.of(2026, 11, 10), "11.10"),
)

private val previewState = TripDetailUiState(
    isLoading = false,
    title = "제주 여행",
    destination = "제주특별자치도 제주시",
    dateText = "2026.11.09 - 2026.11.10",
    description = "친구와 함께하는 여행",
    days = previewDays,
    selectedDate = previewDays.first().date,
    itineraryItems = listOf(
        ItineraryItemUiModel(
            1L,
            "성산일출봉",
            "서귀포시",
            "09:00 - 11:00",
            "입장권 확인",
            canMoveUp = false,
            canMoveDown = false,
        ),
    ),
)

@Preview(showBackground = true)
@Composable
private fun TripDetailDataPreview() {
    MaterialTheme { TripDetailScreenContent(uiState = previewState) }
}

@Preview(showBackground = true)
@Composable
private fun TripDetailEmptyPreview() {
    MaterialTheme {
        TripDetailScreenContent(uiState = previewState.copy(itineraryItems = emptyList()))
    }
}

@Preview(showBackground = true)
@Composable
private fun TripDetailErrorPreview() {
    MaterialTheme {
        TripDetailScreenContent(
            uiState = TripDetailUiState(isLoading = false, isLoadFailed = true)
        )
    }
}
