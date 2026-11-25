package com.example.tripplan.presentation.planning

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.tripplan.R
import com.example.tripplan.presentation.lodging.LodgingDayInfo
import com.example.tripplan.presentation.lodging.LodgingEditor
import com.example.tripplan.presentation.lodging.LodgingSection
import com.example.tripplan.presentation.lodging.LodgingUiModel
import com.example.tripplan.presentation.lodging.LodgingViewModel
import java.time.LocalDate

@Composable
fun TripPlanningScreen(
    tripPlanId: Long,
    onSaveDraft: () -> Unit,
    onFinalized: (Long) -> Unit,
    onEditTripInfo: () -> Unit,
    onAddItinerary: (LocalDate) -> Unit,
    onEditItinerary: (Long) -> Unit,
    onMapClick: () -> Unit,
    viewModel: TripPlanningViewModel = hiltViewModel(key = "trip-planning-$tripPlanId"),
) {
    val uiState by viewModel.uiState.collectAsState()
    val lodgingViewModel: LodgingViewModel = hiltViewModel(key = "lodging-editor-$tripPlanId")
    val lodgingState by lodgingViewModel.uiState.collectAsState()
    var editingLodgingId by rememberSaveable { mutableStateOf<Long?>(null) }
    LaunchedEffect(lodgingViewModel, tripPlanId) { lodgingViewModel.load(tripPlanId, editable = true) }
    var showCandidates by rememberSaveable { mutableStateOf(false) }
    var showCandidateForm by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(viewModel, tripPlanId) { viewModel.load(tripPlanId) }
    LaunchedEffect(viewModel, onFinalized) {
        viewModel.events.collect { event ->
            when (event) {
                is TripPlanningEvent.Finalized -> onFinalized(event.tripPlanId)
                TripPlanningEvent.CandidateAdded -> showCandidateForm = false
                TripPlanningEvent.CandidateScheduled -> showCandidates = false
            }
        }
    }
    TripPlanningScreenContent(
        uiState = uiState,
        onSaveDraft = onSaveDraft,
        onFinalize = viewModel::finalizePlan,
        onEditTripInfo = onEditTripInfo,
        onDaySelected = viewModel::selectDay,
        onAddItinerary = onAddItinerary,
        onEditItinerary = onEditItinerary,
        onMoveItinerary = viewModel::moveItineraryToDay,
        onCandidatesClick = { showCandidates = true },
        lodgings = lodgingState.lodgings,
        onAddLodging = {
            lodgingViewModel.beginEdit()
            editingLodgingId = 0
        },
        onEditLodging = {
            lodgingViewModel.beginEdit()
            editingLodgingId = it.id
        },
        onMapClick = onMapClick,
    )
    editingLodgingId?.let { id ->
        val lodging = lodgingState.lodgings.firstOrNull { it.id == id }
        if (id == 0L || lodging != null) {
            LodgingEditor(
                existing = lodging,
                dates = uiState.days.map { it.date },
                state = lodgingState,
                onDismiss = { editingLodgingId = null },
                onSave = { input, resolvePlace ->
                    lodgingViewModel.save(input, resolvePlace) { editingLodgingId = null }
                },
                onSearch = { lodgingViewModel.search(uiState.countryCode, it) },
                onClearSearch = lodgingViewModel::clearSearch,
                onDelete = { lodgingViewModel.remove(it) { editingLodgingId = null } },
            )
        }
    }
    if (showCandidates && !showCandidateForm) {
        PlaceCandidateSheet(
            state = uiState.candidates,
            days = uiState.days,
            selectedDate = uiState.selectedDate,
            onDismiss = { showCandidates = false },
            onAdd = { showCandidateForm = true },
            onSchedule = viewModel::scheduleCandidate,
            onRemove = viewModel::removeCandidate,
        )
    }
    if (showCandidateForm) {
        PlaceCandidateForm(
            state = uiState.candidates,
            onDismiss = { showCandidateForm = false },
            onSearch = viewModel::searchCandidates,
            onSave = viewModel::addCandidate,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TripPlanningScreenContent(
    uiState: TripPlanningUiState,
    onSaveDraft: () -> Unit = {},
    onFinalize: () -> Unit = {},
    onEditTripInfo: () -> Unit = {},
    onDaySelected: (LocalDate) -> Unit = {},
    onAddItinerary: (LocalDate) -> Unit = {},
    onEditItinerary: (Long) -> Unit = {},
    onMapClick: () -> Unit = {},
    onMoveItinerary: (Long, LocalDate) -> Unit = { _, _ -> },
    onCandidatesClick: () -> Unit = {},
    lodgings: List<LodgingUiModel> = emptyList(),
    onAddLodging: () -> Unit = {},
    onEditLodging: (LodgingUiModel) -> Unit = {},
) {
    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = { Text(if (uiState.isDraft) "여행 계획 작성" else "여행 계획 수정") },
                navigationIcon = {
                    IconButton(onClick = onSaveDraft) {
                        Icon(
                            ImageVector.vectorResource(R.drawable.ic_arrow_back_24),
                            contentDescription = "뒤로",
                        )
                    }
                },
                actions = {
                    if (uiState.isDraft) {
                        TextButton(onClick = onSaveDraft, enabled = !uiState.isFinalizing && !uiState.isMovingItinerary) {
                            Text("임시저장")
                        }
                        TextButton(onClick = onFinalize, enabled = !uiState.isFinalizing && !uiState.isMovingItinerary) {
                            Text("계획 완료")
                        }
                    } else {
                        TextButton(onClick = onSaveDraft, enabled = !uiState.isMovingItinerary) { Text("수정 완료") }
                    }
                },
            )
        },
    ) { padding ->
        when {
            uiState.isLoading -> Center { CircularProgressIndicator() }
            uiState.isNotFound -> Center { Text("여행 계획을 찾을 수 없습니다.") }
            uiState.isLoadFailed -> Center { Text("여행 계획을 불러오지 못했습니다.") }
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item {
                    OutlinedCard(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(uiState.title, style = MaterialTheme.typography.titleLarge)
                                    Text(uiState.dateText, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    if (uiState.destination.isNotBlank()) Text(uiState.destination)
                                }
                                TextButton(onClick = onEditTripInfo) { Text("기본 정보 수정") }
                            }
                        }
                    }
                }
                if (uiState.days.size > 1 || lodgings.isNotEmpty()) {
                    item {
                        LodgingSection(
                            lodgings = lodgings,
                            onAdd = onAddLodging.takeIf { uiState.days.size > 1 },
                            onEdit = onEditLodging,
                        )
                    }
                }
                item {
                    OutlinedButton(onClick = onCandidatesClick, modifier = Modifier.fillMaxWidth()) {
                        Text("후보 장소 ${uiState.candidates.items.size}곳")
                    }
                }
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                if (lodgings.isNotEmpty()) {
                    item { LodgingDayInfo(lodgings, uiState.selectedDate) }
                }
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("일정", style = MaterialTheme.typography.titleMedium)
                        Row {
                            TextButton(onClick = onMapClick) { Text("지도") }
                            TextButton(
                                onClick = { uiState.selectedDate?.let(onAddItinerary) },
                                enabled = uiState.selectedDate != null,
                            ) { Text("일정 추가") }
                        }
                    }
                }
                if (uiState.items.isEmpty()) {
                    item {
                        Text(
                            "선택한 일차에 작성한 일정이 없습니다.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    val targetDays = uiState.days.filter { it.date != uiState.selectedDate }
                    items(uiState.items, key = { it.id }) { item ->
                        OutlinedCard(
                            onClick = { onEditItinerary(item.id) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                if (item.timeText.isNotBlank()) {
                                    Text(item.timeText, style = MaterialTheme.typography.labelLarge)
                                    Spacer(Modifier.padding(horizontal = 6.dp))
                                }
                                Column(Modifier.weight(1f)) {
                                    Text(item.title, style = MaterialTheme.typography.titleMedium)
                                    if (item.place.isNotBlank()) {
                                        Text(item.place, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                if (targetDays.isNotEmpty()) {
                                    MoveItineraryMenu(
                                        days = targetDays,
                                        enabled = !uiState.isMovingItinerary && !uiState.isFinalizing,
                                        onDaySelected = { onMoveItinerary(item.id, it) },
                                    )
                                }
                            }
                        }
                    }
                }
                if (uiState.isItineraryMoveFailed) {
                    item { Text("일정을 옮기지 못했습니다. 다시 시도해 주세요.", color = MaterialTheme.colorScheme.error) }
                }
                if (uiState.isMutationFailed) {
                    item { Text("계획 상태를 저장하지 못했습니다.", color = MaterialTheme.colorScheme.error) }
                }
            }
        }
    }
}

@Composable
private fun MoveItineraryMenu(
    days: List<PlanningDayUiModel>,
    enabled: Boolean,
    onDaySelected: (LocalDate) -> Unit,
) {
    var isExpanded by remember { mutableStateOf(false) }
    Box {
        TextButton(onClick = { isExpanded = true }, enabled = enabled) { Text("일차 이동") }
        DropdownMenu(expanded = isExpanded, onDismissRequest = { isExpanded = false }) {
            days.forEach { day ->
                DropdownMenuItem(
                    text = { Text("${day.dayNumber}일차 · ${day.dateText}") },
                    onClick = {
                        isExpanded = false
                        onDaySelected(day.date)
                    },
                )
            }
        }
    }
}

@Composable
private fun Center(content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { content() }
}

@Preview(showBackground = true, widthDp = 420, heightDp = 800)
@Composable
private fun TripPlanningPreview() {
    MaterialTheme {
        TripPlanningScreenContent(
            uiState = TripPlanningUiState(
                isDraft = true,
                title = "다낭 여행",
                destination = "베트남 다낭",
                dateText = "2026.11.20 - 2026.11.23",
                days = listOf(
                    PlanningDayUiModel(1, LocalDate.of(2026, 11, 20), "11.20"),
                    PlanningDayUiModel(2, LocalDate.of(2026, 11, 21), "11.21"),
                ),
                selectedDate = LocalDate.of(2026, 11, 20),
                items = listOf(PlanningItemUiModel(1, "한시장", "119 Trần Phú", "10:00")),
            )
        )
    }
}
