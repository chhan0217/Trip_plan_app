package com.example.tripplan.presentation.place

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.tripplan.presentation.create.ComposeTestActivity
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class PlaceScreenTest {@get:Rule val rule=createAndroidComposeRule<ComposeTestActivity>()
 @Test fun editExistingPlace_populatesEditorAndSavesId(){var id:Long?=null;rule.setContent{MaterialTheme{PlaceContent(PlaceUiState(false,listOf(PlaceUiModel(7,"성산일출봉","제주"))),onSave={value,_,_->id=value})}};rule.onNodeWithText("수정").performClick();rule.onNodeWithText("장소 수정 저장").performClick();rule.runOnIdle{assertEquals(7L,id)}}}
