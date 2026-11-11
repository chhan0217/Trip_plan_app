package com.example.tripplan.presentation.detail

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import com.example.tripplan.presentation.create.ComposeTestActivity
import java.time.LocalDate
import java.math.BigDecimal
import com.example.tripplan.domain.model.TripMoneyDisplayCurrency
import com.example.tripplan.domain.model.ExpenseCategory
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class TripDetailScreenTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComposeTestActivity>()

    @Test
    fun emptyDay_displaysTripAndEmptyScheduleMessage() {
        composeRule.setContent {
            MaterialTheme { TripDetailScreenContent(uiState = sampleState()) }
        }

        composeRule.onNodeWithText("제주 여행").assertIsDisplayed()
        composeRule.onNodeWithTag("trip_detail_list").performScrollToNode(hasText("1일차"))
        composeRule.onNodeWithText("1일차").assertIsDisplayed()
        composeRule.onNodeWithTag("trip_detail_list")
            .performScrollToNode(hasText("이 날짜에는 등록된 일정이 없습니다."))
        composeRule.onNodeWithText("이 날짜에는 등록된 일정이 없습니다.").assertIsDisplayed()
    }

    @Test
    fun dayChip_invokesSelectedDate() {
        var selectedDate: LocalDate? = null
        val secondDay = LocalDate.of(2026, 11, 10)
        composeRule.setContent {
            MaterialTheme {
                TripDetailScreenContent(
                    uiState = sampleState(),
                    onDaySelected = { selectedDate = it },
                )
            }
        }

        composeRule.onNodeWithTag("trip_detail_list").performScrollToNode(hasText("2일차"))
        composeRule.onNodeWithText("2일차").performClick()

        composeRule.runOnIdle { assertEquals(secondDay, selectedDate) }
    }

    @Test
    fun addAction_invokesSelectedDate() {
        var addedDate: LocalDate? = null
        composeRule.setContent {
            MaterialTheme {
                TripDetailScreenContent(
                    uiState = sampleState(),
                    onAddItinerary = { addedDate = it },
                )
            }
        }

        composeRule.onNodeWithTag("trip_detail_list").performScrollToNode(hasText("일정 추가"))
        composeRule.onNodeWithText("일정 추가").performClick()

        composeRule.runOnIdle { assertEquals(LocalDate.of(2026, 11, 9), addedDate) }
    }

    @Test
    fun editTripAction_invokesCallback() {
        var clicked = false
        composeRule.setContent {
            MaterialTheme {
                TripDetailScreenContent(
                    uiState = sampleState(),
                    onEditTrip = { clicked = true },
                )
            }
        }

        composeRule.onNodeWithText("여행 수정").performClick()

        composeRule.runOnIdle { org.junit.Assert.assertTrue(clicked) }
    }

    @Test
    fun exportAction_invokesCallback() {
        var clicked = false
        composeRule.setContent {
            MaterialTheme {
                TripDetailScreenContent(
                    uiState = sampleState(),
                    onExportTrip = { clicked = true },
                )
            }
        }

        composeRule.onNodeWithText("파일 공유").performClick()

        composeRule.runOnIdle { org.junit.Assert.assertTrue(clicked) }
    }

    @Test
    fun budgetAndDailyActual_areShownAndCostSectionExpands() {
        val state = sampleState().copy(
            budget = BudgetSummaryUiModel(
                currencyCode = "JPY",
                totalAmountText = "350,000",
                totalKrwText = "3,341,000원",
                estimatedKrwText = "1,500,000원",
                actualKrwText = "1,700,000원",
                remainingKrwText = "1,641,000원",
                suggestedRateText = "9.52",
            ),
            selectedDayActualKrwText = "171,360원",
            itineraryItems = listOf(
                ItineraryItemUiModel(
                    id = 1,
                    title = "스시 오마카세",
                    place = "긴자",
                    timeText = "10:00",
                    note = "",
                    canMoveUp = false,
                    canMoveDown = false,
                )
            ),
        )
        composeRule.setContent {
            MaterialTheme { TripDetailScreenContent(uiState = state) }
        }

        composeRule.onNodeWithTag("trip_detail_list")
            .performScrollToNode(hasText("실제 사용 1,700,000원"))
        composeRule.onNodeWithText("실제 사용 1,700,000원").assertIsDisplayed()
        composeRule.onNodeWithTag("trip_detail_list").performScrollToNode(hasText("171,360원"))
        composeRule.onNodeWithText("171,360원").assertIsDisplayed()
        composeRule.onNodeWithTag("trip_detail_list")
            .performScrollToNode(hasText("예상 비용 미입력 · 펼치기"))
        composeRule.onNodeWithText("예상 비용 미입력 · 펼치기").performClick()
        composeRule.onNodeWithText("예상 비용").assertIsDisplayed()
    }

    @Test
    fun krwBudgetInput_immediatelyShowsTripCurrencyAmount() {
        val state = sampleState().copy(
            tripCurrencyCode = "JPY",
            budgetInputRate = BudgetInputRate(
                krwPerUnit = BigDecimal("9.52"),
                rateDate = LocalDate.of(2026, 11, 8),
            ),
        )
        composeRule.setContent {
            MaterialTheme { TripDetailScreenContent(uiState = state) }
        }

        composeRule.onNodeWithText("예산 설정").performClick()
        composeRule.onNodeWithText("최초 예산 (원화)").performTextInput("1000000")

        composeRule.onNodeWithTag("budget_converted_amount")
            .assertTextEquals("약 105,042.02 JPY")
    }

    @Test
    fun standaloneExpense_showsTimeCategoryAndAmountInTimeline() {
        var editedExpenseId: Long? = null
        val state = sampleState().copy(
            standaloneExpenses = listOf(
                ExpenseUiModel(
                    id = 8,
                    timeText = "10:30",
                    categoryText = "교통",
                    amountText = "1,200 JPY",
                    convertedKrwText = "11,424원",
                    sortMinutes = 630,
                )
            )
        )
        composeRule.setContent {
            MaterialTheme {
                TripDetailScreenContent(
                    uiState = state,
                    onExpenseEditRequest = { editedExpenseId = it },
                )
            }
        }

        composeRule.onNodeWithTag("trip_detail_list")
            .performScrollToNode(hasText("10:30 · 교통"))
        composeRule.onNodeWithText("추가 지출").assertIsDisplayed()
        composeRule.onNodeWithText("10:30 · 교통").assertIsDisplayed()
        composeRule.onNodeWithText("1,200 JPY").assertIsDisplayed()
        composeRule.onNodeWithText("추가 지출").performClick()
        composeRule.runOnIdle { assertEquals(8L, editedExpenseId) }
    }

    @Test
    fun expenseEditDialog_prefillsValuesAndInvokesUpdate() {
        var update: List<Any>? = null
        val editor = ExpenseEditorUiModel(
            id = 8,
            dateText = "2026-11-09",
            timeText = "10:30",
            amountText = "2,500",
            currencyCode = "JPY",
            category = ExpenseCategory.Food,
            inputRate = BudgetInputRate(
                BigDecimal("9.52"),
                LocalDate.of(2026, 11, 8),
            ),
        )
        composeRule.setContent {
            MaterialTheme {
                TripDetailScreenContent(
                    uiState = sampleState().copy(
                        tripCurrencyCode = "JPY",
                        editingExpense = editor,
                    ),
                    onUpdateExpense = { amount, category, currency, time ->
                        update = listOf(amount, category, currency, time)
                    },
                )
            }
        }

        composeRule.onNodeWithTag("expense_edit_dialog").assertIsDisplayed()
        composeRule.onNodeWithText("지출 수정").assertIsDisplayed()
        composeRule.onNodeWithText("교통").performClick()
        composeRule.onNodeWithText("수정 저장").performClick()

        composeRule.runOnIdle {
            assertEquals(
                listOf("2,500", ExpenseCategory.Transportation, "JPY", "10:30"),
                update,
            )
        }
    }

    @Test
    fun expenseDelete_requiresConfirmation() {
        var deleted = false
        var state by mutableStateOf(
            sampleState().copy(
                tripCurrencyCode = "JPY",
                editingExpense = ExpenseEditorUiModel(
                    id = 8,
                    dateText = "2026-11-09",
                    timeText = "10:30",
                    amountText = "2,500",
                    currencyCode = "JPY",
                    category = ExpenseCategory.Food,
                    inputRate = BudgetInputRate(
                        BigDecimal("9.52"),
                        LocalDate.of(2026, 11, 8),
                    ),
                ),
            )
        )
        composeRule.setContent {
            MaterialTheme {
                TripDetailScreenContent(
                    uiState = state,
                    onExpenseDeleteRequest = {
                        state = state.copy(isExpenseDeleteConfirmationVisible = true)
                    },
                    onDeleteExpense = { deleted = true },
                )
            }
        }

        composeRule.onNodeWithText("지출 삭제").performClick()
        composeRule.onNodeWithText("이 지출 기록을 삭제할까요?").assertIsDisplayed()
        composeRule.onNodeWithText("삭제").performClick()
        composeRule.runOnIdle { org.junit.Assert.assertTrue(deleted) }
    }

    @Test
    fun expenseDialog_showsCapturedTimeCurrencyAndCategories() {
        val state = sampleState().copy(
            tripCurrencyCode = "JPY",
            isExpenseDialogVisible = true,
            expenseOccurredAtText = "2026-11-09 12:15",
            budgetInputRate = BudgetInputRate(
                BigDecimal("9.52"),
                LocalDate.of(2026, 11, 8),
            ),
        )
        composeRule.setContent {
            MaterialTheme { TripDetailScreenContent(uiState = state) }
        }

        composeRule.onNodeWithText("기록 시각 2026-11-09 12:15").assertIsDisplayed()
        composeRule.onNodeWithText("금액 (JPY)").assertIsDisplayed()
        composeRule.onNodeWithText("식비").assertIsDisplayed()
        composeRule.onNodeWithText("교통").assertIsDisplayed()
        composeRule.onNodeWithText("금액 (JPY)").performTextInput("2500")
        composeRule.onNodeWithText("약 23,800원").assertIsDisplayed()
    }

    @Test
    fun budgetSummary_usesOneSelectedCurrencyAndInvokesCurrencyChange() {
        var selected: TripMoneyDisplayCurrency? = null
        val state = sampleState().copy(
            tripCurrencyCode = "JPY",
            moneyDisplayCurrency = TripMoneyDisplayCurrency.Local,
            budget = BudgetSummaryUiModel(
                currencyCode = "JPY",
                totalAmountText = "105,042.02",
                totalKrwText = "1,000,000원",
                estimatedKrwText = "200,000원",
                actualKrwText = "238,000원",
                remainingKrwText = "762,000원",
                suggestedRateText = "9.52",
                estimatedLocalText = "21,008.40 JPY",
                actualLocalText = "25,000 JPY",
                remainingLocalText = "80,042.02 JPY",
            ),
        )
        composeRule.setContent {
            MaterialTheme {
                TripDetailScreenContent(
                    uiState = state,
                    onMoneyDisplayCurrencyChange = { selected = it },
                )
            }
        }

        composeRule.onNodeWithText("105,042.02 JPY").assertIsDisplayed()
        composeRule.onNodeWithText("원화").performClick()
        composeRule.runOnIdle { assertEquals(TripMoneyDisplayCurrency.KRW, selected) }
    }

    @Test
    fun expenseSummary_showsDonutPercentagesUsedCategoriesAndLocalTotal() {
        val state = sampleState().copy(
            tripCurrencyCode = "JPY",
            moneyDisplayCurrency = TripMoneyDisplayCurrency.Local,
            expenseSummary = ExpenseSummaryUiModel(
                categories = listOf(
                    ExpenseCategorySummaryUiModel(
                        ExpenseCategory.Food,
                        "식비",
                        60f,
                        "36,000원",
                        "3,800 JPY",
                    ),
                    ExpenseCategorySummaryUiModel(
                        ExpenseCategory.Transportation,
                        "교통",
                        40f,
                        "24,000원",
                        "2,500 JPY",
                    ),
                ),
                totalKrwText = "60,000원",
                totalLocalText = "6,300 JPY",
            ),
        )
        composeRule.setContent {
            MaterialTheme { TripDetailScreenContent(uiState = state) }
        }

        composeRule.onNodeWithTag("trip_detail_list")
            .performScrollToNode(hasText("지출 현황 보기"))
        composeRule.onNodeWithText("지출 현황 보기").performClick()
        composeRule.onNodeWithText("누적 지출 현황").assertIsDisplayed()
        composeRule.onNodeWithTag("expense_summary_dialog").assertIsDisplayed()
        composeRule.onNodeWithTag("expense_summary_chart").assertIsDisplayed()
        composeRule.onNodeWithText("식비 60%").assertIsDisplayed()
        composeRule.onNodeWithTag("expense_summary_dialog_content")
            .performScrollToNode(hasText("3,800 JPY"))
        composeRule.onNodeWithText("3,800 JPY").assertIsDisplayed()
        composeRule.onNodeWithTag("expense_summary_dialog_content")
            .performScrollToNode(hasText("6,300 JPY"))
        composeRule.onNodeWithText("6,300 JPY").assertIsDisplayed()
    }

    @Test
    fun endedTrip_labelsExpenseSummaryAsSettlement() {
        composeRule.setContent {
            MaterialTheme {
                TripDetailScreenContent(uiState = sampleState().copy(isTripEnded = true))
            }
        }

        composeRule.onNodeWithTag("trip_detail_list")
            .performScrollToNode(hasText("지출 현황 보기"))
        composeRule.onNodeWithText("지출 현황 보기").performClick()
        composeRule.onNodeWithText("여행 지출 결산").assertIsDisplayed()
    }

    private fun sampleState(): TripDetailUiState {
        val firstDay = LocalDate.of(2026, 11, 9)
        return TripDetailUiState(
            isLoading = false,
            title = "제주 여행",
            destination = "제주",
            dateText = "2026.11.09 - 2026.11.10",
            days = listOf(
                TripDayUiModel(1, firstDay, "11.09"),
                TripDayUiModel(2, LocalDate.of(2026, 11, 10), "11.10"),
            ),
            selectedDate = firstDay,
        )
    }
}
