package com.example.tripplan.presentation.home

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.tripplan.presentation.create.ComposeTestActivity
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class HomeScreenTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComposeTestActivity>()

    @Test
    fun emptyState_displaysAddActionAndEmptyMessage() {
        composeRule.setContent {
            MaterialTheme { HomeScreenContent(uiState = HomeUiState(isLoading = false)) }
        }

        composeRule.onNodeWithText("새 여행 추가").assertIsDisplayed()
        composeRule.onNodeWithText("여행 파일 가져오기").assertIsDisplayed()
        composeRule.onNodeWithText("등록된 여행이 없습니다.").assertIsDisplayed()
    }

    @Test
    fun tripCard_invokesSelectedTripId() {
        var selectedId: Long? = null
        composeRule.setContent {
            MaterialTheme {
                HomeScreenContent(
                    uiState = HomeUiState(
                        isLoading = false,
                        upcomingTrips = listOf(
                            HomeTripUiModel(7L, "제주 여행", "2026.11.09 - 2026.11.11", "제주")
                        ),
                    ),
                    onTripClick = { selectedId = it },
                )
            }
        }

        composeRule.onNodeWithText("제주 여행").performClick()

        composeRule.runOnIdle { assertEquals(7L, selectedId) }
    }

    @Test
    fun exchangeRateCard_showsSelectedRatesAndReferenceDate() {
        composeRule.setContent {
            MaterialTheme {
                HomeScreenContent(
                    uiState = HomeUiState(
                        isLoading = false,
                        exchangeRates = listOf(
                            HomeExchangeRateUiModel("USD", "미국 달러", 1, "1,466.8"),
                        ),
                        exchangeRateDateText = "2026.11.10 기준",
                    ),
                )
            }
        }

        composeRule.onNodeWithText("오늘의 환율").assertIsDisplayed()
        composeRule.onNodeWithText("미국 달러").assertIsDisplayed()
        composeRule.onNodeWithText("1 USD = 1,466.8원").assertIsDisplayed()
        composeRule.onNodeWithText("2026.11.10 기준").assertIsDisplayed()
    }

    @Test
    fun currencySelector_invokesSelectionChange() {
        var selectedUnit: String? = null
        composeRule.setContent {
            MaterialTheme {
                HomeScreenContent(
                    uiState = HomeUiState(
                        isLoading = false,
                        availableCurrencies = listOf(
                            HomeExchangeRateUiModel("USD", "미국 달러", 1, "1,466.8"),
                            HomeExchangeRateUiModel("EUR", "유로", 1, "1,692.4"),
                        ),
                        selectedCurrencyUnits = setOf("USD"),
                        isCurrencySelectorVisible = true,
                    ),
                    onCurrencySelectionChanged = { unit, selected ->
                        if (selected) selectedUnit = unit
                    },
                )
            }
        }

        composeRule.onNodeWithText("유로 (EUR)").performClick()

        composeRule.runOnIdle { assertEquals("EUR", selectedUnit) }
    }
}
