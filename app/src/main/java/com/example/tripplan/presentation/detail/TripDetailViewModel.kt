package com.example.tripplan.presentation.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tripplan.domain.model.ItineraryItem
import com.example.tripplan.domain.model.ItineraryCost
import com.example.tripplan.domain.model.ItineraryCostInput
import com.example.tripplan.domain.model.TripPlanDetails
import com.example.tripplan.domain.model.TripBudget
import com.example.tripplan.domain.model.TripDatePhase
import com.example.tripplan.domain.model.ExchangeRateSnapshot
import com.example.tripplan.domain.model.Expense
import com.example.tripplan.domain.model.ExpenseCategory
import com.example.tripplan.domain.model.RecordExpenseInput
import com.example.tripplan.domain.model.UpdateExpenseInput
import com.example.tripplan.domain.model.TripMoneyDisplayCurrency
import com.example.tripplan.domain.usecase.ObserveTripPlanDetailsUseCase
import com.example.tripplan.domain.usecase.CompleteTripPlanUseCase
import com.example.tripplan.domain.usecase.GetTripStatusUseCase
import com.example.tripplan.domain.usecase.TripCompletionResult
import com.example.tripplan.domain.usecase.UndoTripCompletionUseCase
import com.example.tripplan.domain.usecase.ItineraryMoveDirection
import com.example.tripplan.domain.usecase.MoveItineraryItemUseCase
import com.example.tripplan.domain.usecase.AddTripBudgetEntryUseCase
import com.example.tripplan.domain.usecase.BudgetMutationResult
import com.example.tripplan.domain.usecase.CreateTripBudgetUseCase
import com.example.tripplan.domain.usecase.ItineraryCostMutationResult
import com.example.tripplan.domain.usecase.ObserveTripBudgetUseCase
import com.example.tripplan.domain.usecase.UpdateItineraryCostsUseCase
import com.example.tripplan.domain.usecase.ObserveLatestExchangeRatesUseCase
import com.example.tripplan.domain.usecase.ObserveExpensesUseCase
import com.example.tripplan.domain.usecase.RecordExpenseResult
import com.example.tripplan.domain.usecase.RecordExpenseUseCase
import com.example.tripplan.domain.usecase.UpdateExpenseUseCase
import com.example.tripplan.domain.usecase.DeleteExpenseUseCase
import com.example.tripplan.domain.usecase.ExpenseMutationResult
import com.example.tripplan.domain.rule.ExpensePlacementRule
import com.example.tripplan.domain.rule.BudgetCalculator
import com.example.tripplan.domain.rule.ExpenseSummaryCalculator
import com.example.tripplan.domain.usecase.ObserveTripMoneyDisplayCurrencyUseCase
import com.example.tripplan.domain.usecase.UpdateTripMoneyDisplayCurrencyUseCase
import com.example.tripplan.domain.model.AddBudgetEntryInput
import com.example.tripplan.domain.model.CreateTripBudgetInput
import dagger.hilt.android.lifecycle.HiltViewModel
import java.math.BigDecimal
import java.time.Clock
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.text.NumberFormat
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class TripDetailViewModel @Inject constructor(
    private val observeTripPlanDetailsUseCase: ObserveTripPlanDetailsUseCase,
    private val moveItineraryItemUseCase: MoveItineraryItemUseCase,
    private val getTripStatusUseCase: GetTripStatusUseCase,
    private val completeTripPlanUseCase: CompleteTripPlanUseCase,
    private val undoTripCompletionUseCase: UndoTripCompletionUseCase,
    private val observeTripBudgetUseCase: ObserveTripBudgetUseCase,
    private val createTripBudgetUseCase: CreateTripBudgetUseCase,
    private val addTripBudgetEntryUseCase: AddTripBudgetEntryUseCase,
    private val updateItineraryCostsUseCase: UpdateItineraryCostsUseCase,
    private val observeLatestExchangeRatesUseCase: ObserveLatestExchangeRatesUseCase,
    private val observeExpensesUseCase: ObserveExpensesUseCase,
    private val recordExpenseUseCase: RecordExpenseUseCase,
    private val updateExpenseUseCase: UpdateExpenseUseCase,
    private val deleteExpenseUseCase: DeleteExpenseUseCase,
    private val observeTripMoneyDisplayCurrency: ObserveTripMoneyDisplayCurrencyUseCase,
    private val updateTripMoneyDisplayCurrency: UpdateTripMoneyDisplayCurrencyUseCase,
    private val clock: Clock,
) : ViewModel() {
    private val _uiState = MutableStateFlow(TripDetailUiState())
    val uiState: StateFlow<TripDetailUiState> = _uiState.asStateFlow()

    private var observationJob: Job? = null
    private var loadedTripPlanId: Long? = null
    private var currentDetails: TripPlanDetails? = null
    private var currentBudget: TripBudget? = null
    private var currentExchangeRates: ExchangeRateSnapshot? = null
    private var currentExpenses: List<Expense> = emptyList()
    private var pendingExpenseDate: LocalDate? = null
    private var pendingExpenseTime: LocalTime? = null
    private var pendingExpenseItineraryItemId: Long? = null

    fun load(tripPlanId: Long) {
        if (loadedTripPlanId == tripPlanId) return
        loadedTripPlanId = tripPlanId
        observationJob?.cancel()
        observationJob = viewModelScope.launch {
            _uiState.value = TripDetailUiState(isLoading = true)
            try {
                combine(
                    observeTripPlanDetailsUseCase(tripPlanId),
                    observeTripBudgetUseCase(tripPlanId),
                    observeLatestExchangeRatesUseCase(),
                    observeExpensesUseCase(tripPlanId),
                    observeTripMoneyDisplayCurrency(tripPlanId),
                ) { details, budget, exchangeRates, expenses, displayCurrency ->
                    ExpenseDetailData(
                        details,
                        budget,
                        exchangeRates,
                        expenses,
                        displayCurrency,
                    )
                }.collect { data ->
                    val details = data.details
                    val budget = data.budget
                    val exchangeRates = data.exchangeRates
                    currentDetails = details
                    currentBudget = budget
                    currentExchangeRates = exchangeRates
                    currentExpenses = data.expenses
                    val previous = _uiState.value
                    _uiState.value = details?.toUiState(
                        previous.selectedDate,
                        getTripStatusUseCase,
                        budget,
                        exchangeRates,
                        LocalDate.now(clock),
                        data.expenses,
                        data.displayCurrency,
                    )
                        ?.copy(
                            isExpenseDialogVisible = previous.isExpenseDialogVisible,
                            expenseOccurredAtText = previous.expenseOccurredAtText,
                            expenseTargetText = previous.expenseTargetText,
                            editingExpense = previous.editingExpense,
                            isExpenseDeleteConfirmationVisible =
                                previous.isExpenseDeleteConfirmationVisible,
                        )
                        ?: TripDetailUiState(isLoading = false, isNotFound = true)
                }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                _uiState.value = TripDetailUiState(isLoading = false, isLoadFailed = true)
            }
        }
    }

    fun onDaySelected(date: LocalDate) {
        val details = currentDetails ?: return
        if (details.days.none { it.date == date }) return
        _uiState.value = details.toUiState(
            date,
            getTripStatusUseCase,
            currentBudget,
            currentExchangeRates,
            LocalDate.now(clock),
            currentExpenses,
            _uiState.value.moneyDisplayCurrency,
        )
    }

    fun setMoneyDisplayCurrency(displayCurrency: TripMoneyDisplayCurrency) {
        val tripPlanId = loadedTripPlanId ?: return
        viewModelScope.launch {
            updateTripMoneyDisplayCurrency(tripPlanId, displayCurrency)
        }
    }

    fun onExpenseRequest() {
        val details = currentDetails ?: return
        val today = LocalDate.now(clock)
        val date = if (!today.isBefore(details.tripPlan.startDate) &&
            !today.isAfter(details.tripPlan.endDate)
        ) {
            today
        } else {
            _uiState.value.selectedDate ?: details.tripPlan.startDate
        }
        pendingExpenseDate = date
        pendingExpenseTime = LocalTime.now(clock).withSecond(0).withNano(0)
        pendingExpenseItineraryItemId = null
        _uiState.update {
            it.copy(
                isExpenseDialogVisible = true,
                expenseOccurredAtText = "$date ${pendingExpenseTime!!.format(timeFormatter)}",
                expenseTargetText = "현재 시각에 맞는 일정으로 자동 연결",
                mutationMessage = null,
            )
        }
    }

    fun onItineraryExpenseRequest(itemId: Long) {
        val item = currentDetails?.itineraryItems?.firstOrNull { it.id == itemId } ?: return
        val now = LocalTime.now(clock).withSecond(0).withNano(0)
        val time = if (
            item.startTime != null && item.endTime != null &&
            !now.isBefore(item.startTime) && now.isBefore(item.endTime)
        ) {
            now
        } else {
            item.startTime ?: now
        }
        pendingExpenseDate = item.date
        pendingExpenseTime = time
        pendingExpenseItineraryItemId = item.id
        _uiState.update {
            it.copy(
                isExpenseDialogVisible = true,
                expenseOccurredAtText = "${item.date} ${time.format(timeFormatter)}",
                expenseTargetText = "${item.title} 일정에 연결",
                mutationMessage = null,
            )
        }
    }

    fun onExpenseDismiss() {
        pendingExpenseDate = null
        pendingExpenseTime = null
        pendingExpenseItineraryItemId = null
        _uiState.update { it.copy(isExpenseDialogVisible = false) }
    }

    fun recordExpense(
        amount: String,
        category: ExpenseCategory,
        inputCurrencyCode: String,
    ) {
        val details = currentDetails ?: return
        val date = pendingExpenseDate ?: return
        val time = pendingExpenseTime ?: return
        val parsedAmount = amount.toMoneyOrNull()
        val tripCurrencyCode = details.tripPlan.currencyCode
        val normalizedInputCurrency = inputCurrencyCode.trim().uppercase()
        if (normalizedInputCurrency !in setOf(tripCurrencyCode, "KRW")) {
            showMutationMessage("지출 통화를 확인해 주세요.")
            return
        }
        val rate = currentBudgetRate(tripCurrencyCode)
        if (parsedAmount == null || parsedAmount <= BigDecimal.ZERO) {
            showMutationMessage("지출 금액을 확인해 주세요.")
            return
        }
        if (rate == null) {
            showMutationMessage("$tripCurrencyCode 환율 정보가 없어 지출을 저장할 수 없습니다.")
            return
        }
        val itineraryItemId = pendingExpenseItineraryItemId ?:
            ExpensePlacementRule.findItineraryItemId(details.itineraryItems, date, time)
        viewModelScope.launch {
            try {
                when (
                    recordExpenseUseCase(
                        RecordExpenseInput(
                            tripPlanId = details.tripPlan.id,
                            itineraryItemId = itineraryItemId,
                            occurredDate = date,
                            occurredTime = time,
                            amount = parsedAmount,
                            currencyCode = normalizedInputCurrency,
                            krwPerUnit = rate.krwPerUnit,
                            rateDate = rate.rateDate,
                            category = category,
                        )
                    )
                ) {
                    is RecordExpenseResult.Success -> {
                        pendingExpenseDate = null
                        pendingExpenseTime = null
                        pendingExpenseItineraryItemId = null
                        _uiState.update {
                            it.copy(isExpenseDialogVisible = false, mutationMessage = null)
                        }
                    }
                    else -> showMutationMessage("지출을 저장하지 못했습니다.")
                }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                showMutationMessage("지출을 저장하지 못했습니다.")
            }
        }
    }

    fun onExpenseEditRequest(expenseId: Long) {
        val expense = currentExpenses.firstOrNull { it.id == expenseId } ?: return
        _uiState.update {
            it.copy(
                editingExpense = ExpenseEditorUiModel(
                    id = expense.id,
                    dateText = expense.occurredDate.toString(),
                    timeText = expense.occurredTime.format(timeFormatter),
                    amountText = expense.amount.asDisplayNumber(),
                    currencyCode = expense.currencyCode,
                    category = expense.category,
                    inputRate = BudgetInputRate(expense.krwPerUnit, expense.rateDate),
                ),
                isExpenseDeleteConfirmationVisible = false,
                mutationMessage = null,
            )
        }
    }

    fun onExpenseEditDismiss() {
        _uiState.update {
            it.copy(editingExpense = null, isExpenseDeleteConfirmationVisible = false)
        }
    }

    fun updateExpense(
        amount: String,
        category: ExpenseCategory,
        inputCurrencyCode: String,
        timeText: String,
    ) {
        val details = currentDetails ?: return
        val editor = _uiState.value.editingExpense ?: return
        val original = currentExpenses.firstOrNull { it.id == editor.id } ?: return
        val parsedAmount = amount.toMoneyOrNull()
        val parsedTime = runCatching { LocalTime.parse(timeText.trim(), timeFormatter) }.getOrNull()
        val normalizedCurrency = inputCurrencyCode.trim().uppercase()
        if (parsedAmount == null || parsedAmount <= BigDecimal.ZERO) {
            showMutationMessage("지출 금액을 확인해 주세요.")
            return
        }
        if (parsedTime == null) {
            showMutationMessage("지출 시간을 HH:mm 형식으로 입력해 주세요.")
            return
        }
        if (normalizedCurrency !in setOf(details.tripPlan.currencyCode, "KRW")) {
            showMutationMessage("지출 통화를 확인해 주세요.")
            return
        }
        val itineraryItemId = ExpensePlacementRule.findItineraryItemId(
            details.itineraryItems,
            original.occurredDate,
            parsedTime,
        )
        viewModelScope.launch {
            try {
                when (
                    updateExpenseUseCase(
                        UpdateExpenseInput(
                            expenseId = original.id,
                            tripPlanId = original.tripPlanId,
                            itineraryItemId = itineraryItemId,
                            occurredDate = original.occurredDate,
                            occurredTime = parsedTime,
                            amount = parsedAmount,
                            currencyCode = normalizedCurrency,
                            category = category,
                        )
                    )
                ) {
                    ExpenseMutationResult.Success -> _uiState.update {
                        it.copy(
                            editingExpense = null,
                            isExpenseDeleteConfirmationVisible = false,
                            mutationMessage = null,
                        )
                    }
                    else -> showMutationMessage("지출을 수정하지 못했습니다.")
                }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                showMutationMessage("지출을 수정하지 못했습니다.")
            }
        }
    }

    fun onExpenseDeleteRequest() {
        if (_uiState.value.editingExpense == null) return
        _uiState.update { it.copy(isExpenseDeleteConfirmationVisible = true) }
    }

    fun onExpenseDeleteDismiss() {
        _uiState.update { it.copy(isExpenseDeleteConfirmationVisible = false) }
    }

    fun deleteExpense() {
        val expenseId = _uiState.value.editingExpense?.id ?: return
        viewModelScope.launch {
            try {
                when (deleteExpenseUseCase(expenseId)) {
                    ExpenseMutationResult.Success -> _uiState.update {
                        it.copy(
                            editingExpense = null,
                            isExpenseDeleteConfirmationVisible = false,
                            mutationMessage = null,
                        )
                    }
                    else -> showMutationMessage("지출을 삭제하지 못했습니다.")
                }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                showMutationMessage("지출을 삭제하지 못했습니다.")
            }
        }
    }

    fun createBudget(krwAmount: String) {
        val tripPlanId = loadedTripPlanId ?: return
        val currencyCode = currentDetails?.tripPlan?.currencyCode ?: return
        val parsedAmount = krwAmount.toMoneyOrNull()
        val rate = currentBudgetRate(currencyCode)
        if (parsedAmount == null) {
            showMutationMessage("원화 예산 금액을 확인해 주세요.")
            return
        }
        if (rate == null) {
            showMutationMessage("$currencyCode 환율 정보가 없어 예산을 환산할 수 없습니다.")
            return
        }
        viewModelScope.launch {
            try {
                when (
                    createTripBudgetUseCase(
                        tripPlanId,
                        CreateTripBudgetInput(
                            currencyCode = currencyCode.trim(),
                            krwAmount = parsedAmount,
                            krwPerUnit = rate.krwPerUnit,
                            rateDate = rate.rateDate,
                        ),
                    )
                ) {
                    is BudgetMutationResult.Success -> showMutationMessage(null)
                    else -> showMutationMessage("여행 예산을 저장하지 못했습니다. 입력값을 확인해 주세요.")
                }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                showMutationMessage("여행 예산을 저장하지 못했습니다.")
            }
        }
    }

    fun addBudget(krwAmount: String) {
        val tripPlanId = loadedTripPlanId ?: return
        val currencyCode = currentDetails?.tripPlan?.currencyCode ?: return
        val parsedAmount = krwAmount.toMoneyOrNull()
        val rate = currentBudgetRate(currencyCode)
        if (parsedAmount == null) {
            showMutationMessage("추가 원화 예산 금액을 확인해 주세요.")
            return
        }
        if (rate == null) {
            showMutationMessage("$currencyCode 환율 정보가 없어 예산을 환산할 수 없습니다.")
            return
        }
        viewModelScope.launch {
            try {
                when (
                    addTripBudgetEntryUseCase(
                        tripPlanId,
                        AddBudgetEntryInput(parsedAmount, rate.krwPerUnit, rate.rateDate),
                    )
                ) {
                    is BudgetMutationResult.Success -> showMutationMessage(null)
                    else -> showMutationMessage("추가 예산을 저장하지 못했습니다.")
                }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                showMutationMessage("추가 예산을 저장하지 못했습니다.")
            }
        }
    }

    private fun currentBudgetRate(currencyCode: String): BudgetInputRate? {
        if (currencyCode == "KRW") {
            return BudgetInputRate(BigDecimal.ONE, LocalDate.now(clock))
        }
        val snapshot = currentExchangeRates ?: return null
        val rate = snapshot.rates.firstOrNull { it.currencyCode == currencyCode } ?: return null
        return BudgetInputRate(rate.krwPerUnit, snapshot.rateDate)
    }

    fun saveItineraryEstimatedCost(
        itemId: Long,
        estimatedAmount: String,
        estimatedCurrencyCode: String,
        estimatedRate: String,
    ) {
        val estimated = parseCostInput(estimatedAmount, estimatedCurrencyCode, estimatedRate)
        if (estimatedAmount.isNotBlank() && estimated == null) {
            showMutationMessage("예상 비용과 적용 환율을 확인해 주세요.")
            return
        }
        viewModelScope.launch {
            try {
                when (updateItineraryCostsUseCase(itemId, estimated)) {
                    ItineraryCostMutationResult.Success -> showMutationMessage(null)
                    ItineraryCostMutationResult.BudgetNotFound ->
                        showMutationMessage("먼저 여행 총예산을 설정해 주세요.")
                    else -> showMutationMessage("일정 비용을 저장하지 못했습니다. 입력값을 확인해 주세요.")
                }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                showMutationMessage("일정 비용을 저장하지 못했습니다.")
            }
        }
    }

    private fun parseCostInput(amount: String, currencyCode: String, rate: String): ItineraryCostInput? {
        if (amount.isBlank()) return null
        val parsedAmount = amount.toMoneyOrNull() ?: return null
        val normalizedCurrency = currencyCode.trim().uppercase()
        val parsedRate = if (normalizedCurrency == "KRW") BigDecimal.ONE else rate.toMoneyOrNull()
            ?: return null
        return ItineraryCostInput(parsedAmount, normalizedCurrency, parsedRate)
    }

    private fun showMutationMessage(message: String?) {
        _uiState.update { it.copy(mutationMessage = message) }
    }

    fun onCompletionRequest() {
        _uiState.value = _uiState.value.copy(isCompletionConfirmationVisible = true)
    }

    fun onCompletionDismiss() {
        _uiState.value = _uiState.value.copy(isCompletionConfirmationVisible = false)
    }

    fun onCompletionConfirm() {
        val tripPlanId = loadedTripPlanId ?: return
        val wasCompleted = _uiState.value.isCompleted
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isCompletionConfirmationVisible = false,
                isCompletionSaving = true,
                isMutationFailed = false,
            )
            try {
                val result = if (wasCompleted) {
                    undoTripCompletionUseCase(tripPlanId)
                } else {
                    completeTripPlanUseCase(tripPlanId)
                }
                if (result == TripCompletionResult.NotFound) {
                    _uiState.value = _uiState.value.copy(isNotFound = true)
                } else {
                    _uiState.value = _uiState.value.copy(isCompletionSaving = false)
                }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                _uiState.value = _uiState.value.copy(
                    isCompletionSaving = false,
                    isMutationFailed = true,
                )
            }
        }
    }

    fun onMoveUp(itemId: Long) = moveItem(itemId, ItineraryMoveDirection.Up)

    fun onMoveDown(itemId: Long) = moveItem(itemId, ItineraryMoveDirection.Down)

    private fun moveItem(itemId: Long, direction: ItineraryMoveDirection) {
        viewModelScope.launch {
            try {
                moveItineraryItemUseCase(itemId, direction)
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                _uiState.value = _uiState.value.copy(isMutationFailed = true)
            }
        }
    }
}

