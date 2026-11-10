package com.example.tripplan.presentation.settings

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.example.tripplan.presentation.create.ComposeTestActivity
import org.junit.Rule
import org.junit.Test

class SettingsScreenTest {@get:Rule val rule=createAndroidComposeRule<ComposeTestActivity>()
 @Test fun showsPreparationMessage(){rule.setContent{MaterialTheme{SettingsScreen()}};rule.onNodeWithText("설정 기능은 준비 중입니다.").assertIsDisplayed()}}
