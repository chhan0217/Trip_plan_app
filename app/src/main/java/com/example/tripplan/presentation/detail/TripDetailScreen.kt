package com.example.tripplan.presentation.detail

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.testTag
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.tripplan.R
import com.example.tripplan.presentation.itineraryphoto.ItineraryPhotoAddDialog
import com.example.tripplan.presentation.itineraryphoto.ItineraryPhotoStrip
import com.example.tripplan.presentation.itineraryphoto.ItineraryPhotoViewer
import com.example.tripplan.presentation.itineraryphoto.ItineraryPhotoViewModel
import com.example.tripplan.presentation.lodging.LodgingDayInfo
import com.example.tripplan.presentation.lodging.LodgingSection
import com.example.tripplan.presentation.lodging.LodgingUiModel
import com.example.tripplan.presentation.lodging.LodgingViewModel
import com.example.tripplan.domain.rule.BudgetCalculator
import com.example.tripplan.domain.model.ExpenseCategory
import com.example.tripplan.domain.model.TripMoneyDisplayCurrency
import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.roundToInt

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
    onItineraryMapClick: (Boolean) -> Unit = {},
    viewModel: TripDetailViewModel = hiltViewModel(key = "trip-detail-$tripPlanId"),
) {
    val uiState by viewModel.uiState.collectAsState()
    val photoViewModel: ItineraryPhotoViewModel = hiltViewModel(key = "itinerary-photos-$tripPlanId")
    val photoState by photoViewModel.uiState.collectAsState()
    var photoAddItineraryId by rememberSaveable { mutableStateOf<Long?>(null) }
    var galleryItineraryId by rememberSaveable { mutableStateOf<Long?>(null) }
    var photoStartIndex by rememberSaveable { mutableStateOf(0) }
    LaunchedEffect(photoViewModel, tripPlanId) { photoViewModel.load(tripPlanId) }
    val lodgingViewModel: LodgingViewModel = hiltViewModel(key = "lodging-viewer-$tripPlanId")
    val lodgingState by lodgingViewModel.uiState.collectAsState()
    LaunchedEffect(lodgingViewModel, tripPlanId) { lodgingViewModel.load(tripPlanId, editable = false) }
    LaunchedEffect(viewModel, tripPlanId) { viewModel.load(tripPlanId) }

    TravelViewerTheme {
        TripDetailScreenContent(
            uiState = uiState,
            lodgings = lodgingState.lodgings,
            photoIdsByItinerary = photoState.photoIdsByItinerary,
            readPhoto = photoViewModel::readPhoto,
            onAddPhotos = { itineraryId ->
                photoViewModel.clearError()
                photoAddItineraryId = itineraryId
            },
            onViewPhotos = { itineraryId, index ->
                photoStartIndex = index
                galleryItineraryId = itineraryId
            },
            modifier = modifier,
            onBackClick = onBackClick,
            onEditTrip = onEditTrip,
            onDaySelected = viewModel::onDaySelected,
            onAddItinerary = onAddItinerary,
            onEditItinerary = onEditItinerary,
            onExportTrip = onExportTrip,
            onPackingClick = onPackingClick,
            onItineraryMapClick = onItineraryMapClick,
            onMoveUp = viewModel::onMoveUp,
            onMoveDown = viewModel::onMoveDown,
            onCompletionRequest = viewModel::onCompletionRequest,
            onCompletionConfirm = viewModel::onCompletionConfirm,
            onCompletionDismiss = viewModel::onCompletionDismiss,
            onCreateBudget = viewModel::createBudget,
            onAddBudget = viewModel::addBudget,
            onSaveEstimatedCost = viewModel::saveItineraryEstimatedCost,
            onExpenseRequest = viewModel::onExpenseRequest,
            onItineraryExpenseRequest = viewModel::onItineraryExpenseRequest,
            onExpenseDismiss = viewModel::onExpenseDismiss,
            onRecordExpense = viewModel::recordExpense,
            onExpenseEditRequest = viewModel::onExpenseEditRequest,
            onExpenseEditDismiss = viewModel::onExpenseEditDismiss,
            onUpdateExpense = viewModel::updateExpense,
            onExpenseDeleteRequest = viewModel::onExpenseDeleteRequest,
            onExpenseDeleteDismiss = viewModel::onExpenseDeleteDismiss,
            onDeleteExpense = viewModel::deleteExpense,
            onMoneyDisplayCurrencyChange = viewModel::setMoneyDisplayCurrency,
        )
        ItineraryPhotoAddDialog(
            itineraryId = photoAddItineraryId,
            canAdd = uiState.canAddItineraryPhotos,
            state = photoState,
            viewModel = photoViewModel,
            onDismiss = { photoAddItineraryId = null },
        )
        galleryItineraryId?.let { itineraryId ->
            ItineraryPhotoViewer(
                photoIds = photoState.photoIdsByItinerary[itineraryId].orEmpty(),
                initialIndex = photoStartIndex,
                readPhoto = photoViewModel::readPhoto,
                onDismiss = { galleryItineraryId = null },
            )
        }
    }
}