data class TripDetailUiState(
    val isLoading: Boolean = true,
    val isNotFound: Boolean = false,
    val isLoadFailed: Boolean = false,
    val isMutationFailed: Boolean = false,
    val title: String = "",
    val destination: String = "",
    val dateText: String = "",
    val description: String = "",
    val days: List<TripDayUiModel> = emptyList(),
    val selectedDate: LocalDate? = null,
    val itineraryItems: List<ItineraryItemUiModel> = emptyList(),
    val statusText: String = "",
    val isCompleted: Boolean = false,
    val isCompletionSaving: Boolean = false,
    val isCompletionConfirmationVisible: Boolean = false,
    val budget: BudgetSummaryUiModel? = null,
    val selectedDayActualKrwText: String = "0원",
    val selectedDayActualLocalText: String = "0 KRW",
    val mutationMessage: String? = null,
    val tripCurrencyCode: String = "KRW",
    val budgetInputRate: BudgetInputRate? = null,
    val standaloneExpenses: List<ExpenseUiModel> = emptyList(),
    val isExpenseDialogVisible: Boolean = false,
    val expenseOccurredAtText: String = "",
    val expenseTargetText: String = "",
    val moneyDisplayCurrency: TripMoneyDisplayCurrency = TripMoneyDisplayCurrency.KRW,
    val expenseSummary: ExpenseSummaryUiModel = ExpenseSummaryUiModel(),
    val isTripEnded: Boolean = false,
    val editingExpense: ExpenseEditorUiModel? = null,
    val isExpenseDeleteConfirmationVisible: Boolean = false,
)

