package com.example.tripplan.presentation.edit

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.tripplan.presentation.create.ComposeTestActivity
import java.time.LocalDate
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class EditTripScreenTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComposeTestActivity>()

    @Test
    fun dataState_displaysInputsAndSaveAction() {
        composeRule.setContent {
            MaterialTheme { EditTripScreenContent(uiState = state()) }
        }

        composeRule.onNodeWithText("제주 여행").assertIsDisplayed()
        composeRule.onNodeWithText("여행 날짜").assertIsDisplayed()
        composeRule.onNodeWithText("변경사항 저장").assertIsDisplayed()
        composeRule.onNodeWithText("여행 삭제").assertIsDisplayed()
    }

    @Test
    fun saveButton_invokesCallback() {
        var clicked = false
        composeRule.setContent {
            MaterialTheme {
                EditTripScreenContent(uiState = state(), onSaveClick = { clicked = true })
            }
        }

        composeRule.onNodeWithText("변경사항 저장").performClick()
        composeRule.runOnIdle { assertTrue(clicked) }
    }

    @Test
    fun outOfRangeState_displaysExplicitWarning() {
        composeRule.setContent {
            MaterialTheme {
                EditTripScreenContent(uiState = state().copy(outOfRangeItemCount = 2))
            }
        }

        composeRule.onNodeWithText("새 여행 기간을 벗어나는 일정 2개가 삭제됩니다. 계속할까요?")
            .assertIsDisplayed()
        composeRule.onNodeWithText("삭제 후 변경").assertIsDisplayed()
    }

    @Test
    fun deleteState_displaysTripAndScheduleWarning() {
        composeRule.setContent {
            MaterialTheme {
                EditTripScreenContent(uiState = state().copy(isDeleteConfirmationVisible = true))
            }
        }

        composeRule.onNodeWithText("여행과 모든 일정을 삭제할까요? 삭제한 데이터는 복구할 수 없습니다.")
            .assertIsDisplayed()
    }

    private fun state() = EditTripUiState(
        isLoading = false,
        tripPlanId = 7L,
        title = "제주 여행",
        destination = "제주",
        startDate = LocalDate.of(2026, 11, 9),
        endDate = LocalDate.of(2026, 11, 11),
        description = "친구와 여행",
    )
}
