package com.example.tripplan.presentation.copy

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.example.tripplan.presentation.create.ComposeTestActivity
import org.junit.Rule
import org.junit.Test

class TripCopyScreenTest {@get:Rule val rule=createAndroidComposeRule<ComposeTestActivity>()
 @Test fun itineraryCopy_showsAllOriginalDays(){rule.setContent{MaterialTheme{TripCopyContent(TripCopyUiState(false,title="복사",originalDayCount=3,retainedDays=setOf(1,2,3)))}};rule.onNodeWithText("1일차").assertIsDisplayed();rule.onNodeWithText("3일차").assertIsDisplayed()}}