data class ExpenseEditorUiModel(
    val id: Long,
    val dateText: String,
    val timeText: String,
    val amountText: String,
    val currencyCode: String,
    val category: ExpenseCategory,
    val inputRate: BudgetInputRate,
)

data class ExpenseSummaryUiModel(
    val categories: List<ExpenseCategorySummaryUiModel> = emptyList(),
    val totalKrwText: String = "0원",
    val totalLocalText: String = "0 KRW",
)

data class ExpenseCategorySummaryUiModel(
    val category: ExpenseCategory,
    val categoryText: String,
    val percentage: Float,
    val krwAmountText: String,
    val localAmountText: String,
)

data class TripDayUiModel(
    val dayNumber: Int,
    val date: LocalDate,
    val dateText: String,
)

data class ItineraryItemUiModel(
    val id: Long,
    val title: String,
    val place: String,
    val timeText: String,
    val note: String,
    val canMoveUp: Boolean,
    val canMoveDown: Boolean,
    val estimatedCost: ItineraryCostUiModel? = null,
    val expenses: List<ExpenseUiModel> = emptyList(),
    val sortMinutes: Int = Int.MAX_VALUE,
    val expenseTotalKrwText: String = "0원",
    val expenseTotalLocalText: String = "0 KRW",
)

data class ExpenseUiModel(
    val id: Long,
    val timeText: String,
    val categoryText: String,
    val amountText: String,
    val convertedKrwText: String,
    val sortMinutes: Int,
    val secondaryAmountText: String = convertedKrwText,
)

