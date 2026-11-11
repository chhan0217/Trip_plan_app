package com.example.tripplan.presentation.create

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class CreateTripScreenTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComposeTestActivity>()

    @Test
    fun emptyState_displaysRequiredInputsAndSaveAction() {
        composeRule.setContent {
            MaterialTheme {
                CreateTripScreenContent(uiState = CreateTripUiState())
            }
        }

        composeRule.onNodeWithText("여행 이름").assertIsDisplayed()
        composeRule.onNodeWithText("대한민국").assertIsDisplayed()
        composeRule.onNodeWithText("KRW · 대한민국 원").assertIsDisplayed()
        composeRule.onNodeWithText("여행 지역").assertIsDisplayed()
        composeRule.onNodeWithText("여행 날짜").assertIsDisplayed()
        composeRule.onNodeWithText("여행 만들기").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun errorState_displaysValidationAndSaveErrors() {
        composeRule.setContent {
            MaterialTheme {
                CreateTripScreenContent(
                    uiState = CreateTripUiState(
                        isTitleError = true,
                        dateError = CreateTripDateError.MissingRange,
                        isSaveFailed = true,
                    )
                )
            }
        }

        composeRule.onNodeWithText("여행 이름을 입력해 주세요.").assertIsDisplayed()
        composeRule.onNodeWithText("여행 날짜를 선택해 주세요.").assertIsDisplayed()
        composeRule.onNodeWithText("여행을 저장하지 못했습니다. 다시 시도해 주세요.")
            .performScrollTo().assertIsDisplayed()
    }

    @Test
    fun saveButton_invokesSaveCallback() {
        var wasClicked = false
        composeRule.setContent {
            MaterialTheme {
                CreateTripScreenContent(
                    uiState = CreateTripUiState(),
                    onSaveClick = { wasClicked = true },
                )
            }
        }

        composeRule.onNodeWithText("여행 만들기").performScrollTo().performClick()

        composeRule.runOnIdle {
            assertTrue(wasClicked)
        }
    }
}
