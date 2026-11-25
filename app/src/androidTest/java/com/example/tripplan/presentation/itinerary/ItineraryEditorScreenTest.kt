package com.example.tripplan.presentation.itinerary

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import com.example.tripplan.presentation.create.ComposeTestActivity
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ItineraryEditorScreenTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComposeTestActivity>()

    @Test
    fun createMode_displaysInputsAndAddAction() {
        composeRule.setContent {
            MaterialTheme { ItineraryEditorScreenContent(uiState = createState()) }
        }

        composeRule.onNodeWithText("일정 제목").assertIsDisplayed()
        composeRule.onNodeWithText("장소").assertIsDisplayed()
        composeRule.onNodeWithText("시작 시간").assertIsDisplayed()
        composeRule.onNode(hasText("일정 추가") and hasClickAction()).assertIsDisplayed()
    }

    @Test
    fun saveButton_invokesCallback() {
        var clicked = false
        composeRule.setContent {
            MaterialTheme {
                ItineraryEditorScreenContent(
                    uiState = createState(),
                    onSaveClick = { clicked = true },
                )
            }
        }

        composeRule.onNode(hasText("일정 추가") and hasClickAction()).performClick()
        composeRule.runOnIdle { assertTrue(clicked) }
    }

    @Test
    fun startTimePicker_selectsHourAndMinuteFromLists() {
        var changedValue: String? = null
        composeRule.setContent {
            MaterialTheme {
                ItineraryEditorScreenContent(
                    uiState = createState(),
                    onStartTimeChange = { changedValue = it },
                )
            }
        }

        composeRule.onNodeWithTag("start_time_picker").performClick()
        composeRule.onNodeWithText("시작 시간 선택").assertIsDisplayed()
        composeRule.onNodeWithTag("time_values_시").performScrollToIndex(23)
        composeRule.onNodeWithText("23시").performClick()
        composeRule.onNodeWithTag("time_values_분").performScrollToIndex(59)
        composeRule.onNodeWithText("59분").performClick()
        composeRule.onNodeWithText("확인").performClick()

        composeRule.runOnIdle { assertEquals("23:59", changedValue) }
    }

    @Test
    fun clearStartTime_invokesEmptyValueCallback() {
        var changedValue = "unchanged"
        composeRule.setContent {
            MaterialTheme {
                ItineraryEditorScreenContent(
                    uiState = createState().copy(startTime = "09:30"),
                    onStartTimeChange = { changedValue = it },
                )
            }
        }

        composeRule.onNodeWithTag("start_time_picker_clear").performClick()

        composeRule.runOnIdle { assertTrue(changedValue.isEmpty()) }
    }

    @Test
    fun deleteRequest_displaysConfirmationDialog() {
        composeRule.setContent {
            MaterialTheme {
                ItineraryEditorScreenContent(
                    uiState = createState().copy(
                        isEdit = true,
                        itemId = 3L,
                        isDeleteConfirmationVisible = true,
                    )
                )
            }
        }

        composeRule.onNodeWithText("이 일정을 삭제할까요? 삭제한 일정은 복구할 수 없습니다.")
            .assertIsDisplayed()
        composeRule.onNode(hasText("삭제") and hasClickAction()).assertIsDisplayed()
    }

    private fun createState(): ItineraryEditorUiState {
        val date = LocalDate.of(2026, 11, 9)
        return ItineraryEditorUiState(
            isLoading = false,
            tripPlanId = 7L,
            days = listOf(EditorDayUiModel(1, date)),
            selectedDate = date,
        )
    }
}