data class ItineraryCostUiModel(
    val amountText: String,
    val currencyCode: String,
    val rateText: String,
    val convertedKrwText: String,
)

data class BudgetSummaryUiModel(
    val currencyCode: String,
    val totalAmountText: String,
    val totalKrwText: String,
    val estimatedKrwText: String,
    val actualKrwText: String,
    val remainingKrwText: String,
    val suggestedRateText: String,
    val estimatedLocalText: String = "",
    val actualLocalText: String = "",
    val remainingLocalText: String = "",
    val isRemainingNegative: Boolean = false,
)

data class BudgetInputRate(
    val krwPerUnit: BigDecimal,
    val rateDate: LocalDate,
)

private fun TripPlanDetails.toUiState(
    previousSelectedDate: LocalDate?,
    getTripStatus: GetTripStatusUseCase,
    budget: TripBudget?,
    exchangeRates: ExchangeRateSnapshot?,
    today: LocalDate,
    expenses: List<Expense>,
    storedDisplayCurrency: TripMoneyDisplayCurrency?,
): TripDetailUiState {
    val selectedDate = previousSelectedDate
        ?.takeIf { selected -> days.any { it.date == selected } }
        ?: days.firstOrNull()?.date

    val selectedItems = itineraryItems.filter { it.date == selectedDate }
    val estimatedKrw = itineraryItems.sumCosts { it.estimatedCost }
    val expenseKrw = expenses.fold(BigDecimal.ZERO) { total, expense ->
        total + expense.convertedKrwAmount
    }
    val selectedExpenses = expenses.filter { it.occurredDate == selectedDate }
    val actualKrw = expenseKrw
    val selectedDayActualKrw = selectedExpenses.fold(BigDecimal.ZERO) { total, expense ->
            total + expense.convertedKrwAmount
        }

    val status = getTripStatus(tripPlan)
    val displayCurrency = storedDisplayCurrency ?: if (
        status.dateStatus.phase == TripDatePhase.Ongoing
    ) {
        TripMoneyDisplayCurrency.Local
    } else {
        TripMoneyDisplayCurrency.KRW
    }
    val tripRate = if (tripPlan.currencyCode == "KRW") {
        BigDecimal.ONE
    } else {
        exchangeRates?.rates?.firstOrNull { it.currencyCode == tripPlan.currencyCode }?.krwPerUnit
            ?: budget?.entries?.maxByOrNull { it.order }?.krwPerUnit
    }
    val estimatedLocal = itineraryItems.sumLocalCosts(tripPlan.currencyCode, tripRate) {
        it.estimatedCost
    }
    val expenseLocal = expenses.fold(BigDecimal.ZERO) { total, expense ->
        total + expense.asTripCurrencyAmount(tripPlan.currencyCode, tripRate)
    }
    val actualLocal = expenseLocal
    val selectedDayActualLocal = selectedExpenses.fold(BigDecimal.ZERO) { total, expense ->
        total + expense.asTripCurrencyAmount(tripPlan.currencyCode, tripRate)
    }
    val categoryTotals = ExpenseSummaryCalculator.calculate(
        expenses = expenses,
        tripCurrencyCode = tripPlan.currencyCode,
        fallbackKrwPerUnit = tripRate,
    )
    val categoryTotalKrw = categoryTotals.sumOf { it.krwAmount }
    val categoryTotalLocal = categoryTotals.sumOf { it.localAmount }
    return TripDetailUiState(
        isLoading = false,
        title = tripPlan.title,
        destination = tripPlan.destination,
        dateText = "${tripPlan.startDate.format(detailDateFormatter)} - " +
            tripPlan.endDate.format(detailDateFormatter),
        description = tripPlan.description,
        tripCurrencyCode = tripPlan.currencyCode,
        moneyDisplayCurrency = displayCurrency,
        budgetInputRate = if (tripPlan.currencyCode == "KRW") {
            BudgetInputRate(BigDecimal.ONE, today)
        } else {
            exchangeRates?.rates?.firstOrNull { it.currencyCode == tripPlan.currencyCode }?.let {
                BudgetInputRate(it.krwPerUnit, exchangeRates.rateDate)
            }
        },
        isCompleted = status.isCompleted,
        isTripEnded = status.dateStatus.phase == TripDatePhase.Ended,
        statusText = if (status.isCompleted) "완료" else when (status.dateStatus.phase) {
            TripDatePhase.Upcoming -> "D-${status.dateStatus.daysUntilStart}"
            TripDatePhase.Ongoing -> if (status.dateStatus.currentDayNumber == 1) {
                "D-Day"
            } else {
                "여행 ${status.dateStatus.currentDayNumber}일차"
            }
            TripDatePhase.Ended -> "여행 종료"
        },
        days = days.map {
            TripDayUiModel(
                dayNumber = it.dayNumber,
                date = it.date,
                dateText = it.date.format(dayDateFormatter),
            )
        },
        selectedDate = selectedDate,
        budget = budget?.let {
            BudgetSummaryUiModel(
                currencyCode = it.currencyCode,
                totalAmountText = it.totalAmount.asDisplayNumber(),
                totalKrwText = it.totalConvertedKrwAmount.asWon(),
                estimatedKrwText = estimatedKrw.asWon(),
                actualKrwText = actualKrw.asWon(),
                remainingKrwText = (it.totalConvertedKrwAmount - actualKrw).asWon(),
                suggestedRateText = it.entries.maxByOrNull { entry -> entry.order }
                    ?.krwPerUnit?.toPlainString().orEmpty(),
                estimatedLocalText = estimatedLocal.asMoney(tripPlan.currencyCode),
                actualLocalText = actualLocal.asMoney(tripPlan.currencyCode),
                remainingLocalText = (it.totalAmount - actualLocal).asMoney(tripPlan.currencyCode),
                isRemainingNegative = it.totalConvertedKrwAmount - actualKrw < BigDecimal.ZERO,
            )
        },
        selectedDayActualKrwText = selectedDayActualKrw.asWon(),
        selectedDayActualLocalText = selectedDayActualLocal.asMoney(tripPlan.currencyCode),
        expenseSummary = ExpenseSummaryUiModel(
            categories = categoryTotals.map { total ->
                ExpenseCategorySummaryUiModel(
                    category = total.category,
                    categoryText = total.category.toKoreanLabel(),
                    percentage = if (categoryTotalKrw.signum() == 0) {
                        0f
                    } else {
                        total.krwAmount.divide(categoryTotalKrw, 4, java.math.RoundingMode.HALF_UP)
                            .multiply(BigDecimal(100)).toFloat()
                    },
                    krwAmountText = total.krwAmount.asWon(),
                    localAmountText = total.localAmount.asMoney(tripPlan.currencyCode),
                )
            },
            totalKrwText = categoryTotalKrw.asWon(),
            totalLocalText = categoryTotalLocal.asMoney(tripPlan.currencyCode),
        ),
        standaloneExpenses = selectedExpenses
            .filter { it.itineraryItemId == null }
            .map { it.toUiModel(tripPlan.currencyCode) },
        itineraryItems = selectedItems.mapIndexed { index, item ->
                item.toUiModel(
                    canMoveUp = index > 0 && selectedItems[index - 1].startTime == item.startTime,
                    canMoveDown = index < selectedItems.lastIndex &&
                        selectedItems[index + 1].startTime == item.startTime,
                    expenses = selectedExpenses.filter { it.itineraryItemId == item.id },
                    tripCurrencyCode = tripPlan.currencyCode,
                    tripRate = tripRate,
                )
            },
    )
}

