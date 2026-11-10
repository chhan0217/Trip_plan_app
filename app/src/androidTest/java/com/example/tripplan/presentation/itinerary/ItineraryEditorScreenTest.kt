package com.example.tripplan.presentation.itinerary

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.tripplan.presentation.create.ComposeTestActivity
import java.time.LocalDate
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