@Composable
private fun TravelViewerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MaterialTheme.colorScheme.copy(
            primary = Color(0xFF12635C),
            onPrimary = Color.White,
            primaryContainer = Color(0xFFDDEEE9),
            onPrimaryContainer = Color(0xFF17443E),
            background = Color(0xFFFAF7F0),
            onBackground = Color(0xFF202C29),
            surface = Color(0xFFFFFDF8),
            onSurface = Color(0xFF202C29),
            surfaceVariant = Color(0xFFE9E9E0),
            onSurfaceVariant = Color(0xFF465650),
            outline = Color(0xFF75837B),
            outlineVariant = Color(0xFFD3DBD2),
            secondaryContainer = Color(0xFFDDEEE9),
            onSecondaryContainer = Color(0xFF17443E),
        ),
        content = content,
    )
}

@Composable
internal fun TripDetailScreenContent(
    uiState: TripDetailUiState,
    modifier: Modifier = Modifier,
    lodgings: List<LodgingUiModel> = emptyList(),
    photoIdsByItinerary: Map<Long, List<Long>> = emptyMap(),
    readPhoto: suspend (Long) -> ByteArray? = { null },
    onAddPhotos: (Long) -> Unit = {},
    onViewPhotos: (Long, Int) -> Unit = { _, _ -> },
    onBackClick: () -> Unit = {},
    onEditTrip: () -> Unit = {},
    onDaySelected: (LocalDate) -> Unit = {},
    onAddItinerary: (LocalDate) -> Unit = {},
    onEditItinerary: (Long) -> Unit = {},
    onExportTrip: () -> Unit = {},
    onPackingClick: () -> Unit = {},
    onItineraryMapClick: (Boolean) -> Unit = {},
    onMoveUp: (Long) -> Unit = {},
    onMoveDown: (Long) -> Unit = {},
    onCompletionRequest: () -> Unit = {},
    onCompletionConfirm: () -> Unit = {},
    onCompletionDismiss: () -> Unit = {},
    onCreateBudget: (String) -> Unit = {},
    onAddBudget: (String) -> Unit = {},
    onSaveEstimatedCost: (Long, String, String, String) -> Unit = { _, _, _, _ -> },
    onExpenseRequest: () -> Unit = {},
    onItineraryExpenseRequest: (Long) -> Unit = {},
    onExpenseDismiss: () -> Unit = {},
    onRecordExpense: (String, ExpenseCategory, String, String) -> Unit = { _, _, _, _ -> },
    onExpenseEditRequest: (Long) -> Unit = {},
    onExpenseEditDismiss: () -> Unit = {},
    onUpdateExpense: (String, ExpenseCategory, String, String, String) -> Unit = { _, _, _, _, _ -> },
    onExpenseDeleteRequest: () -> Unit = {},
    onExpenseDeleteDismiss: () -> Unit = {},
    onDeleteExpense: () -> Unit = {},
    onMoneyDisplayCurrencyChange: (TripMoneyDisplayCurrency) -> Unit = {},
) {
    val isReadOnly = uiState.isTripEnded || uiState.isCompleted
    var isExpenseSummaryVisible by rememberSaveable { mutableStateOf(false) }
    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing,
    ) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
        ) {
            DetailHeader(
                showActions = !isReadOnly && !uiState.isLoading && !uiState.isNotFound && !uiState.isLoadFailed,
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
                    isReadOnly = isReadOnly,
                    lodgings = lodgings,
                    photoIdsByItinerary = photoIdsByItinerary,
                    readPhoto = readPhoto,
                    onAddPhotos = onAddPhotos,
                    onViewPhotos = onViewPhotos,
                    onDaySelected = onDaySelected,
                    onAddItinerary = onAddItinerary,
                    onEditItinerary = onEditItinerary,
                    onMoveUp = onMoveUp,
                    onMoveDown = onMoveDown,
                    onCompletionRequest = onCompletionRequest,
                    onPackingClick = onPackingClick,
                    onItineraryMapClick = onItineraryMapClick,
                    onCreateBudget = onCreateBudget,
                    onAddBudget = onAddBudget,
                    onSaveEstimatedCost = onSaveEstimatedCost,
                    onExpenseRequest = onExpenseRequest,
                    onItineraryExpenseRequest = onItineraryExpenseRequest,
                    onExpenseEditRequest = onExpenseEditRequest,
                    onMoneyDisplayCurrencyChange = onMoneyDisplayCurrencyChange,
                    onExpenseSummaryRequest = { isExpenseSummaryVisible = true },
                )
            }
        }
    }
    if (!isReadOnly && uiState.isCompletionConfirmationVisible) {
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
    if (!isReadOnly && uiState.isExpenseDialogVisible) {
        ExpenseInputDialog(
            currencyCode = uiState.tripCurrencyCode,
            occurredAtText = uiState.expenseOccurredAtText,
            targetText = uiState.expenseTargetText,
            inputRate = uiState.budgetInputRate,
            onDismiss = onExpenseDismiss,
            onSave = onRecordExpense,
        )
    }
    uiState.editingExpense?.takeUnless { isReadOnly }?.let { editor ->
        ExpenseEditDialog(
            editor = editor,
            tripCurrencyCode = uiState.tripCurrencyCode,
            errorMessage = uiState.mutationMessage,
            onDismiss = onExpenseEditDismiss,
            onSave = onUpdateExpense,
            onDeleteRequest = onExpenseDeleteRequest,
        )
    }
    if (!isReadOnly && uiState.isExpenseDeleteConfirmationVisible) {
        AlertDialog(
            onDismissRequest = onExpenseDeleteDismiss,
            title = { Text("지출 삭제") },
            text = { Text("이 지출 기록을 삭제할까요?") },
            confirmButton = { TextButton(onClick = onDeleteExpense) { Text("삭제") } },
            dismissButton = { TextButton(onClick = onExpenseDeleteDismiss) { Text("취소") } },
        )
    }
    if (isExpenseSummaryVisible) {
        ExpenseSummaryDialog(
            summary = uiState.expenseSummary,
            displayCurrency = uiState.moneyDisplayCurrency,
            isTripEnded = isReadOnly,
            onDismiss = { isExpenseSummaryVisible = false },
        )
    }
}