private fun ItineraryItem.toUiModel(
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    expenses: List<Expense>,
    tripCurrencyCode: String,
    tripRate: BigDecimal?,
) = ItineraryItemUiModel(
    id = id,
    title = title,
    place = place,
    timeText = formatTimeRange(startTime, endTime),
    note = note,
    canMoveUp = canMoveUp,
    canMoveDown = canMoveDown,
    estimatedCost = estimatedCost?.toUiModel(),
    expenses = expenses.map { it.toUiModel(currencyCode = tripCurrencyCode) },
    sortMinutes = startTime?.let { it.hour * 60 + it.minute } ?: Int.MAX_VALUE,
    expenseTotalKrwText = expenses.fold(BigDecimal.ZERO) { total, expense ->
        total + expense.convertedKrwAmount
    }.asWon(),
    expenseTotalLocalText = expenses.fold(BigDecimal.ZERO) { total, expense ->
        total + expense.asTripCurrencyAmount(
            tripCurrencyCode,
            tripRate,
        )
    }.asMoney(tripCurrencyCode),
)

private fun Expense.toUiModel(currencyCode: String) = ExpenseUiModel(
    id = id,
    timeText = occurredTime.format(timeFormatter),
    categoryText = category.toKoreanLabel(),
    amountText = "${amount.asDisplayNumber()} $currencyCode",
    convertedKrwText = convertedKrwAmount.asWon(),
    sortMinutes = occurredTime.hour * 60 + occurredTime.minute,
    secondaryAmountText = if (this.currencyCode == "KRW" && currencyCode != "KRW") {
        BudgetCalculator.convertFromKrw(convertedKrwAmount, krwPerUnit).asMoney(currencyCode)
    } else {
        convertedKrwAmount.asWon()
    },
)

