package com.example.tripplan.presentation.copy

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun TripCopyScreen(sourceTripId: Long, onBack: () -> Unit, onCopied: (Long) -> Unit,
    viewModel: TripCopyViewModel = hiltViewModel(key = "copy-$sourceTripId")) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(sourceTripId) { viewModel.load(sourceTripId) }
    LaunchedEffect(viewModel) { viewModel.events.collect(onCopied) }
    TripCopyContent(state, onBack, viewModel::updateTitle, viewModel::updateDates, viewModel::toggleInclude, viewModel::toggleDay, viewModel::copy)
}

@Composable
internal fun TripCopyContent(state: TripCopyUiState, onBack: () -> Unit = {}, onTitle: (String) -> Unit = {},
    onDates: (java.time.LocalDate, java.time.LocalDate) -> Unit = { _, _ -> }, onInclude: () -> Unit = {},
    onDay: (Int) -> Unit = {}, onCopy: () -> Unit = {}) {
    var startText by remember(state.startDate) { mutableStateOf(state.startDate.toString()) }
    var endText by remember(state.endDate) { mutableStateOf(state.endDate.toString()) }
    var showConfirmation by remember { mutableStateOf(false) }
    val totalItems = state.itemCountsByDay.values.sum()
    val includedItems = if (state.includeItinerary) state.retainedDays.sumOf { state.itemCountsByDay[it] ?: 0 } else 0
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row { TextButton(onClick = onBack) { Text("뒤로") }; Text("여행 복사", style = MaterialTheme.typography.headlineSmall) }
        OutlinedTextField(state.title, onTitle, Modifier.fillMaxWidth(), label = { Text("새 여행 이름") })
        OutlinedTextField(startText, { startText = it }, Modifier.fillMaxWidth(), label = { Text("새 출발일 (YYYY-MM-DD)") })
        OutlinedTextField(endText, { endText = it }, Modifier.fillMaxWidth(), label = { Text("새 종료일 (YYYY-MM-DD)") })
        TextButton(onClick = {
            runCatching { onDates(java.time.LocalDate.parse(startText), java.time.LocalDate.parse(endText)) }
        }) { Text("날짜 적용") }
        Row { Checkbox(state.includeItinerary, { onInclude() }); Text("일정 포함") }
        if (state.includeItinerary) {
            Text("가져올 원본 일차")
            (1..state.originalDayCount).forEach { day -> Row { Checkbox(day in state.retainedDays, { onDay(day) }); Text("${day}일차") } }
        }
        if (state.isError) Text("복사 조건을 확인해 주세요.", color = MaterialTheme.colorScheme.error)
        Text("포함 일정 ${includedItems}개 · 제외 일정 ${totalItems - includedItems}개")
        Button(onClick = { showConfirmation = true }, enabled = !state.isSaving, modifier = Modifier.fillMaxWidth()) { Text("복사 내용 확인") }
    }
    if (showConfirmation) AlertDialog(
        onDismissRequest = { showConfirmation = false },
        title = { Text("여행 복사") },
        text = { Text("일정 ${includedItems}개를 포함해 새 여행을 만들까요?") },
        confirmButton = { TextButton(onClick = { showConfirmation = false; onCopy() }) { Text("복사하기") } },
        dismissButton = { TextButton(onClick = { showConfirmation = false }) { Text("취소") } },
    )
}

@Preview(showBackground = true) @Composable private fun TripCopyPreview() { MaterialTheme { TripCopyContent(TripCopyUiState(false, title="제주 여행 복사", originalDayCount=3, retainedDays=setOf(1,2,3))) } }
