package com.example.tripplan.presentation.file

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.tripplan.domain.model.TripPlanFileFailure
import com.example.tripplan.domain.model.TripPlanImportPreview
import com.example.tripplan.presentation.create.ComposeTestActivity
import java.time.LocalDate
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class TripPlanFileScreensTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComposeTestActivity>()

    @Test
    fun exportReady_showsShareAndSaveActions() {
        composeRule.setContent {
            MaterialTheme {
                TripPlanExportContent(
                    state = TripPlanExportUiState(fileName = "제주.tripplan.json")
                )
            }
        }

        composeRule.onNodeWithText("친구에게 공유").assertIsDisplayed()
        composeRule.onNodeWithText("기기에 파일 저장").assertIsDisplayed()
    }

    @Test
    fun importPreview_showsSummaryAndInvokesConfirm() {
        var clicked = false
        composeRule.setContent {
            MaterialTheme {
                TripPlanImportContent(
                    state = TripPlanImportUiState(
                        preview = TripPlanImportPreview(
                            title = "제주 여행",
                            destination = "제주",
                            startDate = LocalDate.of(2026, 11, 9),
                            endDate = LocalDate.of(2026, 11, 11),
                            dayCount = 3,
                            itineraryItemCount = 4,
                        )
                    ),
                    onImportClick = { clicked = true },
                )
            }
        }

        composeRule.onNodeWithText("3일 · 일정 4개").assertIsDisplayed()
        composeRule.onNodeWithText("새 여행으로 가져오기").performClick()
        composeRule.runOnIdle { assertTrue(clicked) }
    }

    @Test
    fun invalidFile_showsClearError() {
        composeRule.setContent {
            MaterialTheme {
                TripPlanImportContent(
                    state = TripPlanImportUiState(failure = TripPlanFileFailure.InvalidFormat)
                )
            }
        }

        composeRule.onNodeWithText("여행 계획 파일 형식이 아닙니다.").assertIsDisplayed()
    }
}