fun ExpenseCategory.toKoreanLabel(): String = when (this) {
    ExpenseCategory.Food -> "식비"
    ExpenseCategory.Transportation -> "교통"
    ExpenseCategory.Accommodation -> "숙박"
    ExpenseCategory.Sightseeing -> "관광"
    ExpenseCategory.Shopping -> "쇼핑"
    ExpenseCategory.Other -> "기타"
}

private data class ExpenseDetailData(
    val details: TripPlanDetails?,
    val budget: TripBudget?,
    val exchangeRates: ExchangeRateSnapshot?,
    val expenses: List<Expense>,
    val displayCurrency: TripMoneyDisplayCurrency?,
)

private fun ItineraryCost.toUiModel() = ItineraryCostUiModel(
    amountText = amount.toPlainString(),
    currencyCode = currencyCode,
    rateText = krwPerUnit.toPlainString(),
    convertedKrwText = convertedKrwAmount.asWon(),
)

private fun List<ItineraryItem>.sumCosts(selector: (ItineraryItem) -> ItineraryCost?): BigDecimal =
    fold(BigDecimal.ZERO) { total, item -> total + (selector(item)?.convertedKrwAmount ?: BigDecimal.ZERO) }

private fun List<ItineraryItem>.sumLocalCosts(
    tripCurrencyCode: String,
    fallbackRate: BigDecimal?,
    selector: (ItineraryItem) -> ItineraryCost?,
): BigDecimal = fold(BigDecimal.ZERO) { total, item ->
    val cost = selector(item)
    total + when {
        cost == null -> BigDecimal.ZERO
        cost.currencyCode == tripCurrencyCode -> cost.amount
        fallbackRate != null -> BudgetCalculator.convertFromKrw(
            cost.convertedKrwAmount,
            fallbackRate,
        )
        else -> BigDecimal.ZERO
    }
}

