package com.example.tripplan.presentation.packing

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.example.tripplan.domain.model.PackingItemType
import com.example.tripplan.presentation.create.ComposeTestActivity
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class PackingScreenTest {
    @get:Rule val rule=createAndroidComposeRule<ComposeTestActivity>()
    @Test fun addSharedItem_invokesSave(){var saved="";rule.setContent{MaterialTheme{PackingScreenContent(PackingUiState(false),onSave={name,type,id->saved="$name:$type:$id"})}};rule.onNodeWithText("준비물 이름").performTextInput("여권");rule.onNodeWithText("추가").performClick();rule.runOnIdle{assertEquals("여권:${PackingItemType.Shared}:null",saved)}}
}
