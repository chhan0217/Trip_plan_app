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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
    onCreateBudget: (String) -> Unit = {},
    onAddBudget: (String) -> Unit = {},
    onSaveEstimatedCost: (Long, String, String, String) -> Unit = { _, _, _, _ -> },
    onExpenseRequest: () -> Unit = {},
    onItineraryExpenseRequest: (Long) -> Unit = {},
    onExpenseDismiss: () -> Unit = {},
    onRecordExpense: (String, ExpenseCategory, String) -> Unit = { _, _, _ -> },
    onExpenseEditRequest: (Long) -> Unit = {},
    onExpenseEditDismiss: () -> Unit = {},
    onUpdateExpense: (String, ExpenseCategory, String, String) -> Unit = { _, _, _, _ -> },
    onExpenseDeleteRequest: () -> Unit = {},
    onExpenseDeleteDismiss: () -> Unit = {},
    onDeleteExpense: () -> Unit = {},
    onMoneyDisplayCurrencyChange: (TripMoneyDisplayCurrency) -> Unit = {},
) {
    var isExpenseSummaryVisible by rememberSaveable { mutableStateOf(false) }
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
    if (uiState.isExpenseDialogVisible) {
        ExpenseInputDialog(
            currencyCode = uiState.tripCurrencyCode,
            occurredAtText = uiState.expenseOccurredAtText,
            targetText = uiState.expenseTargetText,
            inputRate = uiState.budgetInputRate,
            onDismiss = onExpenseDismiss,
            onSave = onRecordExpense,
        )
    }
    uiState.editingExpense?.let { editor ->
        ExpenseEditDialog(
            editor = editor,
            tripCurrencyCode = uiState.tripCurrencyCode,
            errorMessage = uiState.mutationMessage,
            onDismiss = onExpenseEditDismiss,
            onSave = onUpdateExpense,
            onDeleteRequest = onExpenseDeleteRequest,
        )
    }
    if (uiState.isExpenseDeleteConfirmationVisible) {
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
            isTripEnded = uiState.isTripEnded,
            onDismiss = { isExpenseSummaryVisible = false },
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
            BudgetOverviewCard(
                budget = uiState.budget,
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
            OutlinedCard(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("1일 사용금액", style = MaterialTheme.typography.labelLarge)
                    Text(
                        if (uiState.moneyDisplayCurrency == TripMoneyDisplayCurrency.KRW) {
                            uiState.selectedDayActualKrwText
                        } else {
                            uiState.selectedDayActualLocalText
                        },
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = onExpenseRequest) { Text("지출 추가") }
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
            items(timelineRows, key = { it.key }) { row ->
                when (row) {
                    is TimelineRow.Schedule -> ItineraryCard(
                        item = row.item,
                        modifier = Modifier.padding(horizontal = 20.dp),
                        onClick = { onEditItinerary(row.item.id) },
                        onMoveUp = { onMoveUp(row.item.id) },
                        onMoveDown = { onMoveDown(row.item.id) },
                        budget = uiState.budget,
                        displayCurrency = uiState.moneyDisplayCurrency,
                        onSaveEstimatedCost = onSaveEstimatedCost,
                        onAddExpense = { onItineraryExpenseRequest(row.item.id) },
                        onEditExpense = onExpenseEditRequest,
                    )
                    is TimelineRow.ExpenseItem -> StandaloneExpenseCard(
                        expense = row.expense,
                        modifier = Modifier.padding(horizontal = 20.dp),
                        onClick = { onExpenseEditRequest(row.expense.id) },
                    )
                }
            }
        }
        item { Box(Modifier.padding(bottom = 12.dp)) }
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
                Text("원화로 입력하면 여행 국가 통화인 $tripCurrencyCode 금액을 자동 계산합니다.")
                TextButton(onClick = { isFormVisible = !isFormVisible }) {
                    Text(if (isFormVisible) "예산 설정 접기" else "예산 설정")
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
                TextButton(onClick = {
                    isFormVisible = !isFormVisible
                }) { Text(if (isFormVisible) "추가 예산 접기" else "추가 예산") }
            }
            if (isFormVisible) {
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
    item: ItineraryItemUiModel,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    budget: BudgetSummaryUiModel?,
    displayCurrency: TripMoneyDisplayCurrency,
    onSaveEstimatedCost: (Long, String, String, String) -> Unit,
    onAddExpense: () -> Unit,
    onEditExpense: (Long) -> Unit,
) {
    var isCostExpanded by rememberSaveable(item.id) { mutableStateOf(false) }
    var estimatedAmount by rememberSaveable(item.id, item.estimatedCost?.amountText) {
        mutableStateOf(item.estimatedCost?.amountText.orEmpty().formatMoneyInput())
    }
    var estimatedCurrency by rememberSaveable(item.id, item.estimatedCost?.currencyCode) {
        mutableStateOf(item.estimatedCost?.currencyCode ?: budget?.currencyCode.orEmpty())
    }
    var estimatedRate by rememberSaveable(item.id, item.estimatedCost?.rateText) {
        mutableStateOf(item.estimatedCost?.rateText ?: budget?.suggestedRateText.orEmpty())
    }
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
            item.expenses.forEach { expense ->
                TextButton(
                    onClick = { onEditExpense(expense.id) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text("${expense.timeText} · ${expense.categoryText}")
                        Text("${expense.amountText} (${expense.secondaryAmountText})")
                    }
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
            TextButton(onClick = { isCostExpanded = !isCostExpanded }) {
                Text(
                    item.estimatedCost?.let { "예상 ${it.convertedKrwText}" }
                        ?.plus(if (isCostExpanded) " · 접기" else " · 펼치기")
                        ?: if (isCostExpanded) "예상 비용 접기" else "예상 비용 미입력 · 펼치기"
                )
            }
            if (isCostExpanded) {
                if (budget == null) {
                    Text("먼저 상단에서 여행 예산을 설정해 주세요.")
                } else {
                    ItineraryCostEditor(
                        label = "예상 비용",
                        amount = estimatedAmount,
                        onAmountChange = { estimatedAmount = it },
                        currencyCode = estimatedCurrency,
                        onCurrencyChange = { estimatedCurrency = it },
                        rate = estimatedRate,
                        onRateChange = { estimatedRate = it },
                        budgetCurrencyCode = budget.currencyCode,
                    )
                    TextButton(
                        onClick = {
                            onSaveEstimatedCost(
                                item.id,
                                estimatedAmount,
                                estimatedCurrency,
                                estimatedRate,
                            )
                        },
                    ) { Text("예상 비용 저장") }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = onAddExpense) { Text("지출 추가") }
                TextButton(onClick = onMoveUp, enabled = item.canMoveUp) { Text("위로") }
                TextButton(onClick = onMoveDown, enabled = item.canMoveDown) { Text("아래로") }
            }
        }
    }
}

@Composable
private fun StandaloneExpenseCard(
    expense: ExpenseUiModel,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    OutlinedCard(onClick = onClick, modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text("추가 지출", style = MaterialTheme.typography.labelLarge)
                Text("${expense.timeText} · ${expense.categoryText}")
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
}

@Composable
private fun ExpenseInputDialog(
    currencyCode: String,
    occurredAtText: String,
    targetText: String,
    inputRate: BudgetInputRate?,
    onDismiss: () -> Unit,
    onSave: (String, ExpenseCategory, String) -> Unit,
) {
    var amount by rememberSaveable { mutableStateOf("") }
    var categoryName by rememberSaveable { mutableStateOf(ExpenseCategory.Food.name) }
    val category = ExpenseCategory.valueOf(categoryName)
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
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(amount, category, inputCurrencyCode) },
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
    onSave: (String, ExpenseCategory, String, String) -> Unit,
    onDeleteRequest: () -> Unit,
) {
    var amount by rememberSaveable(editor.id) {
        mutableStateOf(editor.amountText.formatMoneyInput())
    }
    var timeText by rememberSaveable(editor.id) { mutableStateOf(editor.timeText) }
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
                                    onSave(amount, category, inputCurrencyCode, timeText)
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

@Preview(showBackground = true)
@Composable
private fun TripDetailExpensePreview() {
    MaterialTheme {
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
                        categoryText = "교통",
                        amountText = "1,200 JPY",
                        convertedKrwText = "11,424원",
                        sortMinutes = 740,
                    )
                ),
            )
        )
    }
}