private fun Expense.asTripCurrencyAmount(
    tripCurrencyCode: String,
    fallbackRate: BigDecimal?,
): BigDecimal = when {
    currencyCode == tripCurrencyCode -> amount
    currencyCode == "KRW" -> BudgetCalculator.convertFromKrw(convertedKrwAmount, krwPerUnit)
    fallbackRate != null -> BudgetCalculator.convertFromKrw(convertedKrwAmount, fallbackRate)
    else -> BigDecimal.ZERO
}

private fun BigDecimal.asWon(): String = "${asDisplayNumber()}원"

private fun BigDecimal.asMoney(currencyCode: String): String =
    "${asDisplayNumber()} $currencyCode"

private fun BigDecimal.asDisplayNumber(): String = numberFormatter.format(this)

private fun String.toMoneyOrNull(): BigDecimal? =
    replace(",", "").trim().takeIf { it.isNotEmpty() }?.toBigDecimalOrNull()

private fun formatTimeRange(startTime: LocalTime?, endTime: LocalTime?): String = when {
    startTime != null && endTime != null ->
        "${startTime.format(timeFormatter)} - ${endTime.format(timeFormatter)}"
    startTime != null -> startTime.format(timeFormatter)
    endTime != null -> "~ ${endTime.format(timeFormatter)}"
    else -> ""
}

private val detailDateFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd")
private val dayDateFormatter = DateTimeFormatter.ofPattern("MM.dd")
private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
private val numberFormatter = NumberFormat.getNumberInstance(Locale.KOREA)
