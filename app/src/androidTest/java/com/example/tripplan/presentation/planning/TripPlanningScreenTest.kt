package com.example.tripplan.presentation.planning

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performTextInput
import com.example.tripplan.presentation.create.ComposeTestActivity
import com.example.tripplan.presentation.lodging.LodgingDayInfo
import com.example.tripplan.presentation.lodging.LodgingEditor
import com.example.tripplan.presentation.lodging.LodgingUiModel
import com.example.tripplan.presentation.lodging.LodgingUiState
import com.example.tripplan.presentation.lodging.LodgingPlaceUiModel
import com.example.tripplan.presentation.lodging.googleMapsUri
import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class TripPlanningScreenTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComposeTestActivity>()

    @Test
    fun draft_showsTemporarySaveAndFinalizeActions() {
        composeRule.setContent {
            MaterialTheme {
                TripPlanningScreenContent(
                    uiState = sampleState.copy(isDraft = true),
                )
            }
        }

        composeRule.onNodeWithText("임시저장").assertIsDisplayed()
        composeRule.onNodeWithText("계획 완료").assertIsDisplayed()
        composeRule.onNodeWithText("일정 추가").assertIsDisplayed()
    }

    @Test
    fun scheduleClick_opensPlanningEditor() {
        var selectedId: Long? = null
        composeRule.setContent {
            MaterialTheme {
                TripPlanningScreenContent(
                    uiState = sampleState,
                    onEditItinerary = { selectedId = it },
                )
            }
        }

        composeRule.onNodeWithText("한시장 방문").performClick()

        composeRule.runOnIdle { assertEquals(3L, selectedId) }
    }

    @Test
    fun moveMenu_selectsAnotherDayForTheSchedule() {
        var requestedMove: Pair<Long, LocalDate>? = null
        val secondDay = LocalDate.of(2026, 11, 21)
        composeRule.setContent {
            MaterialTheme {
                TripPlanningScreenContent(
                    uiState = sampleState.copy(days = sampleState.days + PlanningDayUiModel(2, secondDay, "11.21")),
                    onMoveItinerary = { itemId, date -> requestedMove = itemId to date },
                )
            }
        }

        composeRule.onNodeWithText("일차 이동").performClick()
        composeRule.onNodeWithText("1일차 · 11.20").assertDoesNotExist()
        composeRule.onNodeWithText("2일차 · 11.21").performClick()

        composeRule.runOnIdle { assertEquals(3L to secondDay, requestedMove) }
    }

    @Test
    fun temporarySave_invokesExitAction() {
        var saved = false
        composeRule.setContent {
            MaterialTheme {
                TripPlanningScreenContent(
                    uiState = sampleState.copy(isDraft = true),
                    onSaveDraft = { saved = true },
                )
            }
        }

        composeRule.onNodeWithText("임시저장").performClick()

        composeRule.runOnIdle { assertTrue(saved) }
    }

    @Test
    fun candidateForm_savesManualNameAndNote() {
        var saved: Pair<String, String>? = null
        composeRule.setContent {
            MaterialTheme {
                PlaceCandidateForm(
                    state = PlaceCandidateListUiState(),
                    onDismiss = {},
                    onSearch = {},
                    onSave = { name, _, note, _, _ -> saved = name to note },
                )
            }
        }

        composeRule.onNodeWithText("장소 이름").performTextInput("동문시장")
        composeRule.onNodeWithText("메모 (선택)").performTextInput("저녁 후보")
        composeRule.onNodeWithText("저장").performClick()

        composeRule.runOnIdle { assertEquals("동문시장" to "저녁 후보", saved) }
    }

    @Test
    fun candidateSheet_addsToChosenDay() {
        val day = sampleState.days.single()
        var scheduled: Pair<Long, LocalDate>? = null
        composeRule.setContent {
            MaterialTheme {
                PlaceCandidateSheet(
                    state = PlaceCandidateListUiState(
                        items = listOf(PlaceCandidateUiModel(4, "동문시장", "", "저녁", null)),
                    ),
                    days = listOf(day),
                    selectedDate = day.date,
                    onDismiss = {},
                    onAdd = {},
                    onSchedule = { id, date -> scheduled = id to date },
                    onRemove = {},
                )
            }
        }

        composeRule.onNodeWithText("일정에 넣기").performClick()
        composeRule.onNodeWithText("1일차 · 11.20").performClick()

        composeRule.runOnIdle { assertEquals(4L to day.date, scheduled) }
    }

    @Test
    fun lodgingEditor_suggestsPreviousCheckoutForNextHotel() {
        val start = LocalDate.of(2026, 11, 26)
        val dates = List(5) { start.plusDays(it.toLong()) }
        var saved: LodgingUiModel? = null
        composeRule.setContent {
            MaterialTheme {
                LodgingEditor(
                    existing = null,
                    dates = dates,
                    state = LodgingUiState(
                        lodgings = listOf(LodgingUiModel(1, "마이애미 호텔", dates[0], dates[2], 2)),
                    ),
                    onDismiss = {},
                    onSave = { lodging, _ -> saved = lodging },
                    onDelete = {},
                )
            }
        }

        composeRule.onNodeWithText("숙소 이름").performTextInput("다른 호텔")
        composeRule.onNodeWithText("숙소 저장").performClick()

        composeRule.runOnIdle {
            assertEquals(dates[2], saved?.checkIn)
            assertEquals(dates[4], saved?.checkOut)
        }
    }

    @Test
    fun lodgingTransferDay_showsCheckoutAndCheckin() {
        val start = LocalDate.of(2026, 11, 26)
        val moveDate = start.plusDays(2)
        composeRule.setContent {
            MaterialTheme {
                LodgingDayInfo(
                    lodgings = listOf(
                        LodgingUiModel(1, "마이애미 호텔", start, moveDate, 2),
                        LodgingUiModel(2, "다른 호텔", moveDate, moveDate.plusDays(2), 2),
                    ),
                    date = moveDate,
                )
            }
        }

        composeRule.onNodeWithText("숙소 이동 · 마이애미 호텔 체크아웃 → 다른 호텔 체크인").assertIsDisplayed()
    }

    @Test
    fun lodgingEditor_selectsCheckoutTimeAndSearchResult() {
        val start = LocalDate.of(2026, 11, 26)
        var saved: LodgingUiModel? = null
        var resolvePlace = false
        composeRule.setContent {
            MaterialTheme {
                LodgingEditor(
                    existing = null,
                    dates = listOf(start, start.plusDays(1)),
                    state = LodgingUiState(
                        suggestions = listOf(LodgingPlaceUiModel("마이애미 호텔", "방콕", googlePlaceId = "hotel-id")),
                    ),
                    onDismiss = {},
                    onSave = { lodging, resolve ->
                        saved = lodging
                        resolvePlace = resolve
                    },
                    onDelete = {},
                )
            }
        }

        composeRule.onNodeWithText("마이애미 호텔 · 방콕").performClick()
        composeRule.onNodeWithText("체크아웃 시간 선택 (선택)").performClick()
        composeRule.onNodeWithTag("time_values_시").performScrollToIndex(10)
        composeRule.onNodeWithText("10시").performClick()
        composeRule.onNodeWithTag("time_values_분").performScrollToIndex(15)
        composeRule.onNodeWithText("15분").performClick()
        composeRule.onNodeWithText("확인").performClick()
        composeRule.onNodeWithText("숙소 저장").performClick()

        composeRule.runOnIdle {
            assertEquals(LocalTime.of(10, 15), saved?.checkOutTime)
            assertTrue(resolvePlace)
            assertEquals("hotel-id", saved?.googleMapsUri()?.getQueryParameter("query_place_id"))
        }
    }

    private val sampleState = TripPlanningUiState(
        title = "다낭 여행",
        destination = "다낭",
        dateText = "2026.11.20 - 2026.11.23",
        days = listOf(PlanningDayUiModel(1, LocalDate.of(2026, 11, 20), "11.20")),
        selectedDate = LocalDate.of(2026, 11, 20),
        items = listOf(PlanningItemUiModel(3L, "한시장 방문", "한시장", "10:00")),
    )
}
