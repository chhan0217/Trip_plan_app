package com.example.tripplan.presentation.detail

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.tripplan.presentation.create.ComposeTestActivity
import java.time.LocalDate
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
        composeRule.onNodeWithText("1일차").assertIsDisplayed()
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
