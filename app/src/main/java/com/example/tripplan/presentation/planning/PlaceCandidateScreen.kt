package com.example.tripplan.presentation.planning

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PlaceCandidateSheet(
    state: PlaceCandidateListUiState,
    days: List<PlanningDayUiModel>,
    selectedDate: LocalDate?,
    onDismiss: () -> Unit,
    onAdd: () -> Unit,
    onSchedule: (Long, LocalDate) -> Unit,
    onRemove: (Long) -> Unit,
) {
    var schedulingId by rememberSaveable { mutableStateOf<Long?>(null) }
    var removingId by rememberSaveable { mutableStateOf<Long?>(null) }
    ModalBottomSheet(onDismissRequest = { if (!state.isBusy) onDismiss() }) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("후보 장소", Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
                    Button(onClick = onAdd, enabled = !state.isBusy) { Text("장소 추가") }
                }
            }
            state.error?.let { error ->
                item { Text(error, color = MaterialTheme.colorScheme.error) }
            }
            if (state.items.isEmpty()) {
                item { Text("가고 싶은 장소를 먼저 모아두세요.") }
            }
            items(state.items, key = { it.id }) { candidate ->
                val scheduledDay = days.firstOrNull { it.date == candidate.scheduledDate }
                OutlinedCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(candidate.name, style = MaterialTheme.typography.titleMedium)
                        if (candidate.address.isNotBlank()) Text(candidate.address)
                        if (candidate.note.isNotBlank()) Text(candidate.note)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (scheduledDay == null) {
                                TextButton(
                                    onClick = { schedulingId = candidate.id },
                                    enabled = !state.isBusy && days.isNotEmpty(),
                                ) { Text("일정에 넣기") }
                            } else {
                                Text("${scheduledDay.dayNumber}일차에 담김", Modifier.padding(end = 8.dp))
                            }
                            TextButton(onClick = { removingId = candidate.id }, enabled = !state.isBusy) {
                                Text("후보에서 삭제")
                            }
                        }
                    }
                }
            }
        }
    }

    schedulingId?.let { id ->
        val orderedDays = days.sortedBy { it.date != selectedDate }
        AlertDialog(
            onDismissRequest = { schedulingId = null },
            title = { Text("넣을 일차 선택") },
            text = {
                LazyColumn(Modifier.heightIn(max = 300.dp)) {
                    items(orderedDays, key = { it.date.toString() }) { day ->
                        TextButton(onClick = {
                            schedulingId = null
                            onSchedule(id, day.date)
                        }) {
                            Text("${day.dayNumber}일차 · ${day.dateText}")
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { schedulingId = null }) { Text("취소") } },
        )
    }

    removingId?.let { id ->
        AlertDialog(
            onDismissRequest = { removingId = null },
            title = { Text("후보 장소 삭제") },
            text = { Text("후보 목록에서 삭제합니다. 추가된 일정은 유지됩니다.") },
            confirmButton = {
                TextButton(onClick = {
                    removingId = null
                    onRemove(id)
                }) { Text("삭제") }
            },
            dismissButton = { TextButton(onClick = { removingId = null }) { Text("취소") } },
        )
    }
}

@Composable
internal fun PlaceCandidateForm(
    state: PlaceCandidateListUiState,
    onDismiss: () -> Unit,
    onSearch: (String) -> Unit,
    onSave: (String, String, String, Long?, String?) -> Unit,
) {
    var name by rememberSaveable { mutableStateOf("") }
    var address by rememberSaveable { mutableStateOf("") }
    var note by rememberSaveable { mutableStateOf("") }
    var savedId by rememberSaveable { mutableStateOf<Long?>(null) }
    var externalId by rememberSaveable { mutableStateOf<String?>(null) }
    var showSaved by rememberSaveable { mutableStateOf(false) }
    var showSearch by rememberSaveable { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { if (!state.isBusy) onDismiss() },
        title = { Text("후보 장소 추가") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = {
                            name = it
                            if (savedId != null || externalId != null) address = ""
                            savedId = null
                            externalId = null
                            showSearch = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("장소 이름") },
                        singleLine = true,
                        enabled = !state.isBusy,
                    )
                }
                item {
                    Row {
                        TextButton(
                            onClick = {
                                showSaved = false
                                showSearch = true
                                onSearch(name)
                            },
                            enabled = name.trim().length >= 2 && !state.isBusy && !state.isSearching,
                        ) { Text("지도 검색") }
                        TextButton(
                            onClick = { showSaved = !showSaved; showSearch = false },
                            enabled = state.savedPlaces.isNotEmpty() && !state.isBusy,
                        ) { Text("저장한 장소") }
                    }
                }
                if (showSaved) {
                    items(state.savedPlaces, key = { it.id }) { place ->
                        TextButton(onClick = {
                            name = place.name
                            address = place.address
                            savedId = place.id
                            externalId = null
                            showSaved = false
                        }) { Text(listOf(place.name, place.address).filter(String::isNotBlank).joinToString(" · ")) }
                    }
                }
                if (showSearch) {
                    if (state.isSearching) item { Text("장소를 검색하는 중입니다.") }
                    if (state.searchPerformed && state.suggestions.isEmpty()) {
                        item { Text("검색 결과가 없습니다. 이름을 직접 저장할 수 있어요.") }
                    }
                    items(state.suggestions, key = { it.id }) { place ->
                        TextButton(onClick = {
                            name = place.name
                            address = place.address
                            savedId = null
                            externalId = place.id
                            showSearch = false
                        }) { Text(listOf(place.name, place.address).filter(String::isNotBlank).joinToString(" · ")) }
                    }
                }
                item {
                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("주소 (선택)") },
                        singleLine = true,
                        enabled = !state.isBusy && savedId == null && externalId == null,
                    )
                }
                item {
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("메모 (선택)") },
                        enabled = !state.isBusy,
                    )
                }
                state.error?.let { error ->
                    item { Text(error, color = MaterialTheme.colorScheme.error) }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(name, address, note, savedId, externalId) },
                enabled = name.isNotBlank() && !state.isBusy,
            ) { Text("저장") }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !state.isBusy) { Text("취소") } },
    )
}

@Preview(showBackground = true)
@Composable
private fun PlaceCandidateFormPreview() {
    MaterialTheme {
        PlaceCandidateForm(PlaceCandidateListUiState(), {}, {}, { _, _, _, _, _ -> })
    }
}

@Preview(showBackground = true)
@Composable
private fun PlaceCandidateSheetPreview() {
    val date = LocalDate.of(2026, 11, 27)
    MaterialTheme {
        PlaceCandidateSheet(
            state = PlaceCandidateListUiState(
                items = listOf(
                    PlaceCandidateUiModel(1, "고기국수집", "제주시", "점심 후보", null),
                    PlaceCandidateUiModel(2, "동문시장", "제주시", "", date),
                ),
            ),
            days = listOf(PlanningDayUiModel(1, date, "11.27")),
            selectedDate = date,
            onDismiss = {},
            onAdd = {},
            onSchedule = { _, _ -> },
            onRemove = {},
        )
    }
}
