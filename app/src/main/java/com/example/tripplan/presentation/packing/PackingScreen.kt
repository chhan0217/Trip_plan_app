package com.example.tripplan.presentation.packing

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.tripplan.domain.model.PackingItemType

@Composable
fun PackingScreen(
    tripPlanId: Long,
    onBackClick: () -> Unit,
    viewModel: PackingViewModel = hiltViewModel(key = "packing-$tripPlanId"),
) {
    val state by viewModel.uiState.collectAsState()
    LaunchedEffect(tripPlanId) { viewModel.load(tripPlanId) }
    PackingScreenContent(state, onBackClick, viewModel::save, viewModel::toggle, viewModel::delete)
}

@Composable
internal fun PackingScreenContent(
    state: PackingUiState,
    onBackClick: () -> Unit = {},
    onSave: (String, PackingItemType, Long?) -> Unit = { _, _, _ -> },
    onToggle: (Long) -> Unit = {},
    onDelete: (Long) -> Unit = {},
) {
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(PackingItemType.Shared) }
    var editingId by remember { mutableStateOf<Long?>(null) }
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onBackClick) { Text("뒤로") }
                Text("준비물", style = MaterialTheme.typography.headlineSmall)
            }
        }
        item {
            OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth(), label = { Text("준비물 이름") })
            Row {
                TextButton(onClick = { type = PackingItemType.Shared }, enabled = editingId == null || type == PackingItemType.Shared) { Text("공통 준비물") }
                TextButton(onClick = { type = PackingItemType.Personal }, enabled = editingId == null || type == PackingItemType.Personal) { Text("내 준비물") }
                Button(onClick = { if (name.isNotBlank()) { onSave(name, type, editingId); name = ""; editingId = null } }) {
                    Text(if (editingId == null) "추가" else "수정 저장")
                }
            }
            Text("추가한 준비물의 유형은 변경할 수 없습니다.", style = MaterialTheme.typography.bodySmall)
        }
        packingSection("공통 준비물", state.sharedItems, onToggle, onDelete) { item ->
            editingId = item.id; name = item.name; type = PackingItemType.Shared
        }
        packingSection("내 준비물", state.personalItems, onToggle, onDelete) { item ->
            editingId = item.id; name = item.name; type = PackingItemType.Personal
        }
        if (!state.isLoading && state.sharedItems.isEmpty() && state.personalItems.isEmpty()) {
            item { Text("등록된 준비물이 없습니다.") }
        }
        if (state.isLoadFailed) item { Text("준비물을 불러오지 못했습니다.", color = MaterialTheme.colorScheme.error) }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.packingSection(
    title: String,
    values: List<PackingItemUiModel>,
    onToggle: (Long) -> Unit,
    onDelete: (Long) -> Unit,
    onEdit: (PackingItemUiModel) -> Unit,
) {
    item { Text(title, style = MaterialTheme.typography.titleMedium) }
    items(values, key = { it.id }) { item ->
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(item.checked, { onToggle(item.id) })
            Column(Modifier.weight(1f)) {
                Text(item.name)
                if (item.quantity > 1) Text("${item.quantity}개", style = MaterialTheme.typography.bodySmall)
            }
            TextButton(onClick = { onDelete(item.id) }) { Text("삭제") }
            TextButton(onClick = { onEdit(item) }) { Text("수정") }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PackingPreview() {
    MaterialTheme {
        PackingScreenContent(PackingUiState(false, sharedItems = listOf(PackingItemUiModel(1, "여권", 1, false))))
    }
}

@Preview(showBackground = true)
@Composable
private fun PackingEmptyPreview() { MaterialTheme { PackingScreenContent(PackingUiState(false)) } }