private val TripDetailUiState.canAddItineraryPhotos: Boolean
    get() = isTripOngoing && !isTripEnded && !isCompleted && !isDraft

@Composable
private fun DetailHeader(
    showActions: Boolean,
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
        Text("여행 보기", style = MaterialTheme.typography.titleLarge)
        Box(Modifier.weight(1f))
        if (showActions) {
            TextButton(onClick = onExportTrip) { Text("파일 공유") }
            TextButton(onClick = onEditTrip) { Text("계획 수정") }
        }
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
    isReadOnly: Boolean,
    lodgings: List<LodgingUiModel>,
    photoIdsByItinerary: Map<Long, List<Long>>,
    readPhoto: suspend (Long) -> ByteArray?,
    onAddPhotos: (Long) -> Unit,
    onViewPhotos: (Long, Int) -> Unit,
    onDaySelected: (LocalDate) -> Unit,
    onAddItinerary: (LocalDate) -> Unit,
    onEditItinerary: (Long) -> Unit,
    onMoveUp: (Long) -> Unit,
    onMoveDown: (Long) -> Unit,
    onCompletionRequest: () -> Unit,
    onPackingClick: () -> Unit,
    onItineraryMapClick: (Boolean) -> Unit,
    onCreateBudget: (String) -> Unit,
    onAddBudget: (String) -> Unit,
    onSaveEstimatedCost: (Long, String, String, String) -> Unit,
    onExpenseRequest: () -> Unit,
    onItineraryExpenseRequest: (Long) -> Unit,
    onExpenseEditRequest: (Long) -> Unit,
    onMoneyDisplayCurrencyChange: (TripMoneyDisplayCurrency) -> Unit,
    onExpenseSummaryRequest: () -> Unit,
) {
    val timelineRows = buildList {
        uiState.itineraryItems.forEach { add(TimelineRow.Schedule(it)) }
        uiState.standaloneExpenses.forEach { add(TimelineRow.ExpenseItem(it)) }
    }.sortedWith(compareBy<TimelineRow> { it.sortMinutes }.thenBy { it.stableOrder })
    LazyColumn(
        modifier = Modifier.fillMaxSize().testTag("trip_detail_list"),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            TripOverview(
                uiState = uiState,
                canManage = !isReadOnly && !uiState.isDraft,
                onCompletionRequest = onCompletionRequest,
                onPackingClick = onPackingClick,
            )
        }
        uiState.mutationMessage?.let { message ->
            item {
                Text(
                    message,
                    modifier = Modifier.padding(horizontal = 20.dp),
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
        item {
            val selectedIndex = uiState.days.indexOfFirst { it.date == uiState.selectedDate }
            LazyRow(
                state = rememberLazyListState(initialFirstVisibleItemIndex = (selectedIndex - 1).coerceAtLeast(0)),
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
        if (lodgings.isNotEmpty()) {
            item { LodgingDayInfo(lodgings, uiState.selectedDate, Modifier.padding(horizontal = 20.dp)) }
        }
        item {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("일정", Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
                    TextButton(onClick = { onItineraryMapClick(isReadOnly) }) { Text("일정 지도에서 보기") }
                }
                if (!uiState.isDraft) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("1일 사용금액", style = MaterialTheme.typography.labelMedium)
                        Text(
                            if (uiState.moneyDisplayCurrency == TripMoneyDisplayCurrency.KRW) {
                                uiState.selectedDayActualKrwText
                            } else {
                                uiState.selectedDayActualLocalText
                            },
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
                if (!isReadOnly && !uiState.isDraft) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                    ) {
                        TextButton(onClick = onExpenseRequest) { Text("지출 추가") }
                        uiState.selectedDate?.let { date ->
                            OutlinedButton(onClick = { onAddItinerary(date) }) { Text("일정 추가") }
                        }
                    }
                }
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
        if (timelineRows.isEmpty()) {
            item {
                Text(
                    text = "이 날짜에는 등록된 일정이 없습니다.",
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            itemsIndexed(timelineRows, key = { _, row -> row.key }) { index, row ->
                val time = when (row) {
                    is TimelineRow.Schedule -> row.item.timeText
                    is TimelineRow.ExpenseItem -> row.expense.timeText
                }
                TripTimelineItem(time = time, hasPrevious = index > 0, hasNext = index < timelineRows.lastIndex) {
                    when (row) {
                        is TimelineRow.Schedule -> ItineraryCard(
                            item = row.item,
                            isReadOnly = isReadOnly,
                            displayCurrency = uiState.moneyDisplayCurrency,
                            onAddExpense = { onItineraryExpenseRequest(row.item.id) },
                            canAddPhotos = uiState.canAddItineraryPhotos,
                            photoIds = photoIdsByItinerary[row.item.id].orEmpty(),
                            readPhoto = readPhoto,
                            onAddPhotos = { onAddPhotos(row.item.id) },
                            onViewPhotos = { photoIndex -> onViewPhotos(row.item.id, photoIndex) },
                            onEditExpense = onExpenseEditRequest,
                        )
                        is TimelineRow.ExpenseItem -> StandaloneExpenseCard(
                            expense = row.expense,
                            isReadOnly = isReadOnly,
                            onClick = { onExpenseEditRequest(row.expense.id) },
                        )
                    }
                }
            }
        }
        if (lodgings.isNotEmpty()) {
            item { LodgingSection(lodgings, Modifier.padding(horizontal = 20.dp)) }
        }
        if (!uiState.isDraft) {
            item {
                BudgetOverviewCard(
                    budget = uiState.budget,
                    isReadOnly = isReadOnly,
                    tripCurrencyCode = uiState.tripCurrencyCode,
                    inputRate = uiState.budgetInputRate,
                    displayCurrency = uiState.moneyDisplayCurrency,
                    modifier = Modifier.padding(horizontal = 20.dp),
                    onCreateBudget = onCreateBudget,
                    onAddBudget = onAddBudget,
                    onDisplayCurrencyChange = onMoneyDisplayCurrencyChange,
                )
            }
            item {
                OutlinedButton(
                    onClick = onExpenseSummaryRequest,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                ) { Text("지출 현황 보기") }
            }
        }
        item { Box(Modifier.padding(bottom = 12.dp)) }
    }
}

@Composable
private fun TripOverview(
    uiState: TripDetailUiState,
    canManage: Boolean,
    onCompletionRequest: () -> Unit,
    onPackingClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            if (uiState.statusText.isNotBlank()) {
                Text(uiState.statusText, style = MaterialTheme.typography.labelLarge)
            }
            Text(uiState.title, style = MaterialTheme.typography.headlineSmall)
            Text(uiState.dateText, style = MaterialTheme.typography.bodyMedium)
            if (uiState.destination.isNotBlank()) Text(uiState.destination, style = MaterialTheme.typography.bodyMedium)
            if (uiState.description.isNotBlank()) Text(uiState.description, style = MaterialTheme.typography.bodySmall)
            if (canManage) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onPackingClick) { Text("준비물") }
                    TextButton(onClick = onCompletionRequest, enabled = !uiState.isCompletionSaving) { Text("여행 완료") }
                }
            }
        }
    }
}

@Composable
private fun TripTimelineItem(
    time: String,
    hasPrevious: Boolean,
    hasNext: Boolean,
    content: @Composable () -> Unit,
) {
    val lineColor = MaterialTheme.colorScheme.outlineVariant
    val dotColor = MaterialTheme.colorScheme.primary
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).drawBehind {
            val x = 59.dp.toPx()
            val y = 25.dp.toPx()
            if (hasPrevious) drawLine(lineColor, Offset(x, -8.dp.toPx()), Offset(x, y), strokeWidth = 1.dp.toPx())
            if (hasNext) drawLine(lineColor, Offset(x, y), Offset(x, size.height + 8.dp.toPx()), strokeWidth = 1.dp.toPx())
            drawCircle(dotColor, radius = 4.dp.toPx(), center = Offset(x, y))
        },
    ) {
        Column(Modifier.width(51.dp).padding(top = 16.dp, end = 3.dp)) {
            val mainTime = time.substringBefore(" - ")
            val endTime = time.substringAfter(" - ", missingDelimiterValue = "")
            Text(
                mainTime.ifBlank { "미정" },
                color = if (mainTime.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant else dotColor,
                style = MaterialTheme.typography.labelMedium,
            )
            if (endTime.isNotBlank()) {
                Text(endTime, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
            }
        }
        Spacer(Modifier.width(17.dp))
        Box(Modifier.weight(1f)) { content() }
    }
}

@Composable
private fun ExpenseSummaryDialog(
    summary: ExpenseSummaryUiModel,
    displayCurrency: TripMoneyDisplayCurrency,
    isTripEnded: Boolean,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        Surface(
            modifier = Modifier.fillMaxSize().testTag("expense_summary_dialog"),
            color = MaterialTheme.colorScheme.surface,
        ) {
            Scaffold(contentWindowInsets = WindowInsets.safeDrawing) { innerPadding ->
                Column(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = if (isTripEnded) "여행 지출 결산" else "누적 지출 현황",
                            style = MaterialTheme.typography.titleLarge,
                        )
                        Spacer(Modifier.weight(1f))
                        TextButton(onClick = onDismiss) { Text("닫기") }
                    }
                    ExpenseSummaryContent(
                        summary = summary,
                        displayCurrency = displayCurrency,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun ExpenseSummaryContent(
    summary: ExpenseSummaryUiModel,
    displayCurrency: TripMoneyDisplayCurrency,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .testTag("expense_summary_dialog_content")
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
            if (summary.categories.isEmpty()) {
                Text(
                    "아직 기록된 지출이 없습니다.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Canvas(
                    modifier = Modifier
                        .size(176.dp)
                        .align(Alignment.CenterHorizontally)
                        .testTag("expense_summary_chart"),
                ) {
                    val stroke = Stroke(width = 32.dp.toPx(), cap = StrokeCap.Butt)
                    var startAngle = -90f
                    summary.categories.forEach { category ->
                        val sweepAngle = category.percentage / 100f * 360f
                        drawArc(
                            color = categoryColor(category.category),
                            startAngle = startAngle,
                            sweepAngle = sweepAngle,
                            useCenter = false,
                            style = stroke,
                        )
                        startAngle += sweepAngle
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    summary.categories.forEach { category ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Box(
                                Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(categoryColor(category.category))
                            )
                            Text(
                                "${category.categoryText} ${category.percentage.roundToInt()}%",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }
                HorizontalDivider()
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    summary.categories.forEach { category ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(category.categoryText)
                            Text(
                                if (displayCurrency == TripMoneyDisplayCurrency.KRW) {
                                    category.krwAmountText
                                } else {
                                    category.localAmountText
                                }
                            )
                        }
                    }
                }
            }
            HorizontalDivider()
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("총액", style = MaterialTheme.typography.titleSmall)
                Text(
                    text = if (displayCurrency == TripMoneyDisplayCurrency.KRW) {
                        summary.totalKrwText
                    } else {
                        summary.totalLocalText
                    },
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
    }
}

private fun categoryColor(category: ExpenseCategory): Color = when (category) {
    ExpenseCategory.Food -> Color(0xFFF28C28)
    ExpenseCategory.Transportation -> Color(0xFF3F7DD9)
    ExpenseCategory.Accommodation -> Color(0xFF8358C7)
    ExpenseCategory.Sightseeing -> Color(0xFF3A9B72)
    ExpenseCategory.Shopping -> Color(0xFFD75C91)
    ExpenseCategory.Other -> Color(0xFF7B818A)
}

@Composable
private fun BudgetOverviewCard(
    isReadOnly: Boolean,
    budget: BudgetSummaryUiModel?,
    tripCurrencyCode: String,
    inputRate: BudgetInputRate?,
    displayCurrency: TripMoneyDisplayCurrency,
    modifier: Modifier = Modifier,
    onCreateBudget: (String) -> Unit,
    onAddBudget: (String) -> Unit,
    onDisplayCurrencyChange: (TripMoneyDisplayCurrency) -> Unit,
) {
    var isFormVisible by rememberSaveable(budget != null) { mutableStateOf(false) }
    var amount by rememberSaveable { mutableStateOf("") }
    val convertedAmount = amount.toKrwAmountOrNull()?.let { krw ->
        inputRate?.let { BudgetCalculator.convertFromKrw(krw, it.krwPerUnit) }
    }
    OutlinedCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("여행 예산 · ${budget?.currencyCode ?: tripCurrencyCode}", style = MaterialTheme.typography.titleMedium)
            if (tripCurrencyCode != "KRW") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = displayCurrency == TripMoneyDisplayCurrency.Local,
                        onClick = { onDisplayCurrencyChange(TripMoneyDisplayCurrency.Local) },
                        label = { Text(tripCurrencyCode) },
                    )
                    FilterChip(
                        selected = displayCurrency == TripMoneyDisplayCurrency.KRW,
                        onClick = { onDisplayCurrencyChange(TripMoneyDisplayCurrency.KRW) },
                        label = { Text("원화") },
                    )
                }
            }
            if (budget == null) {
                if (isReadOnly) {
                    Text("등록된 예산이 없습니다.")
                } else {
                    Text("원화로 입력하면 여행 국가 통화인 $tripCurrencyCode 금액을 자동 계산합니다.")
                    TextButton(onClick = { isFormVisible = !isFormVisible }) {
                        Text(if (isFormVisible) "예산 설정 접기" else "예산 설정")
                    }
                }
            } else {
                Text(
                    if (displayCurrency == TripMoneyDisplayCurrency.KRW) {
                        budget.totalKrwText
                    } else {
                        "${budget.totalAmountText} ${budget.currencyCode}"
                    },
                    style = MaterialTheme.typography.headlineSmall,
                )
                Text(
                    "예상 지출 " + if (displayCurrency == TripMoneyDisplayCurrency.KRW) {
                        budget.estimatedKrwText
                    } else {
                        budget.estimatedLocalText
                    }
                )
                Text(
                    "실제 사용 " + if (displayCurrency == TripMoneyDisplayCurrency.KRW) {
                        budget.actualKrwText
                    } else {
                        budget.actualLocalText
                    },
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    "남은 예산 " + if (displayCurrency == TripMoneyDisplayCurrency.KRW) {
                        budget.remainingKrwText
                    } else {
                        budget.remainingLocalText
                    },
                    style = MaterialTheme.typography.titleMedium,
                    color = if (budget.isRemainingNegative) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                )
                if (!isReadOnly) {
                    TextButton(onClick = {
                        isFormVisible = !isFormVisible
                    }) { Text(if (isFormVisible) "추가 예산 접기" else "추가 예산") }
                }
            }
            if (!isReadOnly && isFormVisible) {
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it.formatMoneyInput() },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(if (budget == null) "최초 예산 (원화)" else "추가 예산 (원화)") },
                    suffix = { Text("원") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                )
                if (inputRate == null) {
                    Text(
                        "$tripCurrencyCode 환율 정보가 없습니다. 환율을 갱신한 뒤 다시 시도해 주세요.",
                        color = MaterialTheme.colorScheme.error,
                    )
                } else {
                    convertedAmount?.let {
                        Text(
                            "약 ${budgetNumberFormatter.format(it)} $tripCurrencyCode",
                            modifier = Modifier.testTag("budget_converted_amount"),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    Text(
                        "적용 환율 1 $tripCurrencyCode = ${budgetNumberFormatter.format(inputRate.krwPerUnit)}원" +
                            " · ${inputRate.rateDate.format(budgetRateDateFormatter)} 기준",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                TextButton(
                    onClick = {
                        if (budget == null) onCreateBudget(amount)
                        else onAddBudget(amount)
                    },
                    enabled = inputRate != null && convertedAmount != null,
                ) { Text("예산 저장") }
            }
        }
    }
}

@Composable
private fun ItineraryCard(
    isReadOnly: Boolean,
    item: ItineraryItemUiModel,
    modifier: Modifier = Modifier,
    displayCurrency: TripMoneyDisplayCurrency,
    onAddExpense: () -> Unit,
    onEditExpense: (Long) -> Unit,
    canAddPhotos: Boolean,
    photoIds: List<Long>,
    readPhoto: suspend (Long) -> ByteArray?,
    onAddPhotos: () -> Unit,
    onViewPhotos: (Int) -> Unit,
) {
    OutlinedCard(
        modifier = modifier.fillMaxWidth(),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
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
            ItineraryPhotoStrip(photoIds, readPhoto, onViewPhotos)
            item.expenses.forEach { expense ->
                val expenseDetails: @Composable () -> Unit = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(Modifier.weight(1f).padding(end = 8.dp)) {
                            Text("${expense.timeText} · ${expense.categoryText}")
                            ExpenseDescriptionText(expense.description)
                        }
                        Text("${expense.amountText} (${expense.secondaryAmountText})")
                    }
                }
                if (isReadOnly) {
                    expenseDetails()
                } else {
                    TextButton(
                        onClick = { onEditExpense(expense.id) },
                        modifier = Modifier.fillMaxWidth(),
                    ) { expenseDetails() }
                }
            }
            if (item.expenses.isNotEmpty()) {
                Text(
                    "실제 지출 합계 " + if (displayCurrency == TripMoneyDisplayCurrency.KRW) {
                        item.expenseTotalKrwText
                    } else {
                        item.expenseTotalLocalText
                    },
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            item.estimatedCost?.let {
                Text(
                    "예상 비용 ${it.convertedKrwText}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (canAddPhotos || !isReadOnly) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    if (canAddPhotos) TextButton(onClick = onAddPhotos) { Text("사진 추가") }
                    if (!isReadOnly) TextButton(onClick = onAddExpense) { Text("지출 추가") }
                }
            }
        }
    }
}

@Composable
private fun StandaloneExpenseCard(
    isReadOnly: Boolean,
    expense: ExpenseUiModel,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val content: @Composable () -> Unit = {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f).padding(end = 8.dp)) {
                Text("추가 지출", style = MaterialTheme.typography.labelLarge)
                Text(expense.categoryText)
                ExpenseDescriptionText(expense.description)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(expense.amountText, style = MaterialTheme.typography.titleMedium)
                Text(
                    expense.secondaryAmountText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
    if (isReadOnly) {
        OutlinedCard(modifier = modifier.fillMaxWidth()) { content() }
    } else {
        OutlinedCard(onClick = onClick, modifier = modifier.fillMaxWidth()) { content() }
    }
}

@Composable
private fun ExpenseDescriptionText(description: String) {
    if (description.isNotBlank()) {
        Text(
            description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ExpenseDescriptionField(value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text("설명 (선택)") },
        maxLines = 3,
    )
}

@Composable
private fun ExpenseInputDialog(
    currencyCode: String,
    occurredAtText: String,
    targetText: String,
    inputRate: BudgetInputRate?,
    onDismiss: () -> Unit,
    onSave: (String, ExpenseCategory, String, String) -> Unit,
) {
    var amount by rememberSaveable { mutableStateOf("") }
    var categoryName by rememberSaveable { mutableStateOf(ExpenseCategory.Food.name) }
    val category = ExpenseCategory.valueOf(categoryName)
    var description by rememberSaveable { mutableStateOf("") }
    var inputCurrencyCode by rememberSaveable(currencyCode) { mutableStateOf(currencyCode) }
    val parsedAmount = amount.toKrwAmountOrNull()
    val secondaryAmountText = parsedAmount?.let { value ->
        inputRate?.let { rate ->
            if (inputCurrencyCode == "KRW") {
                "약 ${budgetNumberFormatter.format(BudgetCalculator.convertFromKrw(value, rate.krwPerUnit))} $currencyCode"
            } else {
                "약 ${budgetNumberFormatter.format(BudgetCalculator.convertToKrw(value, rate.krwPerUnit))}원"
            }
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("지출 추가") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("기록 시각 $occurredAtText")
                if (targetText.isNotBlank()) {
                    Text(
                        targetText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (currencyCode != "KRW") {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(currencyCode, "KRW").forEach { code ->
                            FilterChip(
                                selected = inputCurrencyCode == code,
                                onClick = { inputCurrencyCode = code },
                                label = { Text(if (code == "KRW") "원화" else code) },
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it.formatMoneyInput() },
                    label = { Text("금액 ($inputCurrencyCode)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                )
                secondaryAmountText?.let {
                    Text(it, color = MaterialTheme.colorScheme.primary)
                }
                Text("카테고리", style = MaterialTheme.typography.labelLarge)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    ExpenseCategory.entries.chunked(3).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            row.forEach { item ->
                                FilterChip(
                                    selected = category == item,
                                    onClick = { categoryName = item.name },
                                    label = { Text(item.toKoreanLabel()) },
                                )
                            }
                        }
                    }
                }
                if (category == ExpenseCategory.Other) {
                    ExpenseDescriptionField(description) { description = it }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(amount, category, inputCurrencyCode, description) },
                enabled = amount.isNotBlank(),
            ) { Text("저장") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("취소") } },
    )
}

@Composable
private fun ExpenseEditDialog(
    editor: ExpenseEditorUiModel,
    tripCurrencyCode: String,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onSave: (String, ExpenseCategory, String, String, String) -> Unit,
    onDeleteRequest: () -> Unit,
) {
    var amount by rememberSaveable(editor.id) {
        mutableStateOf(editor.amountText.formatMoneyInput())
    }
    var timeText by rememberSaveable(editor.id) { mutableStateOf(editor.timeText) }
    var description by rememberSaveable(editor.id) { mutableStateOf(editor.description) }
    var categoryName by rememberSaveable(editor.id) { mutableStateOf(editor.category.name) }
    var inputCurrencyCode by rememberSaveable(editor.id) {
        mutableStateOf(editor.currencyCode)
    }
    val category = ExpenseCategory.valueOf(categoryName)
    val parsedAmount = amount.toKrwAmountOrNull()
    val secondaryAmountText = parsedAmount?.let { value ->
        if (inputCurrencyCode == "KRW") {
            "약 ${budgetNumberFormatter.format(BudgetCalculator.convertFromKrw(value, editor.inputRate.krwPerUnit))} $tripCurrencyCode"
        } else {
            "약 ${budgetNumberFormatter.format(BudgetCalculator.convertToKrw(value, editor.inputRate.krwPerUnit))}원"
        }
    }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        Surface(
            modifier = Modifier.fillMaxSize().testTag("expense_edit_dialog"),
            color = MaterialTheme.colorScheme.surface,
        ) {
            Scaffold(contentWindowInsets = WindowInsets.safeDrawing) { innerPadding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("지출 수정", style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.weight(1f))
                        TextButton(onClick = onDismiss) { Text("닫기") }
                    }
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 24.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text("지출 날짜 ${editor.dateText}")
                        OutlinedTextField(
                            value = timeText,
                            onValueChange = { timeText = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("지출 시간 (HH:mm)") },
                            singleLine = true,
                        )
                        if (tripCurrencyCode != "KRW") {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf(tripCurrencyCode, "KRW").forEach { code ->
                                    FilterChip(
                                        selected = inputCurrencyCode == code,
                                        onClick = {
                                            if (inputCurrencyCode != code) {
                                                parsedAmount?.let { value ->
                                                    val converted = if (code == "KRW") {
                                                        BudgetCalculator.convertToKrw(
                                                            value,
                                                            editor.inputRate.krwPerUnit,
                                                        )
                                                    } else {
                                                        BudgetCalculator.convertFromKrw(
                                                            value,
                                                            editor.inputRate.krwPerUnit,
                                                        )
                                                    }
                                                    amount = budgetNumberFormatter.format(converted)
                                                }
                                                inputCurrencyCode = code
                                            }
                                        },
                                        label = { Text(if (code == "KRW") "원화" else code) },
                                    )
                                }
                            }
                        }
                        OutlinedTextField(
                            value = amount,
                            onValueChange = { amount = it.formatMoneyInput() },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("금액 ($inputCurrencyCode)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                        )
                        secondaryAmountText?.let {
                            Text(it, color = MaterialTheme.colorScheme.primary)
                        }
                        Text(
                            "기록 환율 1 $tripCurrencyCode = " +
                                "${budgetNumberFormatter.format(editor.inputRate.krwPerUnit)}원 · " +
                                "${editor.inputRate.rateDate.format(budgetRateDateFormatter)} 기준",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text("카테고리", style = MaterialTheme.typography.labelLarge)
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            ExpenseCategory.entries.chunked(3).forEach { row ->
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    row.forEach { item ->
                                        FilterChip(
                                            selected = category == item,
                                            onClick = { categoryName = item.name },
                                            label = { Text(item.toKoreanLabel()) },
                                        )
                                    }
                                }
                            }
                        }
                        if (category == ExpenseCategory.Other) {
                            ExpenseDescriptionField(description) { description = it }
                        }
                        errorMessage?.let {
                            Text(it, color = MaterialTheme.colorScheme.error)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            TextButton(onClick = onDeleteRequest) {
                                Text("지출 삭제", color = MaterialTheme.colorScheme.error)
                            }
                            TextButton(
                                onClick = {
                                    onSave(amount, category, inputCurrencyCode, timeText, description)
                                },
                                enabled = amount.isNotBlank() && timeText.isNotBlank(),
                            ) { Text("수정 저장") }
                        }
                    }
                }
            }
        }
    }
}

private sealed interface TimelineRow {
    val sortMinutes: Int
    val stableOrder: Int
    val key: String

    data class Schedule(val item: ItineraryItemUiModel) : TimelineRow {
        override val sortMinutes = item.sortMinutes
        override val stableOrder = 0
        override val key = "schedule-${item.id}"
    }

    data class ExpenseItem(val expense: ExpenseUiModel) : TimelineRow {
        override val sortMinutes = expense.sortMinutes
        override val stableOrder = 1
        override val key = "expense-${expense.id}"
    }
}

@Composable
private fun ItineraryCostEditor(
    label: String,
    amount: String,
    onAmountChange: (String) -> Unit,
    currencyCode: String,
    onCurrencyChange: (String) -> Unit,
    rate: String,
    onRateChange: (String) -> Unit,
    budgetCurrencyCode: String,
) {
    Text(label, style = MaterialTheme.typography.labelLarge)
    OutlinedTextField(
        value = amount,
        onValueChange = { onAmountChange(it.formatMoneyInput()) },
        modifier = Modifier.fillMaxWidth(),
        label = { Text("금액") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true,
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(budgetCurrencyCode, "KRW").distinct().forEach { code ->
            FilterChip(
                selected = currencyCode == code,
                onClick = { onCurrencyChange(code) },
                label = { Text(code) },
            )
        }
    }
    if (currencyCode != "KRW") {
        OutlinedTextField(
            value = rate,
            onValueChange = onRateChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("외화 1단위당 원화 환율") },
            singleLine = true,
        )
    }
}

private val previewDays = listOf(
    TripDayUiModel(1, LocalDate.of(2026, 11, 9), "11.09"),
    TripDayUiModel(2, LocalDate.of(2026, 11, 10), "11.10"),
)

private fun String.toKrwAmountOrNull(): BigDecimal? =
    replace(",", "").trim().takeIf { it.isNotEmpty() }?.toBigDecimalOrNull()

internal fun String.formatMoneyInput(): String {
    val withoutCommas = replace(",", "")
    val sanitized = buildString {
        var hasDecimalPoint = false
        withoutCommas.forEach { character ->
            when {
                character.isDigit() -> append(character)
                character == '.' && !hasDecimalPoint -> {
                    append(character)
                    hasDecimalPoint = true
                }
            }
        }
    }
    if (sanitized.isEmpty()) return ""
    val hasTrailingDecimalPoint = sanitized.endsWith('.')
    val whole = sanitized.substringBefore('.').ifEmpty { "0" }
    val fraction = sanitized.substringAfter('.', missingDelimiterValue = "")
    val groupedWhole = whole.reversed().chunked(3).joinToString(",").reversed()
    return when {
        hasTrailingDecimalPoint -> "$groupedWhole."
        '.' in sanitized -> "$groupedWhole.$fraction"
        else -> groupedWhole
    }
}

private val budgetNumberFormatter = NumberFormat.getNumberInstance(Locale.KOREA).apply {
    maximumFractionDigits = 2
}
private val budgetRateDateFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd")

private val previewState = TripDetailUiState(
    isLoading = false,
    title = "제주 여행",
    destination = "제주특별자치도 제주시",
    dateText = "2026.11.09 - 2026.11.10",
    description = "친구와 함께하는 여행",
    days = previewDays,
    selectedDate = previewDays.first().date,
    isTripOngoing = true,
    statusText = "여행 1일차",
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
    TravelViewerTheme { TripDetailScreenContent(uiState = previewState) }
}

@Preview(showBackground = true)
@Composable
private fun TripDetailEmptyPreview() {
    TravelViewerTheme {
        TripDetailScreenContent(uiState = previewState.copy(itineraryItems = emptyList()))
    }
}

@Preview(showBackground = true)
@Composable
private fun TripDetailErrorPreview() {
    TravelViewerTheme {
        TripDetailScreenContent(
            uiState = TripDetailUiState(isLoading = false, isLoadFailed = true)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun TripDetailExpensePreview() {
    TravelViewerTheme {
        TripDetailScreenContent(
            uiState = previewState.copy(
                tripCurrencyCode = "JPY",
                moneyDisplayCurrency = TripMoneyDisplayCurrency.Local,
                expenseSummary = ExpenseSummaryUiModel(
                    categories = listOf(
                        ExpenseCategorySummaryUiModel(
                            ExpenseCategory.Food,
                            "식비",
                            62f,
                            "36,000원",
                            "3,800 JPY",
                        ),
                        ExpenseCategorySummaryUiModel(
                            ExpenseCategory.Transportation,
                            "교통",
                            38f,
                            "22,000원",
                            "2,320 JPY",
                        ),
                    ),
                    totalKrwText = "58,000원",
                    totalLocalText = "6,120 JPY",
                ),
                standaloneExpenses = listOf(
                    ExpenseUiModel(
                        id = 10,
                        timeText = "12:20",
                        categoryText = "기타",
                        description = "짐 보관 요금",
                        amountText = "1,200 JPY",
                        convertedKrwText = "11,424원",
                        sortMinutes = 740,
                    )
                ),
            )
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PastTripDetailPreview() {
    TravelViewerTheme {
        TripDetailScreenContent(uiState = previewState.copy(isTripEnded = true, statusText = "여행 종료"))
    }
}
